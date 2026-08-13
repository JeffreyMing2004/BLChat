package net.ming.bilibilichatmcforge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class VersionChecker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String VERSION_URL = "https://version.mingpixel.net/1.21/version.blchat";
    private static final String CURRENT_VERSION = "1.0.0";

    public static void checkAsync(MinecraftServer server) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(VERSION_URL))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    LOGGER.warn("Version check failed: HTTP {}", response.statusCode());
                    return;
                }

                String body = response.body().trim();
                String latestVersion;
                String changelog = null;

                try {
                    JsonObject json = GSON.fromJson(body, JsonObject.class);
                    latestVersion = json.get("version").getAsString();
                    if (json.has("changelog")) {
                        changelog = json.get("changelog").getAsString();
                    }
                } catch (Exception e) {
                    latestVersion = body;
                }

                LOGGER.info("Version check: current={}, latest={}", CURRENT_VERSION, latestVersion);

                if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                    String finalChangelog = changelog;
                    String finalLatestVersion = latestVersion;
                    server.execute(() -> {
                        server.getPlayerList().broadcastSystemMessage(
                                Component.translatable("mod.bilibilichatmcforge.info.update_available", finalLatestVersion, CURRENT_VERSION),
                                false);
                        if (finalChangelog != null && !finalChangelog.isEmpty()) {
                            server.getPlayerList().broadcastSystemMessage(
                                    Component.translatable("mod.bilibilichatmcforge.info.update_changelog", finalChangelog),
                                    false);
                        }
                    });
                } else {
                    LOGGER.info("BLChat is up to date (version {})", CURRENT_VERSION);
                }
            } catch (Exception e) {
                LOGGER.warn("Version check failed: {}", e.getMessage());
            }
        });
    }

    private static boolean isNewerVersion(String remote, String local) {
        String cleanRemote = cleanVersionString(remote);
        String cleanLocal = cleanVersionString(local);
        String[] remoteParts = cleanRemote.split("\\.");
        String[] localParts = cleanLocal.split("\\.");

        int maxLen = Math.max(remoteParts.length, localParts.length);
        for (int i = 0; i < maxLen; i++) {
            int r = i < remoteParts.length ? parseVersionPart(remoteParts[i]) : 0;
            int l = i < localParts.length ? parseVersionPart(localParts[i]) : 0;
            if (r > l) return true;
            if (r < l) return false;
        }
        return false;
    }

    private static String cleanVersionString(String version) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(\\.\\d+)+)").matcher(version);
        if (m.find()) {
            return m.group(1);
        }
        m = java.util.regex.Pattern.compile("(\\d+)").matcher(version);
        if (m.find()) {
            return m.group(1);
        }
        return version;
    }

    private static int parseVersionPart(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9].*", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
