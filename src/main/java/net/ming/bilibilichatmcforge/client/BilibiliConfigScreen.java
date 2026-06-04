package net.ming.bilibilichatmcforge.client;

import net.ming.bilibilichatmcforge.Bilibilichatmcforge;
import net.ming.bilibilichatmcforge.JsonConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import java.util.List;
import java.util.Collections;

public class BilibiliConfigScreen extends Screen {
    private final Screen lastScreen;
    private ConfigList list;

    public BilibiliConfigScreen(Screen lastScreen) {
        super(Component.translatable("mod.bilibilichatmcforge.config.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.list = new ConfigList(this.minecraft, this.width, this.height, 32, this.height - 32, 50);
        this.addRenderableWidget(this.list);

        // Save Button
        this.addRenderableWidget(Button.builder(Component.translatable("mod.bilibilichatmcforge.config.save"), (button) -> {
            JsonConfigManager.ConfigData config = JsonConfigManager.getInstance();
            config.accessKey = this.list.accessKeyField.getValue();
            config.accessSecret = this.list.accessSecretField.getValue();
            try {
                config.appId = Long.parseLong(this.list.appIdField.getValue());
            } catch (NumberFormatException e) {
                config.appId = 0;
            }
            config.roomCode = this.list.roomCodeField.getValue();
            
            JsonConfigManager.save();
            Bilibilichatmcforge.restartClient();
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(this.width / 2 - 105, this.height - 27, 100, 20).build());

        // Cancel Button
        this.addRenderableWidget(Button.builder(Component.translatable("mod.bilibilichatmcforge.config.cancel"), (button) -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(this.width / 2 + 5, this.height - 27, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.list.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    class ConfigList extends ContainerObjectSelectionList<ConfigList.Entry> {
        public EditBox accessKeyField;
        public EditBox accessSecretField;
        public EditBox appIdField;
        public EditBox roomCodeField;

        public ConfigList(net.minecraft.client.Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
            JsonConfigManager.ConfigData config = JsonConfigManager.getInstance();

            this.accessKeyField = createField(config.accessKey, 256);
            this.addEntry(new Entry(Component.translatable("mod.bilibilichatmcforge.config.access_key"), this.accessKeyField));

            this.accessSecretField = createField(config.accessSecret, 256);
            this.addEntry(new Entry(Component.translatable("mod.bilibilichatmcforge.config.access_secret"), this.accessSecretField));

            this.appIdField = createField(String.valueOf(config.appId), 20);
            this.addEntry(new Entry(Component.translatable("mod.bilibilichatmcforge.config.app_id"), this.appIdField));

            this.roomCodeField = createField(config.roomCode, 64);
            this.addEntry(new Entry(Component.translatable("mod.bilibilichatmcforge.config.room_code"), this.roomCodeField));
        }

        private EditBox createField(String value, int maxLen) {
            EditBox field = new EditBox(BilibiliConfigScreen.this.font, 0, 0, 260, 20, Component.empty());
            field.setValue(value);
            field.setMaxLength(maxLen);
            return field;
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 155;
        }

        class Entry extends ContainerObjectSelectionList.Entry<Entry> {
            private final Component label;
            private final EditBox field;

            public Entry(Component label, EditBox field) {
                this.label = label;
                this.field = field;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                guiGraphics.drawString(BilibiliConfigScreen.this.font, this.label, left, top + 2, 0xA0A0A0);
                this.field.setX(left);
                this.field.setY(top + 15);
                this.field.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return Collections.singletonList(this.field);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return Collections.singletonList(this.field);
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
