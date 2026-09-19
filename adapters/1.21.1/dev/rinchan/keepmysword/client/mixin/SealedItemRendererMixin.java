package dev.rinchan.keepmysword.client.mixin;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Only renderer arguments receive the original appearance; inventory identity stays inert. */
@Mixin(ItemRenderer.class)
public abstract class SealedItemRendererMixin {
    @ModifyVariable(
            method = {"getModel", "render"},
            at = @At("HEAD"),
            argsOnly = true)
    private ItemStack kms$displayOriginal(ItemStack stack) {
        return SealedItems.visualCopy(stack);
    }
}
