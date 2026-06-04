package net.ming.bilibilichatmcforge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = Bilibilichatmcforge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> ACCESS_KEY = BUILDER
            .comment("Bilibili Open Live Access Key")
            .define("accessKey", "");

    private static final ForgeConfigSpec.ConfigValue<String> ACCESS_SECRET = BUILDER
            .comment("Bilibili Open Live Access Secret")
            .define("accessSecret", "");

    private static final ForgeConfigSpec.LongValue APP_ID = BUILDER
            .comment("Bilibili Open Live App ID")
            .defineInRange("appId", 0L, 0L, Long.MAX_VALUE);

    private static final ForgeConfigSpec.ConfigValue<String> ROOM_CODE = BUILDER
            .comment("Bilibili Open Live Room Code (Anchor's identity code)")
            .define("roomCode", "");

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String accessKey;
    public static String accessSecret;
    public static long appId;
    public static String roomCode;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        JsonConfigManager.setAuth(ACCESS_KEY.get(), ACCESS_SECRET.get(), APP_ID.get());
        JsonConfigManager.setRoomCode(ROOM_CODE.get());
    }
}
