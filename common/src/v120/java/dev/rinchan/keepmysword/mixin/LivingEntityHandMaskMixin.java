package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.state.HandMask;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Masks a broken hand during 1.20.1 BlockState.use calls so item-specific block interactions are skipped. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHandMaskMixin {
    @Inject(method = "getItemInHand", at = @At("HEAD"), cancellable = true)
    private void kms$maskBrokenHand(InteractionHand hand, CallbackInfoReturnable<ItemStack> cir) {
        if (HandMask.isMasked(hand)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
