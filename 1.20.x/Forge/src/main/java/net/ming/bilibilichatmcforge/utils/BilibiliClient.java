package net.ming.bilibilichatmcforge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import java.util.List;
import java.util.Map;
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
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    private WebSocket webSocket;
    private final MinecraftServer server;
    private String gameId;
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
            LOGGER.error("Bilibili identity code is not configured");
            server.execute(() -> server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("mod.bilibilichatmcforge.error.identity_code_missing"), false));
            isRunning = false;
            return;
        }

        CompletableFuture.runAsync(this::connect);
    }

    private void connect() {
        try {
            JsonConfigManager.ConfigData config = JsonConfigManager.getInstance();
            String url = "https://open-live.bilibili.com/v2/app/start";
            JsonObject body = new JsonObject();
            body.addProperty("app_id", APP_ID);
            body.addProperty("code", config.identityCode);
            String bodyStr = GSON.toJson(body);

            Map<String, String> headers = getHeaders(bodyStr);
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bodyStr));

            headers.forEach(requestBuilder::header);

            java.net.http.HttpResponse<String> response = HTTP_CLIENT.send(requestBuilder.build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            LOGGER.debug("HTTP Response Code: {}", response.statusCode());
            LOGGER.debug("HTTP Response Body: {}", response.body());
            JsonObject respJson = GSON.fromJson(response.body(), JsonObject.class);

            if (respJson.get("code").getAsInt() != 0) {
                String errorMsg = respJson.get("message").getAsString();
                LOGGER.error("Failed to start Bilibili app: {}", errorMsg);
                server.execute(() -> server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("mod.bilibilichatmcforge.error.app_start_failed", errorMsg), false));
                isRunning = false;
                return;
            }

            JsonObject data = respJson.getAsJsonObject("data");
            gameId = data.get("game_info").getAsJsonObject().get("game_id").getAsString();

            // Start Project Heartbeat (v2/app/heartbeat) - every 20 seconds
            if (heartbeatTask != null) heartbeatTask.cancel(true);
            heartbeatTask = SCHEDULER.scheduleAtFixedRate(this::sendHeartbeat, 20, 20, TimeUnit.SECONDS);

            JsonObject websocketInfo = data.getAsJsonObject("websocket_info");
            String authBody = websocketInfo.get("auth_body").getAsString();
            List<String> wssLinks = GSON.fromJson(websocketInfo.get("wss_link"), List.class);

            if (wssLinks.isEmpty()) {
                LOGGER.error("No WSS links provided by Bilibili.");
                isRunning = false;
                return;
            }

            String wssUrl = wssLinks.get(0);
            HTTP_CLIENT.newWebSocketBuilder()
                    .buildAsync(URI.create(wssUrl), new BilibiliWebSocketListener(authBody))
                    .thenAccept(ws -> {
                        this.webSocket = ws;
                        reconnectAttempts = 0;
                        LOGGER.info("Connected to Bilibili WebSocket");
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

    private void sendHeartbeat() {
        if (!isRunning || gameId == null) return;
        try {
            String url = "https://open-live.bilibili.com/v2/app/heartbeat";
            JsonObject body = new JsonObject();
            body.addProperty("game_id", gameId);
            String bodyStr = GSON.toJson(body);

            Map<String, String> headers = getHeaders(bodyStr);
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bodyStr));

            headers.forEach(requestBuilder::header);
            HTTP_CLIENT.sendAsync(requestBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOGGER.error("Error sending Bilibili heartbeat", e);
        }
    }

    private static final String ACCESS_KEY_ID = "bq96FKKv15yroVpW1K77HRlZ";
    private static final String ACCESS_KEY_SECRET = "5irBHscUC37KT5rq9SL0MhgKkDKks";
    private static final long APP_ID = 1779863002402L;

    private Map<String, String> getHeaders(String body) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = UUID.randomUUID().toString();
        String contentMd5 = md5(body);

        LOGGER.debug("Signing body: {}", body);
        LOGGER.debug("Content-MD5: {}", contentMd5);

        Map<String, String> headerMap = new java.util.TreeMap<>();
        headerMap.put("x-bili-accesskeyid", ACCESS_KEY_ID);
        headerMap.put("x-bili-content-md5", contentMd5);
        headerMap.put("x-bili-signature-method", "HMAC-SHA256");
        headerMap.put("x-bili-signature-nonce", nonce);
        headerMap.put("x-bili-signature-version", "1.0");
        headerMap.put("x-bili-timestamp", timestamp);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }

        String stringToSign = sb.toString();
        LOGGER.debug("String to sign:\n{}", stringToSign);

        String signature = hmacSha256(ACCESS_KEY_SECRET, stringToSign);
        LOGGER.debug("Signature: {}", signature);

        headerMap.put("Authorization", signature);
        headerMap.put("Content-Type", "application/json");
        headerMap.put("Accept", "application/json");

        return headerMap;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
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

        if (gameId != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    String url = "https://open-live.bilibili.com/v2/app/end";
                    JsonObject body = new JsonObject();
                    body.addProperty("app_id", APP_ID);
                    body.addProperty("game_id", gameId);
                    String bodyStr = GSON.toJson(body);

                    Map<String, String> headers = getHeaders(bodyStr);
                    java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bodyStr));

                    headers.forEach(requestBuilder::header);
                    HTTP_CLIENT.send(requestBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
                } catch (Exception e) {
                    LOGGER.error("Error ending Bilibili app", e);
                }
            });
        }
    }

    private class BilibiliWebSocketListener implements WebSocket.Listener {
        private final String authBody;

        public BilibiliWebSocketListener(String authBody) {
            this.authBody = authBody;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            sendPacket(webSocket, 7, authBody);
            SCHEDULER.scheduleAtFixedRate(() -> {
                if (isRunning) {
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
