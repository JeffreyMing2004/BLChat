package net.ming.bilibilichatmcforge.client;

import net.ming.bilibilichatmcforge.Bilibilichatmcforge;
import net.ming.bilibilichatmcforge.JsonConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BilibiliConfigScreen extends Screen {
    private final Screen lastScreen;
    private EditBox identityCodeField;

    public BilibiliConfigScreen(Screen lastScreen) {
        super(Component.translatable("mod.bilibilichatmcforge.config.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        JsonConfigManager.ConfigData config = JsonConfigManager.getInstance();

        int centerX = this.width / 2;
        int fieldWidth = 260;
        int fieldX = centerX - fieldWidth / 2;

        // Identity Code field
        this.identityCodeField = new EditBox(this.font, fieldX, 60, fieldWidth, 20, Component.empty());
        this.identityCodeField.setValue(config.identityCode);
        this.identityCodeField.setMaxLength(32);
        this.identityCodeField.setHint(Component.translatable("mod.bilibilichatmcforge.config.identity_code.hint"));
        this.addRenderableWidget(this.identityCodeField);

        // Save Button
        this.addRenderableWidget(Button.builder(Component.translatable("mod.bilibilichatmcforge.config.save"), (button) -> {
            JsonConfigManager.setIdentityCode(this.identityCodeField.getValue());
            Bilibilichatmcforge.restartClient();
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(centerX - 105, this.height - 27, 100, 20).build());

        // Cancel Button
        this.addRenderableWidget(Button.builder(Component.translatable("mod.bilibilichatmcforge.config.cancel"), (button) -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(centerX + 5, this.height - 27, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title.getString(), this.width / 2, 10, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("mod.bilibilichatmcforge.config.identity_code").getString(), this.width / 2 - 130, 48, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
