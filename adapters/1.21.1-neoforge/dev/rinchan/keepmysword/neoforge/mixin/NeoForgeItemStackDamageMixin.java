package dev.rinchan.keepmysword.neoforge.mixin;

import dev.rinchan.keepmysword.KeepMySword;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class NeoForgeItemStackDamageMixin {
    @Inject(
            method =
                    "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
            cancellable = true)
    private void keepMySword$preserveBeforeDestruction(CallbackInfo ci) {
        if (KeepMySword.preserveAtBreak((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }
}
