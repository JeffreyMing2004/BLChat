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

    public BilibiliClient(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;

        if (Config.accessKey.isEmpty() || Config.accessSecret.isEmpty() || Config.appId == 0 || Config.roomCode.isEmpty()) {
            LOGGER.error("Bilibili config is incomplete. Please check your config file.");
            return;
        }

        CompletableFuture.runAsync(this::connect);
    }

    private void connect() {
        try {
            String url = "https://live-open.bilibili.com/v2/app/start";
            JsonObject body = new JsonObject();
            body.addProperty("app_id", Config.appId);
            body.addProperty("code", Config.roomCode);
            String bodyStr = GSON.toJson(body);

            Map<String, String> headers = BilibiliAuth.getHeaders(bodyStr);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyStr));

            headers.forEach(requestBuilder::header);

            HttpResponse<String> response = HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            JsonObject respJson = GSON.fromJson(response.body(), JsonObject.class);

            if (respJson.get("code").getAsInt() != 0) {
                LOGGER.error("Failed to start Bilibili app: {}", respJson.get("message").getAsString());
                isRunning = false;
                return;
            }

            JsonObject data = respJson.getAsJsonObject("data");
            gameId = data.get("game_info").getAsJsonObject().get("game_id").getAsString();
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
                    });

        } catch (Exception e) {
            LOGGER.error("Error connecting to Bilibili", e);
            isRunning = false;
        }
    }

    public void stop() {
        if (!isRunning) return;
        isRunning = false;

        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Stopping");
        }

        if (gameId != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    String url = "https://live-open.bilibili.com/v2/app/end";
                    JsonObject body = new JsonObject();
                    body.addProperty("app_id", Config.appId);
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
        public void onClose(WebSocket webSocket, int statusCode, String reason) {
            LOGGER.info("Bilibili WebSocket closed: {} {}", statusCode, reason);
            if (isRunning) {
                LOGGER.info("Reconnecting in 5 seconds...");
                SCHEDULER.schedule(BilibiliClient.this::connect, 5, TimeUnit.SECONDS);
            }
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
                            String chatMsg = null;

                            switch (json.get("cmd").getAsString()) {
                                case "LIVE_OPEN_PLATFORM_DM":
                                    String uname = msgData.get("uname").getAsString();
                                    String msg = msgData.get("msg").getAsString();
                                    chatMsg = String.format("[Bilibili 弹幕] %s: %s", uname, msg);
                                    break;
                                case "LIVE_OPEN_PLATFORM_SEND_GIFT":
                                    String giftUname = msgData.get("uname").getAsString();
                                    String giftName = msgData.get("gift_name").getAsString();
                                    int giftNum = msgData.get("gift_num").getAsInt();
                                    chatMsg = String.format("[Bilibili 礼物] %s 赠送了 %s x%d", giftUname, giftName, giftNum);
                                    break;
                                case "LIVE_OPEN_PLATFORM_SUPER_CHAT":
                                    String scUname = msgData.get("uname").getAsString();
                                    String scMsg = msgData.get("message").getAsString();
                                    long rmb = msgData.get("rmb").getAsLong();
                                    chatMsg = String.format("[Bilibili SC] %s (￥%d): %s", scUname, rmb, scMsg);
                                    break;
                                case "LIVE_OPEN_PLATFORM_GUARD":
                                    String guardUname = msgData.getAsJsonObject("user_info").get("uname").getAsString();
                                    String guardName = msgData.get("guard_level").getAsInt() == 1 ? "总督" : 
                                                      msgData.get("guard_level").getAsInt() == 2 ? "提督" : "舰长";
                                    chatMsg = String.format("[Bilibili 大航海] 欢迎 %s 成为 %s", guardUname, guardName);
                                    break;
                            }

                            if (chatMsg != null) {
                                String finalChatMsg = chatMsg;
                                server.execute(() -> server.getPlayerList().broadcastSystemMessage(Component.literal(finalChatMsg), false));
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
