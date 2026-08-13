package net.ming.bilibilichatmcforge.client;

import net.ming.bilibilichatmcforge.Bilibilichatmcforge;
import net.ming.bilibilichatmcforge.JsonConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

        this.identityCodeField = new EditBox(this.font, fieldX, 60, fieldWidth, 20, Component.empty());
        this.identityCodeField.setValue(config.identityCode);
        this.identityCodeField.setMaxLength(64);
        this.identityCodeField.setHint(Component.translatable("mod.bilibilichatmcforge.config.identity_code.hint"));
        this.addRenderableWidget(this.identityCodeField);

        this.addRenderableWidget(Button.builder(Component.translatable("mod.bilibilichatmcforge.config.save"), (button) -> {
            JsonConfigManager.setIdentityCode(this.identityCodeField.getValue());
            Bilibilichatmcforge.restartClient();
            this.minecraft.gui.setScreen(this.lastScreen);
        }).bounds(centerX - 105, this.height - 27, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("mod.bilibilichatmcforge.config.cancel"), (button) -> {
            this.minecraft.gui.setScreen(this.lastScreen);
        }).bounds(centerX + 5, this.height - 27, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("mod.bilibilichatmcforge.config.identity_code"), this.width / 2 - 130, 48, 0xFFA0A0A0);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }
}
