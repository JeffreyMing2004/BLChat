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
    private EditBox accessKeyField;
    private EditBox accessSecretField;
    private EditBox appIdField;
    private EditBox roomCodeField;

    public BilibiliConfigScreen(Screen lastScreen) {
        super(Component.translatable("mod.bilibilichatmcforge.config.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        JsonConfigManager.ConfigData config = JsonConfigManager.getInstance();

        int fieldWidth = 260;
        int fieldHeight = 24;
        int spacing = 50;
        int startY = this.height / 2 - 120;

        // Access Key
        this.accessKeyField = new EditBox(this.font, this.width / 2 - fieldWidth / 2, startY, fieldWidth, fieldHeight, Component.translatable("mod.bilibilichatmcforge.config.access_key"));
        this.accessKeyField.setValue(config.accessKey);
        this.accessKeyField.setMaxLength(256);
        this.addRenderableWidget(this.accessKeyField);

        // Access Secret
        this.accessSecretField = new EditBox(this.font, this.width / 2 - fieldWidth / 2, startY + spacing, fieldWidth, fieldHeight, Component.translatable("mod.bilibilichatmcforge.config.access_secret"));
        this.accessSecretField.setValue(config.accessSecret);
        this.accessSecretField.setMaxLength(256);
        this.addRenderableWidget(this.accessSecretField);

        // App ID
        this.appIdField = new EditBox(this.font, this.width / 2 - fieldWidth / 2, startY + spacing * 2, fieldWidth, fieldHeight, Component.translatable("mod.bilibilichatmcforge.config.app_id"));
        this.appIdField.setValue(String.valueOf(config.appId));
        this.appIdField.setMaxLength(20);
        this.addRenderableWidget(this.appIdField);

        // Room Code
        this.roomCodeField = new EditBox(this.font, this.width / 2 - fieldWidth / 2, startY + spacing * 3, fieldWidth, fieldHeight, Component.translatable("mod.bilibilichatmcforge.config.room_code"));
        this.roomCodeField.setValue(config.roomCode);
        this.roomCodeField.setMaxLength(64);
        this.addRenderableWidget(this.roomCodeField);

        // Save Button
        this.addRenderableWidget(Button.builder(Component.translatable("mod.bilibilichatmcforge.config.save"), (button) -> {
            config.accessKey = this.accessKeyField.getValue();
            config.accessSecret = this.accessSecretField.getValue();
            try {
                config.appId = Long.parseLong(this.appIdField.getValue());
            } catch (NumberFormatException e) {
                config.appId = 0;
            }
            config.roomCode = this.roomCodeField.getValue();
            
            JsonConfigManager.save();
            
            // Trigger restart
            Bilibilichatmcforge.restartClient();
            
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(this.width / 2 - 105, startY + spacing * 4, 100, 20).build());

        // Cancel Button
        this.addRenderableWidget(Button.builder(Component.translatable("mod.bilibilichatmcforge.config.cancel"), (button) -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(this.width / 2 + 5, startY + spacing * 4, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        
        int labelX = this.width / 2 - 130;
        int labelOffset = 14;
        guiGraphics.drawString(this.font, Component.translatable("mod.bilibilichatmcforge.config.access_key"), labelX, this.accessKeyField.getY() - labelOffset, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("mod.bilibilichatmcforge.config.access_secret"), labelX, this.accessSecretField.getY() - labelOffset, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("mod.bilibilichatmcforge.config.app_id"), labelX, this.appIdField.getY() - labelOffset, 0xA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("mod.bilibilichatmcforge.config.room_code"), labelX, this.roomCodeField.getY() - labelOffset, 0xA0A0A0);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
