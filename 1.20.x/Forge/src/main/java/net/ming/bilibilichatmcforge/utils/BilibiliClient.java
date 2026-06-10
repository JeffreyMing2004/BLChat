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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;
import java.io.ByteArrayOutputStream;

public class BilibiliClient {
    private static final String WSS_URL = "wss://broadcastlv.chat.bilibili.com/sub";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    private WebSocket webSocket;
    private final MinecraftServer server;
    private boolean isRunning = false;
    private java.util.concurrent.ScheduledFuture<?> heartbeatTask;

    public BilibiliClient(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;

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

    private void connect() {
        try {
            String identityCode = JsonConfigManager.getInstance().identityCode;
            LOGGER.info("Connecting to Bilibili live room {}...", identityCode);

            HTTP_CLIENT.newWebSocketBuilder()
                    .buildAsync(URI.create(WSS_URL), new BilibiliWebSocketListener(identityCode))
                    .thenAccept(ws -> {
                        this.webSocket = ws;
                        LOGGER.info("Connected to Bilibili WebSocket for room {}", identityCode);
                        server.execute(() -> server.getPlayerList().broadcastSystemMessage(
                                Component.translatable("mod.bilibilichatmcforge.info.connected"), false));
                    });
        } catch (Exception e) {
            LOGGER.error("Error connecting to Bilibili", e);
            String errorMsg = e.getMessage();
            if (e.getCause() instanceof java.nio.channels.UnresolvedAddressException) {
                errorMsg = "DNS Resolve Failed. Please check your network.";
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
        private final String identityCode;

        public BilibiliWebSocketListener(String identityCode) {
            this.identityCode = identityCode;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            JsonObject authBody = new JsonObject();
            authBody.addProperty("uid", 0);
            authBody.addProperty("roomid", Long.parseLong(identityCode));
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
