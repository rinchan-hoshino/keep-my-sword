package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.config.KeepMySwordConfig;
import dev.rinchan.keepmysword.state.BrokenState;
import dev.rinchan.keepmysword.state.BrokenVisualStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Renders the crisp 5x5 broken-item corner badge on GUI slots. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL"))
    private void kms$drawBrokenBadge(
            Font font, ItemStack stack, int x, int y, String countText, CallbackInfo ci) {
        if (!KeepMySwordConfig.get().showBrokenOverlay() || !BrokenState.isBroken(stack)) return;

        GuiGraphics self = (GuiGraphics) (Object) this;
        self.pose().pushPose();
        self.pose().translate(0.0F, 0.0F, 200.0F);
        int baseX = x + BrokenVisualStyle.OFFSET_X;
        int baseY = y + BrokenVisualStyle.OFFSET_Y;
        int[][] pixels = BrokenVisualStyle.BADGE_PIXELS;
        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[row].length; col++) {
                int color = BrokenVisualStyle.colorFor(pixels[row][col]);
                if (color != 0) {
                    self.fill(baseX + col, baseY + row, baseX + col + 1, baseY + row + 1, color);
                }
            }
        }
        self.pose().popPose();
    }
}
