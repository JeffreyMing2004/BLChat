package net.ming.bilibilichatmcforge;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> IDENTITY_CODE = BUILDER
            .comment("Bilibili Live Identity Code")
            .define("identityCode", "");

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String getIdentityCode() {
        return IDENTITY_CODE.get();
    }

    public static void setIdentityCode(String code) {
        IDENTITY_CODE.set(code);
    }
}
