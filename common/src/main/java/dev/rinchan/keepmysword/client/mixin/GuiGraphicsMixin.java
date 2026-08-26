package dev.rinchan.keepmysword.client.mixin;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.keepmysword.client.BrokenVisualStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void keepMySword$renderBrokenSlotOverlay(Font font, ItemStack stack, int x, int y, @Nullable String text, CallbackInfo ci) {
        if (KeepMySword.isManagedBroken(stack)) {
            ((GuiGraphics) (Object) this).fill(RenderType.guiOverlay(), x, y, x + 16, y + 16, BrokenVisualStyle.SLOT_OVERLAY_ARGB);
        }
    }
}
