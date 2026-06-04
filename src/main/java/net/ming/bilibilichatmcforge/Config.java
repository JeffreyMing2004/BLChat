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
            .define("accessKey", "bq96FKKv15yroVpW1K77HRlZ");

    private static final ForgeConfigSpec.ConfigValue<String> ACCESS_SECRET = BUILDER
            .comment("Bilibili Open Live Access Secret")
            .define("accessSecret", "y5irBHscUC37KT5rq9SL0MhgKkDKks");

    private static final ForgeConfigSpec.LongValue APP_ID = BUILDER
            .comment("Bilibili Open Live App ID")
            .defineInRange("appId", 0L, 0L, Long.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String accessKey;
    public static String accessSecret;
    public static long appId;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        accessKey = ACCESS_KEY.get();
        accessSecret = ACCESS_SECRET.get();
        appId = APP_ID.get();
    }
}
