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
        super(Component.literal("Bilibili Chat Mod Configuration"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        JsonConfigManager.ConfigData config = JsonConfigManager.getInstance();

        int fieldWidth = 200;
        int fieldHeight = 20;
        int spacing = 30;
        int startY = this.height / 2 - 80;

        // Access Key
        this.accessKeyField = new EditBox(this.font, this.width / 2 - fieldWidth / 2, startY, fieldWidth, fieldHeight, Component.literal("Access Key"));
        this.accessKeyField.setValue(config.accessKey);
        this.accessKeyField.setMaxLength(128);
        this.addRenderableWidget(this.accessKeyField);

        // Access Secret
        this.accessSecretField = new EditBox(this.font, this.width / 2 - fieldWidth / 2, startY + spacing, fieldWidth, fieldHeight, Component.literal("Access Secret"));
        this.accessSecretField.setValue(config.accessSecret);
        this.accessSecretField.setMaxLength(128);
        this.addRenderableWidget(this.accessSecretField);

        // App ID
        this.appIdField = new EditBox(this.font, this.width / 2 - fieldWidth / 2, startY + spacing * 2, fieldWidth, fieldHeight, Component.literal("App ID"));
        this.appIdField.setValue(String.valueOf(config.appId));
        this.appIdField.setMaxLength(20);
        this.addRenderableWidget(this.appIdField);

        // Room Code
        this.roomCodeField = new EditBox(this.font, this.width / 2 - fieldWidth / 2, startY + spacing * 3, fieldWidth, fieldHeight, Component.literal("Room Code"));
        this.roomCodeField.setValue(config.roomCode);
        this.roomCodeField.setMaxLength(64);
        this.addRenderableWidget(this.roomCodeField);

        // Save Button
        this.addRenderableWidget(Button.builder(Component.literal("Save & Apply"), (button) -> {
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
        }).bounds(this.width / 2 - 105, startY + spacing * 4 + 10, 100, 20).build());

        // Cancel Button
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), (button) -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(this.width / 2 + 5, startY + spacing * 4 + 10, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        guiGraphics.drawString(this.font, "Access Key:", this.width / 2 - 100, this.accessKeyField.getY() - 12, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Access Secret:", this.width / 2 - 100, this.accessSecretField.getY() - 12, 0xA0A0A0);
        guiGraphics.drawString(this.font, "App ID:", this.width / 2 - 100, this.appIdField.getY() - 12, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Room Code (Identity Code):", this.width / 2 - 100, this.roomCodeField.getY() - 12, 0xA0A0A0);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
