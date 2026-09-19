package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class DurabilityMutationMixin {
    @Inject(
            method =
                    "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentPatch;)V",
            at = @At("RETURN"))
    private void kms$constructed(CallbackInfo ci) {
        SealedItems.sealAfterMutation((ItemStack) (Object) this);
    }

    @Inject(method = "set", at = @At("RETURN"))
    private <T> void kms$componentChanged(
            DataComponentType<? super T> type, T value, CallbackInfoReturnable<T> cir) {
        if (type == DataComponents.DAMAGE
                || type == DataComponents.MAX_DAMAGE
                || type == DataComponents.UNBREAKABLE)
            SealedItems.sealAfterMutation((ItemStack) (Object) this);
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private <T> void kms$componentRemoved(
            DataComponentType<? extends T> type, CallbackInfoReturnable<T> cir) {
        if (type == DataComponents.UNBREAKABLE)
            SealedItems.sealAfterMutation((ItemStack) (Object) this);
    }

    @Inject(
            method = {"applyComponents", "applyComponentsAndValidate"},
            at = @At("RETURN"))
    private void kms$componentsApplied(CallbackInfo ci) {
        SealedItems.sealAfterMutation((ItemStack) (Object) this);
    }
}
