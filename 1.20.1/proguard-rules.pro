# ProGuard rules for BilibiliChat Mod

# Keep the mod entry point
-keep class net.ming.bilibilichatmcforge.Bilibilichatmcforge {
    <init>(...);
    public static final java.lang.String MODID;
}

# Keep all classes with @Mod annotation
-keep @net.minecraftforge.fml.common.Mod class * {
    <init>(...);
}

# Keep all classes with @Mod.EventBusSubscriber annotation
-keep @net.minecraftforge.fml.common.Mod$EventBusSubscriber class * {
    *;
}

# Keep @SubscribeEvent methods
-keepclassmembers class * {
    @net.minecraftforge.eventbus.api.SubscribeEvent <methods>;
}

# Keep Gson serialization classes
-keep class net.ming.bilibilichatmcforge.JsonConfigManager$ConfigData {
    *;
}

# Keep Forge config classes
-keep class net.ming.bilibilichatmcforge.Config {
    *;
}

# Keep client screen classes (referenced by name)
-keep class net.ming.bilibilichatmcforge.client.BilibiliConfigScreen {
    *;
}

# Keep utils classes
-keep class net.ming.bilibilichatmcforge.utils.BilibiliClient {
    *;
}

# Keep all Minecraft/Forge API references
-keep class net.minecraft.** { *; }
-keep class net.minecraftforge.** { *; }
-keep class com.mojang.** { *; }

# Keep SLF4J logger
-keep class org.slf4j.** { *; }

# Obfuscate everything else
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# Remove debug info
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Optimize
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
