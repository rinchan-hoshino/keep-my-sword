package dev.rinchan.rinlibprobe;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class PreviewScreen extends Screen {
    private final ItemStack[] healthy = {
        new ItemStack(Items.DIAMOND_SWORD),
        new ItemStack(Items.SHIELD),
        new ItemStack(Items.DIAMOND_CHESTPLATE)
    };
    private final ItemStack[] broken = new ItemStack[healthy.length];

    public PreviewScreen() {
        super(Component.literal("Keep My Sword client validation"));
        for (int i = 0; i < healthy.length; i++) {
            broken[i] = healthy[i].copy();
            broken[i].setDamageValue(broken[i].getMaxDamage());
            if (!SealedItems.isSealed(broken[i]))
                throw new AssertionError("client item not sealed");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xff20242c);
        graphics.drawString(font, "Keep My Sword - client validation", 20, 20, 0xffffffff);
        graphics.drawString(font, "Healthy", 40, 45, 0xffffffff);
        graphics.drawString(font, "Broken", 130, 45, 0xffff8888);
        for (int i = 0; i < healthy.length; i++) {
            graphics.renderItem(healthy[i], 50, 65 + i * 24);
            graphics.renderItem(broken[i], 140, 65 + i * 24);
            graphics.renderItemDecorations(font, broken[i], 140, 65 + i * 24);
        }
        var placeholder = new ItemStack(Items.RECOVERY_COMPASS);
        graphics.renderItem(placeholder, 220, 65);
        placeholder.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED);
        int y = 150;
        for (var line :
                broken[0].getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED)) {
            graphics.drawString(font, line, 20, y, 0xffffffff);
            y += 11;
        }
    }
}
