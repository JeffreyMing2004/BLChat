package net.ming.bilibilichatmcforge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.ming.bilibilichatmcforge.JsonConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

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
import java.util.ArrayList;
import java.util.List;
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
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    // 收到的数据包不可信，超限直接丢弃，防止被撑爆内存
    private static final int MAX_PACKET_BYTES = 1024 * 1024;
    private static final int MAX_DECOMPRESSED_BYTES = 4 * 1024 * 1024;

    private WebSocket webSocket;
    private final MinecraftServer server;
    private String gameId;
    private volatile boolean isRunning = false;
    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> wsHeartbeatTask;

    public BilibiliClient(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;

        List<String> missingFields = checkConfig();
        if (!missingFields.isEmpty()) {
            String missing = String.join(", ", missingFields);
            LOGGER.error("Bilibili config is incomplete. Missing: {}", missing);
            broadcast(Component.translatable("mod.bilibilichatmcforge.error.config_incomplete", missing));
            isRunning = false;
            return;
        }

        CompletableFuture.runAsync(this::connect);
    }

    private void connect() {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("app_id", JsonConfigManager.getInstance().appId);
            body.addProperty("code", JsonConfigManager.getInstance().roomCode);

            HttpResponse<String> response = post("https://live-open.biliapi.com/v2/app/start", GSON.toJson(body));
            JsonObject respJson = GSON.fromJson(response.body(), JsonObject.class);

            if (respJson.get("code").getAsInt() != 0) {
                String errorMsg = respJson.get("message").getAsString();
                LOGGER.error("Failed to start Bilibili app: {}", errorMsg);
                broadcast(Component.translatable("mod.bilibilichatmcforge.error.app_start_failed", errorMsg));
                isRunning = false;
                return;
            }

            JsonObject data = respJson.getAsJsonObject("data");
            gameId = data.getAsJsonObject("game_info").get("game_id").getAsString();
            // 项目级心跳，官方要求每 20 秒一次，断了会被回收会话
            if (heartbeatTask != null) heartbeatTask.cancel(false);
            heartbeatTask = SCHEDULER.scheduleAtFixedRate(this::sendHeartbeat, 20, 20, TimeUnit.SECONDS);

            JsonObject websocketInfo = data.getAsJsonObject("websocket_info");
            List<String> wssLinks = GSON.fromJson(websocketInfo.get("wss_link"), List.class);
            if (wssLinks == null || wssLinks.isEmpty()) {
                LOGGER.error("No WSS links provided by Bilibili.");
                isRunning = false;
                return;
            }

            HTTP_CLIENT.newWebSocketBuilder()
                    .buildAsync(URI.create(wssLinks.get(0)), new BilibiliWebSocketListener(websocketInfo.get("auth_body").getAsString()))
                    .thenAccept(ws -> {
                        webSocket = ws;
                        broadcast(Component.translatable("mod.bilibilichatmcforge.info.connected"));
                    });
        } catch (Exception e) {
            LOGGER.error("Error connecting to Bilibili", e);
            String reason = e instanceof java.nio.channels.UnresolvedAddressException
                    ? "DNS Resolve Failed, please check your network or DNS settings."
                    : e.getMessage();
            broadcast(Component.translatable("mod.bilibilichatmcforge.error.app_start_failed", reason));
            isRunning = false;
        }
    }

    private List<String> checkConfig() {
        JsonConfigManager.ConfigData config = JsonConfigManager.getInstance();
        List<String> missing = new ArrayList<>();
        if (config.accessKey.isEmpty()) missing.add(Component.translatable("mod.bilibilichatmcforge.config.missing.access_key").getString());
        if (config.accessSecret.isEmpty()) missing.add(Component.translatable("mod.bilibilichatmcforge.config.missing.access_secret").getString());
        if (config.appId == 0) missing.add(Component.translatable("mod.bilibilichatmcforge.config.missing.app_id").getString());
        if (config.roomCode.isEmpty()) missing.add(Component.translatable("mod.bilibilichatmcforge.config.missing.room_code").getString());
        return missing;
    }

    private HttpResponse<String> post(String url, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        BilibiliAuth.getHeaders(body).forEach(builder::header);
        return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void sendHeartbeat() {
        if (!isRunning || gameId == null) return;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("game_id", gameId);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("https://live-open.biliapi.com/v2/app/heartbeat"))
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)));
            BilibiliAuth.getHeaders(GSON.toJson(body)).forEach(builder::header);
            HTTP_CLIENT.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOGGER.error("Error sending Bilibili heartbeat", e);
        }
    }

    public void stop() {
        if (!isRunning) return;
        isRunning = false;

        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
        if (wsHeartbeatTask != null) {
            wsHeartbeatTask.cancel(false);
            wsHeartbeatTask = null;
        }
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Stopping");
        }
        if (gameId != null) {
            JsonObject body = new JsonObject();
            body.addProperty("app_id", JsonConfigManager.getInstance().appId);
            body.addProperty("game_id", gameId);
            CompletableFuture.runAsync(() -> {
                try {
                    post("https://live-open.biliapi.com/v2/app/end", GSON.toJson(body));
                } catch (Exception e) {
                    LOGGER.error("Error ending Bilibili app", e);
                }
            });
        }
    }

    private void broadcast(Component msg) {
        server.execute(() -> server.getPlayerList().broadcastSystemMessage(msg, false));
    }

    /**
     * 弹幕 WebSocket。协议是 B 站自定义的二进制分包：
     * 16 字节头（包长/头长/协议版本/opcode/序号）+ 正文，op=5 为业务消息。
     */
    private class BilibiliWebSocketListener implements WebSocket.Listener {
        private final String authBody;

        BilibiliWebSocketListener(String authBody) {
            this.authBody = authBody;
        }

        @Override
        public void onOpen(WebSocket socket) {
            sendPacket(socket, 7, authBody);
            // 协议层心跳，30 秒一次；重连会走到新的 onOpen，旧任务先撤掉
            if (wsHeartbeatTask != null) wsHeartbeatTask.cancel(false);
            wsHeartbeatTask = SCHEDULER.scheduleAtFixedRate(() -> {
                if (isRunning) sendPacket(socket, 2, "");
            }, 30, 30, TimeUnit.SECONDS);
            WebSocket.Listener.super.onOpen(socket);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket socket, ByteBuffer data, boolean last) {
            try {
                handleBinary(data);
            } catch (Exception e) {
                LOGGER.warn("Failed to handle danmaku packet", e);
            }
            return WebSocket.Listener.super.onBinary(socket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
            LOGGER.info("Bilibili WebSocket closed: {} {}", statusCode, reason);
            if (isRunning) {
                LOGGER.info("Reconnecting in 5 seconds...");
                SCHEDULER.schedule(BilibiliClient.this::connect, 5, TimeUnit.SECONDS);
            }
            return WebSocket.Listener.super.onClose(socket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket socket, Throwable error) {
            LOGGER.error("Bilibili WebSocket error", error);
        }

        private void sendPacket(WebSocket ws, int op, String body) {
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            ByteBuffer packet = ByteBuffer.allocate(16 + payload.length).order(ByteOrder.BIG_ENDIAN);
            packet.putInt(16 + payload.length);
            packet.putShort((short) 16); // 头部长度
            packet.putShort((short) 1);  // 协议版本，正文裸 JSON
            packet.putInt(op);
            packet.putInt(1);
            packet.put(payload);
            packet.flip();
            ws.sendBinary(packet, true);
        }
    }

    private void handleBinary(ByteBuffer buf) throws IOException {
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

            if (op != 5) {
                continue; // 心跳回复(op=3)、认证回复(op=8)不用处理
            }
            if (protoVer == 2) {
                handleBinary(ByteBuffer.wrap(decompress(body)));
                return; // 解压出来是完整的包流，递归处理完即可
            }
            dispatch(GSON.fromJson(new String(body, StandardCharsets.UTF_8), JsonObject.class));
        }
    }

    private void dispatch(JsonObject json) {
        try {
            Component msg = parseMessage(json.get("cmd").getAsString(), json.getAsJsonObject("data"));
            if (msg != null) {
                server.execute(() -> server.getPlayerList().broadcastSystemMessage(msg, false));
            }
        } catch (Exception e) {
            // 字段缺失或格式变化的消息直接忽略
        }
    }

    private Component parseMessage(String cmd, JsonObject data) {
        if (data == null) return null;
        switch (cmd) {
            case "LIVE_OPEN_PLATFORM_DM":
                return Component.translatable("mod.bilibilichatmcforge.chat.danmaku",
                        data.get("uname").getAsString(), data.get("msg").getAsString());
            case "LIVE_OPEN_PLATFORM_SEND_GIFT":
                return Component.translatable("mod.bilibilichatmcforge.chat.gift",
                        data.get("uname").getAsString(), data.get("gift_name").getAsString(), data.get("gift_num").getAsInt());
            case "LIVE_OPEN_PLATFORM_SUPER_CHAT":
                return Component.translatable("mod.bilibilichatmcforge.chat.sc",
                        data.get("uname").getAsString(), data.get("rmb").getAsLong(), data.get("message").getAsString());
            case "LIVE_OPEN_PLATFORM_GUARD": {
                String uname = data.getAsJsonObject("user_info").get("uname").getAsString();
                return Component.translatable("mod.bilibilichatmcforge.chat.guard",
                        uname, guardName(data.get("guard_level").getAsInt()));
            }
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
    private byte[] decompress(byte[] compressed) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 4);
        byte[] chunk = new byte[8192];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(chunk);
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
