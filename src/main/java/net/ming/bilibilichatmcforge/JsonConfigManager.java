package net.ming.bilibilichatmcforge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class JsonConfigManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("bilibilichat-config.json");
    
    private static ConfigData instance;

    public static class ConfigData {
        public String accessKey = "";
        public String accessSecret = "";
        public long appId = 0;
        public String roomCode = "";
    }

    public static void load() {
        File configFile = CONFIG_PATH.toFile();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                instance = GSON.fromJson(reader, ConfigData.class);
            } catch (IOException e) {
                LOGGER.error("Could not load config.json", e);
                instance = new ConfigData();
            }
        } else {
            instance = new ConfigData();
            // Try loading dev defaults if not in production
            if (!FMLLoader.isProduction()) {
                try (InputStream is = JsonConfigManager.class.getResourceAsStream("/dev_config.json")) {
                    if (is != null) {
                        try (InputStreamReader reader = new InputStreamReader(is)) {
                            ConfigData devData = GSON.fromJson(reader, ConfigData.class);
                            if (devData != null) {
                                instance.accessKey = devData.accessKey;
                                instance.accessSecret = devData.accessSecret;
                                instance.appId = devData.appId;
                                LOGGER.info("Loaded development authentication defaults from dev_config.json");
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to load dev_config.json: {}", e.getMessage());
                }
            }
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            LOGGER.error("Could not save config.json", e);
        }
    }

    public static ConfigData getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void setRoomCode(String roomCode) {
        getInstance().roomCode = roomCode;
        save();
    }

    public static void setAuth(String accessKey, String accessSecret, long appId) {
        ConfigData data = getInstance();
        data.accessKey = accessKey;
        data.accessSecret = accessSecret;
        data.appId = appId;
        save();
    }
}
