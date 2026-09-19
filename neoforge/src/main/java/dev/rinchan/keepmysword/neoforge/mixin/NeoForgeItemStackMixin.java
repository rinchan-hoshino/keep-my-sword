package dev.rinchan.keepmysword.neoforge.mixin;

import dev.rinchan.keepmysword.KeepMySword;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class NeoForgeItemStackMixin {
    @Inject(method = "onItemUseFirst", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseFirst(
            UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
