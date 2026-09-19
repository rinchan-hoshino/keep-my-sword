package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.KeepMySword;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes broken stacks behave like model-only items. */
@Mixin(ItemStack.class)
public abstract class VersionItemStackMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (KeepMySword.isManagedBroken(stack)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseAnimation(CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(ItemUseAnimation.NONE);
        }
    }

    @Inject(
            method =
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void keepMySword$disableSlotGroupModifiers(CallbackInfo ci) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }
}
