package net.ming.bilibilichatmcforge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.ming.bilibilichatmcforge.JsonConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;
import java.io.ByteArrayOutputStream;

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

    public BilibiliClient(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;

        JsonConfigManager.ConfigData config = JsonConfigManager.getInstance();
        List<String> missingFields = new ArrayList<>();
        if (config.accessKey.isEmpty()) missingFields.add(Component.translatable("mod.bilibilichatmcforge.config.missing.access_key").getString());
        if (config.accessSecret.isEmpty()) missingFields.add(Component.translatable("mod.bilibilichatmcforge.config.missing.access_secret").getString());
        if (config.appId == 0) missingFields.add(Component.translatable("mod.bilibilichatmcforge.config.missing.app_id").getString());
        if (config.roomCode.isEmpty()) missingFields.add(Component.translatable("mod.bilibilichatmcforge.config.missing.room_code").getString());

        if (!missingFields.isEmpty()) {
            String missing = String.join(", ", missingFields);
            LOGGER.error("Bilibili config is incomplete. Missing: {}", missing);
            server.execute(() -> server.getPlayerList().broadcastSystemMessage(Component.translatable("mod.bilibilichatmcforge.error.config_incomplete", missing), false));
            isRunning = false;
            return;
        }

        CompletableFuture.runAsync(this::connect);
    }

    private void connect() {
        try {
            JsonConfigManager.ConfigData config = JsonConfigManager.getInstance();
            // Try live-open.biliapi.com as it's more reliable in some environments
            String url = "https://live-open.biliapi.com/v2/app/start";
            JsonObject body = new JsonObject();
            body.addProperty("app_id", config.appId);
            body.addProperty("code", config.roomCode);
            String bodyStr = GSON.toJson(body);

            Map<String, String> headers = BilibiliAuth.getHeaders(bodyStr);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyStr));

            headers.forEach(requestBuilder::header);

            HttpResponse<String> response = HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            JsonObject respJson = GSON.fromJson(response.body(), JsonObject.class);

            if (respJson.get("code").getAsInt() != 0) {
                String errorMsg = respJson.get("message").getAsString();
                LOGGER.error("Failed to start Bilibili app: {}", errorMsg);
                server.execute(() -> server.getPlayerList().broadcastSystemMessage(Component.translatable("mod.bilibilichatmcforge.error.app_start_failed", errorMsg), false));
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
                        LOGGER.info("Connected to Bilibili WebSocket");
                        server.execute(() -> server.getPlayerList().broadcastSystemMessage(Component.translatable("mod.bilibilichatmcforge.info.connected"), false));
                    });

        } catch (Exception e) {
            LOGGER.error("Error connecting to Bilibili", e);
            String errorMsg = e.getMessage();
            if (e.getCause() instanceof java.nio.channels.UnresolvedAddressException || e instanceof java.nio.channels.UnresolvedAddressException) {
                errorMsg = "DNS Resolve Failed (UnresolvedAddressException). Please check your network or DNS settings.";
            }
            String finalErrorMsg = errorMsg;
            server.execute(() -> server.getPlayerList().broadcastSystemMessage(Component.translatable("mod.bilibilichatmcforge.error.app_start_failed", finalErrorMsg), false));
            isRunning = false;
        }
    }

    private void sendHeartbeat() {
        if (!isRunning || gameId == null) return;
        try {
            String url = "https://live-open.biliapi.com/v2/app/heartbeat";
            JsonObject body = new JsonObject();
            body.addProperty("game_id", gameId);
            String bodyStr = GSON.toJson(body);

            Map<String, String> headers = BilibiliAuth.getHeaders(bodyStr);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyStr));

            headers.forEach(requestBuilder::header);
            HTTP_CLIENT.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOGGER.error("Error sending Bilibili heartbeat", e);
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
            JsonConfigManager.ConfigData config = JsonConfigManager.getInstance();
            CompletableFuture.runAsync(() -> {
                try {
                    String url = "https://live-open.biliapi.com/v2/app/end";
                    JsonObject body = new JsonObject();
                    body.addProperty("app_id", config.appId);
                    body.addProperty("game_id", gameId);
                    String bodyStr = GSON.toJson(body);

                    Map<String, String> headers = BilibiliAuth.getHeaders(bodyStr);
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .POST(HttpRequest.BodyPublishers.ofString(bodyStr));

                    headers.forEach(requestBuilder::header);
                    HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
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
                LOGGER.info("Reconnecting in 5 seconds...");
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
                    
                    if (op == 5) { // Data
                        if (protoVer == 2) {
                            try {
                                bodyBytes = decompress(bodyBytes);
                                handleBinary(ByteBuffer.wrap(bodyBytes));
                                continue;
                            } catch (Exception e) {
                                LOGGER.error("Error decompressing Bilibili packet", e);
                            }
                        }
                        
                        String jsonStr = new String(bodyBytes, StandardCharsets.UTF_8);
                        try {
                            JsonObject json = GSON.fromJson(jsonStr, JsonObject.class);
                            JsonObject msgData = json.getAsJsonObject("data");
                            Component finalChatMsg = null;

                            switch (json.get("cmd").getAsString()) {
                                case "LIVE_OPEN_PLATFORM_DM":
                                    String uname = msgData.get("uname").getAsString();
                                    String msg = msgData.get("msg").getAsString();
                                    finalChatMsg = Component.translatable("mod.bilibilichatmcforge.chat.danmaku", uname, msg);
                                    break;
                                case "LIVE_OPEN_PLATFORM_SEND_GIFT":
                                    String giftUname = msgData.get("uname").getAsString();
                                    String giftName = msgData.get("gift_name").getAsString();
                                    int giftNum = msgData.get("gift_num").getAsInt();
                                    finalChatMsg = Component.translatable("mod.bilibilichatmcforge.chat.gift", giftUname, giftName, giftNum);
                                    break;
                                case "LIVE_OPEN_PLATFORM_SUPER_CHAT":
                                    String scUname = msgData.get("uname").getAsString();
                                    String scMsg = msgData.get("message").getAsString();
                                    long rmb = msgData.get("rmb").getAsLong();
                                    finalChatMsg = Component.translatable("mod.bilibilichatmcforge.chat.sc", scUname, rmb, scMsg);
                                    break;
                                case "LIVE_OPEN_PLATFORM_GUARD":
                                    String guardUname = msgData.getAsJsonObject("user_info").get("uname").getAsString();
                                    String guardName = msgData.get("guard_level").getAsInt() == 1 ? "总督" : 
                                                      msgData.get("guard_level").getAsInt() == 2 ? "提督" : "舰长";
                                    finalChatMsg = Component.translatable("mod.bilibilichatmcforge.chat.guard", guardUname, guardName);
                                    break;
                            }

                            if (finalChatMsg != null) {
                                Component chatComponent = finalChatMsg;
                                server.execute(() -> server.getPlayerList().broadcastSystemMessage(chatComponent, false));
                            }
                        } catch (Exception e) {
                            // Ignore non-json or other errors
                        }
                    }
                } else {
                    break;
                }
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
