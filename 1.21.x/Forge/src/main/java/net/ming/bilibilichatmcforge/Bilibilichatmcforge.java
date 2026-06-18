package net.ming.bilibilichatmcforge;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.client.ConfigScreenHandler;
import net.ming.bilibilichatmcforge.client.BilibiliConfigScreen;
import net.ming.bilibilichatmcforge.utils.BilibiliClient;
import org.slf4j.Logger;

@Mod(Bilibilichatmcforge.MODID)
public class Bilibilichatmcforge {

    public static final String MODID = "bilibilichatmcforge";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static BilibiliClient bilibiliClient;

    public static void restartClient() {
        if (bilibiliClient != null) {
            bilibiliClient.stop();
            bilibiliClient.start();
        }
    }

    public Bilibilichatmcforge() {
        JsonConfigManager.load();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, lastScreen) -> new BilibiliConfigScreen(lastScreen)));

        LOGGER.info("BLChat mod loaded successfully!");
    }

    public static void startBilibiliClient(net.minecraft.server.MinecraftServer server) {
        if (bilibiliClient == null) {
            bilibiliClient = new BilibiliClient(server);
        }
        bilibiliClient.start();
    }

    public static void stopBilibiliClient() {
        if (bilibiliClient != null) {
            bilibiliClient.stop();
        }
    }
}
