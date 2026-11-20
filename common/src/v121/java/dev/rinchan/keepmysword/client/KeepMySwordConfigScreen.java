package dev.rinchan.keepmysword.client;

import dev.rinchan.keepmysword.config.ExclusionMatcher;
import dev.rinchan.keepmysword.config.KeepMySwordConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Native zero-dependency configuration screen using vanilla Minecraft widgets. */
public final class KeepMySwordConfigScreen extends Screen {
    private final Screen parent;
    private boolean allowMending;
    private boolean showNotification;
    private boolean showOverlay;
    private String excludedText;
    private EditBox excludedBox;

    public KeepMySwordConfigScreen(Screen parent) {
        super(Component.translatable("screen.keep_my_sword.title"));
        this.parent = parent;
        KeepMySwordConfig cfg = KeepMySwordConfig.get();
        this.allowMending = cfg.allowMendingWhenBroken();
        this.showNotification = cfg.showBreakNotification();
        this.showOverlay = cfg.showBrokenOverlay();
        this.excludedText = ExclusionMatcher.formatCommaSeparated(cfg.excludedItems());
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int fieldWidth = Math.min(300, this.width - 40);
        int left = centerX - fieldWidth / 2;
        int y = 44;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.allowMending)
                .create(left, y, fieldWidth, 20,
                        Component.translatable("screen.keep_my_sword.allow_mending"),
                        (btn, value) -> this.allowMending = value));
        y += 26;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.showNotification)
                .create(left, y, fieldWidth, 20,
                        Component.translatable("screen.keep_my_sword.show_notification"),
                        (btn, value) -> this.showNotification = value));
        y += 26;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.showOverlay)
                .create(left, y, fieldWidth, 20,
                        Component.translatable("screen.keep_my_sword.show_overlay"),
                        (btn, value) -> this.showOverlay = value));
        y += 38;

        this.excludedBox = new EditBox(
                this.font,
                left,
                y,
                fieldWidth,
                20,
                Component.translatable("screen.keep_my_sword.excluded_items"));
        this.excludedBox.setMaxLength(1024);
        this.excludedBox.setValue(this.excludedText);
        this.excludedBox.setHint(Component.translatable("screen.keep_my_sword.excluded_hint"));
        this.excludedBox.setResponder(text -> this.excludedText = text);
        this.addRenderableWidget(this.excludedBox);

        int btnY = Math.min(this.height - 32, y + 44);
        int btnW = (fieldWidth - 12) / 3;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.keep_my_sword.reset"),
                        btn -> resetDefaults())
                .bounds(left, btnY, btnW, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.keep_my_sword.cancel"),
                        btn -> onClose())
                .bounds(left + btnW + 6, btnY, btnW, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.keep_my_sword.save"),
                        btn -> saveAndClose())
                .bounds(left + (btnW + 6) * 2, btnY, fieldWidth - (btnW + 6) * 2, 20)
                .build());
    }

    private void resetDefaults() {
        KeepMySwordConfig defaults = KeepMySwordConfig.defaults();
        this.allowMending = defaults.allowMendingWhenBroken();
        this.showNotification = defaults.showBreakNotification();
        this.showOverlay = defaults.showBrokenOverlay();
        this.excludedText = ExclusionMatcher.formatCommaSeparated(defaults.excludedItems());
        this.rebuildWidgets();
    }

    private void saveAndClose() {
        String raw = this.excludedBox != null ? this.excludedBox.getValue() : this.excludedText;
        KeepMySwordConfig updated = new KeepMySwordConfig(
                this.allowMending,
                this.showNotification,
                this.showOverlay,
                ExclusionMatcher.parseCommaSeparated(raw));
        KeepMySwordConfig.updateAndSave(updated);
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        if (this.excludedBox != null) {
            graphics.drawString(
                    this.font,
                    Component.translatable("screen.keep_my_sword.excluded_items"),
                    this.excludedBox.getX(),
                    this.excludedBox.getY() - 12,
                    0xD0D0D0);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
