package net.ming.bilibilichatmcforge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.ming.bilibilichatmcforge.JsonConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;
import java.io.ByteArrayOutputStream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class BilibiliClient {
    private static final String WSS_URL = "wss://broadcastlv.chat.bilibili.com/sub";
    private static final long APP_ID = 1779863002402L;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    private WebSocket webSocket;
    private final MinecraftServer server;
    private boolean isRunning = false;
    private java.util.concurrent.ScheduledFuture<?> heartbeatTask;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;

    public BilibiliClient(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        reconnectAttempts = 0;

        String identityCode = JsonConfigManager.getInstance().identityCode;
        if (identityCode == null || identityCode.isEmpty()) {
            LOGGER.error("Bilibili room code is not configured");
            server.execute(() -> server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("mod.bilibilichatmcforge.error.identity_code_missing"), false));
            isRunning = false;
            return;
        }

        CompletableFuture.runAsync(this::connect);
    }

    private long resolveRoomId(String identityCode) throws Exception {
        // 尝试直接解析为数字房间号
        try {
            long roomId = Long.parseLong(identityCode);
            return resolveShortRoomId(roomId);
        } catch (NumberFormatException ignored) {
        }

        // 非数字，作为身份码通过开放平台API获取房间号
        return resolveRoomIdByCode(identityCode);
    }

    private long resolveShortRoomId(long roomId) throws Exception {
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("https://api.live.bilibili.com/room/v1/Room/get_info?room_id=" + roomId))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();

        java.net.http.HttpResponse<String> response = HTTP_CLIENT.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

        JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
        if (json.get("code").getAsInt() == 0) {
            return json.getAsJsonObject("data").get("room_id").getAsLong();
        }

        throw new Exception("获取房间信息失败: " + json.get("message").getAsString());
    }

    private long resolveRoomIdByCode(String code) throws Exception {
        String accessKeyId = JsonConfigManager.getInstance().accessKeyId;
        String accessKeySecret = JsonConfigManager.getInstance().accessKeySecret;

        if (accessKeyId == null || accessKeyId.isEmpty() || accessKeySecret == null || accessKeySecret.isEmpty()) {
            throw new IllegalArgumentException(
                    "使用身份码需要配置 accessKeyId 和 accessKeySecret。\n"
                            + "请在B站直播开放平台(https://open-live.bilibili.com/)获取这些密钥，\n"
                            + "或直接使用数字房间号代替身份码。");
        }

        JsonObject body = new JsonObject();
        body.addProperty("app_id", APP_ID);
        body.addProperty("code", code);
        String bodyStr = GSON.toJson(body);

        String bodyMd5 = md5("");
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().replace("-", "");

        // 构造待签名字符串
        String stringToSign = "x-bili-accesskeyid:" + accessKeyId + "\n"
                + "x-bili-content-md5:" + bodyMd5 + "\n"
                + "x-bili-signature-method:HMAC-SHA256\n"
                + "x-bili-signature-nonce:" + nonce + "\n"
                + "x-bili-signature-version:2.0\n"
                + "x-bili-timestamp:" + timestamp;

        String signature = hmacSha256(accessKeySecret, stringToSign);

        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create("https://member.bilibili.com/arcopen/fn/live/room/info"))
                    .header("Content-Type", "application/json")
                    .header("x-bili-accesskeyid", accessKeyId)
                    .header("x-bili-content-md5", bodyMd5)
                    .header("x-bili-signature-method", "HMAC-SHA256")
                    .header("x-bili-signature-nonce", nonce)
                    .header("x-bili-signature-version", "2.0")
                    .header("x-bili-timestamp", String.valueOf(timestamp))
                    .header("Authorization", signature)
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = HTTP_CLIENT.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            LOGGER.info("开放平台API响应: status={}, body={}", response.statusCode(), response.body());

            String responseBody = response.body();
            if (responseBody == null || responseBody.isEmpty()) {
                throw new Exception("开放平台API返回空响应");
            }

            // 尝试解析JSON，如果不是JSON格式则返回原始内容
            if (!responseBody.trim().startsWith("{")) {
                throw new Exception("开放平台API返回非JSON响应: " + responseBody);
            }

            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            if (json.get("code").getAsInt() == 0) {
                JsonObject data = json.getAsJsonObject("data");
                long roomId = data.get("room_id").getAsLong();
                LOGGER.info("通过身份码获取到房间号: {}", roomId);
                return roomId;
            }

            throw new Exception("通过身份码获取房间号失败: " + json.get("message").getAsString());
        } catch (java.net.ConnectException e) {
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof java.nio.channels.UnresolvedAddressException) {
                    throw new Exception("无法解析 member.bilibili.com 域名，请检查网络连接或DNS设置。");
                }
                cause = cause.getCause();
            }
            throw new Exception("连接开放平台API失败: " + e.getMessage());
        }
    }

    private String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void connect() {
        try {
            String identityCode = JsonConfigManager.getInstance().identityCode;
            LOGGER.info("Resolving Bilibili live room {}...", identityCode);

            long roomId = resolveRoomId(identityCode);
            LOGGER.info("Resolved to room ID: {}", roomId);

            HTTP_CLIENT.newWebSocketBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Origin", "https://live.bilibili.com")
                    .buildAsync(URI.create(WSS_URL), new BilibiliWebSocketListener(roomId))
                    .thenAccept(ws -> {
                        this.webSocket = ws;
                        reconnectAttempts = 0;
                        LOGGER.info("Connected to Bilibili WebSocket for room {}", roomId);
                        server.execute(() -> server.getPlayerList().broadcastSystemMessage(
                                Component.translatable("mod.bilibilichatmcforge.info.connected"), false));
                    });
        } catch (Exception e) {
            LOGGER.error("Error connecting to Bilibili", e);
            String errorMsg = e.getMessage();
            if (e.getCause() instanceof java.nio.channels.UnresolvedAddressException) {
                errorMsg = "DNS解析失败，请检查网络连接";
            }
            String finalErrorMsg = errorMsg;
            server.execute(() -> server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("mod.bilibilichatmcforge.error.connect_failed", finalErrorMsg), false));
            isRunning = false;
        }
    }

    public void stop() {
        if (!isRunning) return;
        isRunning = false;

        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
            heartbeatTask = null;
        }

        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Stopping");
        }
    }

    private class BilibiliWebSocketListener implements WebSocket.Listener {
        private final long roomId;

        public BilibiliWebSocketListener(long roomId) {
            this.roomId = roomId;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            JsonObject authBody = new JsonObject();
            authBody.addProperty("uid", 0);
            authBody.addProperty("roomid", roomId);
            authBody.addProperty("protover", 3);
            authBody.addProperty("platform", "web");
            authBody.addProperty("type", 2);
            sendPacket(webSocket, 7, GSON.toJson(authBody));

            if (heartbeatTask != null) heartbeatTask.cancel(true);
            heartbeatTask = SCHEDULER.scheduleAtFixedRate(() -> {
                if (isRunning && webSocket != null) {
                    sendPacket(webSocket, 2, "");
                }
            }, 30, 30, TimeUnit.SECONDS);

            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            handleBinary(data);
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            LOGGER.info("Bilibili WebSocket closed: {} {}", statusCode, reason);
            if (isRunning) {
                reconnectAttempts++;
                if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
                    LOGGER.error("达到最大重连次数({})，停止重连", MAX_RECONNECT_ATTEMPTS);
                    server.execute(() -> server.getPlayerList().broadcastSystemMessage(
                            Component.translatable("mod.bilibilichatmcforge.error.connect_failed",
                                    "WebSocket连接失败，已达到最大重连次数"), false));
                    isRunning = false;
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }
                LOGGER.info("Reconnecting in 5 seconds... (attempt {}/{})", reconnectAttempts, MAX_RECONNECT_ATTEMPTS);
                SCHEDULER.schedule(BilibiliClient.this::connect, 5, TimeUnit.SECONDS);
            }
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            LOGGER.error("Bilibili WebSocket error", error);
        }

        private void sendPacket(WebSocket ws, int op, String body) {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            int totalLen = 16 + bodyBytes.length;
            ByteBuffer buffer = ByteBuffer.allocate(totalLen).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(totalLen);
            buffer.putShort((short) 16);
            buffer.putShort((short) 1);
            buffer.putInt(op);
            buffer.putInt(1);
            buffer.put(bodyBytes);
            buffer.flip();
            ws.sendBinary(buffer, true);
        }

        private void handleBinary(ByteBuffer data) {
            data.order(ByteOrder.BIG_ENDIAN);
            while (data.remaining() >= 16) {
                int totalLen = data.getInt();
                short headerLen = data.getShort();
                short protoVer = data.getShort();
                int op = data.getInt();
                int seq = data.getInt();

                int bodyLen = totalLen - headerLen;
                if (bodyLen > 0 && data.remaining() >= bodyLen) {
                    byte[] bodyBytes = new byte[bodyLen];
                    data.get(bodyBytes);

                    if (op == 5) {
                        if (protoVer == 2) {
                            try {
                                bodyBytes = decompress(bodyBytes);
                                handleBinary(ByteBuffer.wrap(bodyBytes));
                                continue;
                            } catch (Exception e) {
                                LOGGER.error("Error decompressing packet", e);
                            }
                        }

                        String jsonStr = new String(bodyBytes, StandardCharsets.UTF_8);
                        try {
                            JsonObject json = GSON.fromJson(jsonStr, JsonObject.class);
                            String cmd = json.get("cmd").getAsString();
                            Component chatMsg = parseMessage(cmd, json);
                            if (chatMsg != null) {
                                Component msg = chatMsg;
                                server.execute(() -> server.getPlayerList().broadcastSystemMessage(msg, false));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    break;
                }
            }
        }

        private Component parseMessage(String cmd, JsonObject json) {
            switch (cmd) {
                case "DANMU_MSG": {
                    JsonArray info = json.getAsJsonArray("info");
                    String msg = info.get(1).getAsString();
                    JsonArray userInfo = info.get(2).getAsJsonArray();
                    String uname = userInfo.get(1).getAsString();
                    return Component.translatable("mod.bilibilichatmcforge.chat.danmaku", uname, msg);
                }
                case "SEND_GIFT": {
                    JsonObject data = json.getAsJsonObject("data");
                    String uname = data.get("uname").getAsString();
                    String giftName = data.get("giftName").getAsString();
                    int num = data.get("num").getAsInt();
                    return Component.translatable("mod.bilibilichatmcforge.chat.gift", uname, giftName, num);
                }
                case "SUPER_CHAT_MESSAGE": {
                    JsonObject data = json.getAsJsonObject("data");
                    String uname = data.getAsJsonObject("user_info").get("uname").getAsString();
                    String message = data.get("message").getAsString();
                    long price = data.get("price").getAsLong();
                    return Component.translatable("mod.bilibilichatmcforge.chat.sc", uname, price, message);
                }
                case "GUARD_BUY": {
                    JsonObject data = json.getAsJsonObject("data");
                    String uname = data.get("username").getAsString();
                    String giftName = data.get("gift_name").getAsString();
                    return Component.translatable("mod.bilibilichatmcforge.chat.guard", uname, giftName);
                }
                default:
                    return null;
            }
        }

        private byte[] decompress(byte[] data) throws Exception {
            Inflater inflater = new Inflater();
            inflater.setInput(data);
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
                byte[] buffer = new byte[1024];
                while (!inflater.finished()) {
                    int count = inflater.inflate(buffer);
                    outputStream.write(buffer, 0, count);
                }
                return outputStream.toByteArray();
            } finally {
                inflater.end();
            }
        }
    }
}
