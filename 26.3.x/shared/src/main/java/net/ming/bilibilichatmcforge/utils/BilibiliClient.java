package net.ming.bilibilichatmcforge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.ming.bilibilichatmcforge.JsonConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;

/**
 * 对接哔哩哔哩直播开放平台：申请弹幕会话、维持心跳，
 * 并把收到的弹幕/礼物/SC/大航海事件广播到游戏聊天栏。
 */
public class BilibiliClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().build();

    // 开放平台应用凭据随模组内置分发（主播侧只填身份码）。
    // 字节码里不落明文：源码保存的是逐字节异或 + Base64 的结果，类加载时还原。
    // 轮换密钥后运行 tools/encode-credentials.ps1 重新生成下面两行。
    private static final String ACCESS_KEY_ID = unmask("XlI7VwbszZP1HnMbJ/n+uv14JUYY5fqv", 0x3C);
    private static final String ACCESS_SECRET = unmask("JnUIcGGMluXyC1o9YJjY/N5pIl4DmZ3x/DM9UVCv", 0x5F);
    // app_id 会随每个请求体明文传输，混淆它没有意义
    private static final long APP_ID = 1779863002402L;

    private static final String API_START = "https://live-open.biliapi.com/v2/app/start";
    private static final String API_HEARTBEAT = "https://live-open.biliapi.com/v2/app/heartbeat";
    private static final String API_END = "https://live-open.biliapi.com/v2/app/end";

    // 收到的数据包不可信，超限直接丢弃，防止被撑爆内存
    private static final int MAX_PACKET_BYTES = 1024 * 1024;
    private static final int MAX_DECOMPRESSED_BYTES = 4 * 1024 * 1024;
    private static final int MAX_RECONNECTS = 5;

    private final MinecraftServer server;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BilibiliDM-Scheduler");
        t.setDaemon(true);
        return t;
    });

    private WebSocket ws;
    private ScheduledFuture<?> appHeartbeatTask;
    private ScheduledFuture<?> wsHeartbeatTask;
    private String gameId;
    private volatile boolean running;
    private int reconnects;

    public BilibiliClient(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        if (running) return;
        running = true;
        reconnects = 0;

        if (JsonConfigManager.getInstance().identityCode.isEmpty()) {
            LOGGER.error("Bilibili identity code is not configured");
            broadcast(Component.translatable("mod.bilibilichatmcforge.error.identity_code_missing"));
            running = false;
            return;
        }
        CompletableFuture.runAsync(this::connect);
    }

    public void stop() {
        if (!running) return;
        running = false;
        cancelTimers();

        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
        if (gameId != null) {
            String id = gameId;
            gameId = null;
            CompletableFuture.runAsync(() -> {
                try {
                    post(API_END, GSON.toJson(endBody(id)));
                } catch (Exception e) {
                    LOGGER.error("Error ending Bilibili app", e);
                }
            });
        }
    }

    private void connect() {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("app_id", APP_ID);
            body.addProperty("code", JsonConfigManager.getInstance().identityCode);

            HttpResponse<String> resp = post(API_START, GSON.toJson(body));
            JsonObject respJson = GSON.fromJson(resp.body(), JsonObject.class);
            if (respJson.get("code").getAsInt() != 0) {
                fail(Component.translatable("mod.bilibilichatmcforge.error.app_start_failed",
                        respJson.get("message").getAsString()));
                return;
            }

            JsonObject data = respJson.getAsJsonObject("data");
            gameId = data.getAsJsonObject("game_info").get("game_id").getAsString();
            // 项目级心跳，官方要求每 20 秒一次，断了会被回收会话
            if (appHeartbeatTask != null) appHeartbeatTask.cancel(false);
            appHeartbeatTask = scheduler.scheduleAtFixedRate(this::sendAppHeartbeat, 20, 20, TimeUnit.SECONDS);

            JsonObject wsInfo = data.getAsJsonObject("websocket_info");
            List<String> links = GSON.fromJson(wsInfo.get("wss_link"), List.class);
            if (links == null || links.isEmpty()) {
                LOGGER.error("No WSS links provided by Bilibili");
                running = false;
                return;
            }

            HTTP.newWebSocketBuilder()
                    .buildAsync(URI.create(links.get(0)), new DanmakuListener(wsInfo.get("auth_body").getAsString()))
                    .thenAccept(w -> {
                        ws = w;
                        reconnects = 0;
                        broadcast(Component.translatable("mod.bilibilichatmcforge.info.connected"));
                    });
        } catch (Exception e) {
            LOGGER.error("Error connecting to Bilibili", e);
            String reason = e.getCause() instanceof java.nio.channels.UnresolvedAddressException
                    ? "DNS解析失败，请检查网络连接"
                    : e.getMessage();
            fail(Component.translatable("mod.bilibilichatmcforge.error.connect_failed", reason));
        }
    }

    private void sendAppHeartbeat() {
        if (!running || gameId == null) return;
        JsonObject body = new JsonObject();
        body.addProperty("game_id", gameId);
        postAsync(API_HEARTBEAT, GSON.toJson(body));
    }

    private void fail(Component msg) {
        LOGGER.error("Bilibili connection failed, stopping client");
        broadcast(msg);
        running = false;
    }

    private void broadcast(Component msg) {
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(msg, false));
    }

    private void cancelTimers() {
        if (appHeartbeatTask != null) {
            appHeartbeatTask.cancel(false);
            appHeartbeatTask = null;
        }
        if (wsHeartbeatTask != null) {
            wsHeartbeatTask.cancel(false);
            wsHeartbeatTask = null;
        }
    }

    // ---- HTTP ----

    private HttpResponse<String> post(String url, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        sign(body).forEach(builder::header);
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void postAsync(String url, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        sign(body).forEach(builder::header);
        HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject endBody(String id) {
        JsonObject body = new JsonObject();
        body.addProperty("app_id", APP_ID);
        body.addProperty("game_id", id);
        return body;
    }

    /**
     * 按开放平台规范计算签名：六个 x-bili 头按 key 排序拼接后做 HMAC-SHA256。
     */
    private Map<String, String> sign(String body) {
        Map<String, String> headers = new TreeMap<>();
        headers.put("x-bili-accesskeyid", ACCESS_KEY_ID);
        headers.put("x-bili-content-md5", md5(body));
        headers.put("x-bili-signature-method", "HMAC-SHA256");
        headers.put("x-bili-signature-nonce", UUID.randomUUID().toString());
        headers.put("x-bili-signature-version", "1.0");
        headers.put("x-bili-timestamp", String.valueOf(System.currentTimeMillis() / 1000));

        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (canonical.length() > 0) canonical.append('\n');
            canonical.append(entry.getKey()).append(':').append(entry.getValue());
        }

        headers.put("Authorization", hmacSha256(ACCESS_SECRET, canonical.toString()));
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }

    private String md5(String input) {
        return digest("MD5", input);
    }

    private String hmacSha256(String secret, String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String digest(String algorithm, String input) {
        try {
            return toHex(MessageDigest.getInstance(algorithm).digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

    /**
     * 还原 encode-credentials.ps1 混淆过的凭据，位置相关的异或避免相同字符产生规律。
     */
    private static String unmask(String encoded, int key) {
        byte[] raw = Base64.getDecoder().decode(encoded);
        byte[] out = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            out[i] = (byte) (raw[i] ^ key ^ (i * 31 & 0xff));
        }
        return new String(out, StandardCharsets.UTF_8);
    }

    /**
     * 弹幕 WebSocket。协议是 B 站自定义的二进制分包：
     * 16 字节头（包长/头长/协议版本/opcode/序号）+ 正文，op=5 为业务消息。
     */
    private class DanmakuListener implements WebSocket.Listener {
        private final String authBody;

        DanmakuListener(String authBody) {
            this.authBody = authBody;
        }

        @Override
        public void onOpen(WebSocket socket) {
            send(socket, OP_AUTH, authBody);
            // 协议层心跳，30 秒一次；重连会走到新的 onOpen，旧任务先撤掉
            if (wsHeartbeatTask != null) wsHeartbeatTask.cancel(false);
            wsHeartbeatTask = scheduler.scheduleAtFixedRate(() -> {
                if (running) send(socket, OP_HEARTBEAT, "");
            }, 30, 30, TimeUnit.SECONDS);
            WebSocket.Listener.super.onOpen(socket);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket socket, ByteBuffer data, boolean last) {
            try {
                handlePackets(data);
            } catch (Exception e) {
                LOGGER.warn("Failed to handle danmaku packet", e);
            }
            socket.request(1);
            return WebSocket.Listener.super.onBinary(socket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
            LOGGER.info("Bilibili WebSocket closed: {} {}", statusCode, reason);
            if (!running) {
                return WebSocket.Listener.super.onClose(socket, statusCode, reason);
            }
            if (++reconnects > MAX_RECONNECTS) {
                fail(Component.translatable("mod.bilibilichatmcforge.error.connect_failed",
                        "WebSocket连接失败，已达到最大重连次数"));
                return WebSocket.Listener.super.onClose(socket, statusCode, reason);
            }
            LOGGER.info("Reconnecting in 5 seconds ({}/{})", reconnects, MAX_RECONNECTS);
            scheduler.schedule(BilibiliClient.this::connect, 5, TimeUnit.SECONDS);
            return WebSocket.Listener.super.onClose(socket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket socket, Throwable error) {
            LOGGER.error("Bilibili WebSocket error", error);
        }

        private void send(WebSocket socket, int op, String body) {
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            ByteBuffer packet = ByteBuffer.allocate(16 + payload.length).order(ByteOrder.BIG_ENDIAN);
            packet.putInt(16 + payload.length);
            packet.putShort((short) 16); // 头部长度
            packet.putShort((short) 1);  // 协议版本，正文裸 JSON
            packet.putInt(op);
            packet.putInt(1);
            packet.put(payload);
            packet.flip();
            socket.sendBinary(packet, true);
        }
    }

    private static final int OP_HEARTBEAT = 2;
    private static final int OP_AUTH = 7;
    private static final int OP_MESSAGE = 5;

    private void handlePackets(ByteBuffer buf) throws IOException {
        buf.order(ByteOrder.BIG_ENDIAN);
        while (buf.remaining() >= 16) {
            int totalLen = buf.getInt();
            int headerLen = buf.getShort();
            int protoVer = buf.getShort();
            int op = buf.getInt();
            buf.getInt(); // 序号，用不到

            if (totalLen < 16 || totalLen > MAX_PACKET_BYTES || headerLen < 16) {
                throw new IOException("malformed packet: len=" + totalLen + " header=" + headerLen);
            }
            int bodyLen = totalLen - headerLen;
            if (bodyLen > buf.remaining()) {
                throw new IOException("truncated packet: need " + bodyLen + ", got " + buf.remaining());
            }

            byte[] body = new byte[bodyLen];
            buf.get(body);

            if (op != OP_MESSAGE) {
                continue; // 心跳回复(op=3)、认证回复(op=8)不用处理
            }
            if (protoVer == 2) {
                handlePackets(ByteBuffer.wrap(inflate(body)));
                return; // 解压出来是完整的包流，递归处理完即可
            }
            dispatch(GSON.fromJson(new String(body, StandardCharsets.UTF_8), JsonObject.class));
        }
    }

    private void dispatch(JsonObject json) {
        try {
            Component msg = parseMessage(json.get("cmd").getAsString(), json);
            if (msg != null) {
                server.execute(() -> server.getPlayerList().broadcastSystemMessage(msg, false));
            }
        } catch (Exception e) {
            // 字段缺失或格式变化的消息直接忽略
            LOGGER.debug("Skipped message: {}", e.getMessage());
        }
    }

    private Component parseMessage(String cmd, JsonObject json) {
        JsonObject data = json.has("data") ? json.getAsJsonObject("data") : null;
        switch (cmd) {
            case "LIVE_OPEN_PLATFORM_DM":
            case "OPEN_LIVEROOM_DM":
                return data == null ? null : Component.translatable("mod.bilibilichatmcforge.chat.danmaku",
                        data.get("uname").getAsString(), data.get("msg").getAsString());
            case "LIVE_OPEN_PLATFORM_SEND_GIFT":
            case "OPEN_LIVEROOM_SEND_GIFT":
                return data == null ? null : Component.translatable("mod.bilibilichatmcforge.chat.gift",
                        data.get("uname").getAsString(), data.get("gift_name").getAsString(), data.get("gift_num").getAsInt());
            case "LIVE_OPEN_PLATFORM_SUPER_CHAT":
            case "OPEN_LIVEROOM_SUPER_CHAT":
                return data == null ? null : Component.translatable("mod.bilibilichatmcforge.chat.sc",
                        data.get("uname").getAsString(), data.get("rmb").getAsLong(), data.get("message").getAsString());
            case "LIVE_OPEN_PLATFORM_GUARD":
            case "OPEN_LIVEROOM_GUARD": {
                if (data == null) return null;
                String uname = data.getAsJsonObject("user_info").get("uname").getAsString();
                return Component.translatable("mod.bilibilichatmcforge.chat.guard", uname, guardName(data.get("guard_level").getAsInt()));
            }
            // 以下是非开放平台的旧版推送格式，留着兼容
            case "DANMU_MSG": {
                JsonArray info = json.getAsJsonArray("info");
                return Component.translatable("mod.bilibilichatmcforge.chat.danmaku",
                        info.get(2).getAsJsonArray().get(1).getAsString(), info.get(1).getAsString());
            }
            case "SEND_GIFT":
                return data == null ? null : Component.translatable("mod.bilibilichatmcforge.chat.gift",
                        data.get("uname").getAsString(), data.get("giftName").getAsString(), data.get("num").getAsInt());
            case "SUPER_CHAT_MESSAGE":
                return data == null ? null : Component.translatable("mod.bilibilichatmcforge.chat.sc",
                        data.getAsJsonObject("user_info").get("uname").getAsString(),
                        data.get("price").getAsLong(), data.get("message").getAsString());
            case "GUARD_BUY":
                return data == null ? null : Component.translatable("mod.bilibilichatmcforge.chat.guard",
                        data.get("username").getAsString(), data.get("gift_name").getAsString());
            default:
                return null;
        }
    }

    private String guardName(int level) {
        switch (level) {
            case 1: return "总督";
            case 2: return "提督";
            case 3: return "舰长";
            default: return "大航海";
        }
    }

    /**
     * zlib 解压，带输出上限。理论上限内的正常弹幕包远小于这个值。
     */
    private byte[] inflate(byte[] compressed) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 4);
        byte[] chunk = new byte[8192];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(chunk);
                if (n == 0 && inflater.needsInput()) {
                    throw new IOException("truncated zlib stream");
                }
                out.write(chunk, 0, n);
                if (out.size() > MAX_DECOMPRESSED_BYTES) {
                    throw new IOException("decompressed packet too large, aborting");
                }
            }
            return out.toByteArray();
        } catch (java.util.zip.DataFormatException e) {
            throw new IOException("bad zlib data", e);
        } finally {
            inflater.end();
        }
    }
}
