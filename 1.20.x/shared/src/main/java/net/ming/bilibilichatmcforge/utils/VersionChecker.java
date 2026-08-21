package net.ming.bilibilichatmcforge.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class VersionChecker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String VERSION_URL = "https://version.mingpixel.net/1.20/version.blchat";
    private static final String CURRENT_VERSION = loadCurrentVersion();

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
        int[] remoteParts = parseVersionParts(remote);
        int[] localParts = parseVersionParts(local);

        int maxLen = Math.max(remoteParts.length, localParts.length);
        for (int i = 0; i < maxLen; i++) {
            int r = i < remoteParts.length ? remoteParts[i] : 0;
            int l = i < localParts.length ? localParts[i] : 0;
            if (r > l) return true;
            if (r < l) return false;
        }
        return false;
    }

    private static int[] parseVersionParts(String version) {
        java.util.List<Integer> parts = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(version);
        while (m.find()) {
            try {
                parts.add(Integer.parseInt(m.group()));
            } catch (NumberFormatException e) {
                parts.add(0);
            }
        }
        int[] result = new int[parts.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = parts.get(i);
        }
        return result;
    }

    private static String loadCurrentVersion() {
        try (InputStream is = VersionChecker.class.getResourceAsStream("/blchat-version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String v = props.getProperty("version");
                if (v != null && !v.trim().isEmpty()) {
                    return v.trim();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read bundled version info, falling back to default", e);
        }
        return "1.0.4.0";
    }
}
