package net.ming.bilibilichatmcforge;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public Bilibilichatmcforge(FMLJavaModLoadingContext context) {
        JsonConfigManager.load();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, lastScreen) -> new BilibiliConfigScreen(lastScreen)));
    }

    private void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("bilibili")
                        .requires(source -> source.hasPermission(2))
                        .then(net.minecraft.commands.Commands.literal("identitycode")
                                .then(net.minecraft.commands.Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .executes(context -> {
                                            String id = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "id");
                                            JsonConfigManager.setIdentityCode(id);
                                            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("mod.bilibilichatmcforge.chat.identity_code_updated", id), true);
                                            if (bilibiliClient != null) {
                                                context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("mod.bilibilichatmcforge.chat.restarting"), true);
                                                bilibiliClient.stop();
                                                bilibiliClient.start();
                                            }
                                            return 1;
                                        }))));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("BilibiliChat Mod starting...");
        bilibiliClient = new BilibiliClient(event.getServer());
        bilibiliClient.start();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (bilibiliClient != null) {
            bilibiliClient.stop();
        }
    }
}
