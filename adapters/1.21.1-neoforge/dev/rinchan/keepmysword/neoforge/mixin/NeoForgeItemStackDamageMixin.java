package dev.rinchan.keepmysword.neoforge.mixin;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class NeoForgeItemStackDamageMixin {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(
            method =
                    "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V")
    private void kms$damageTransaction(
            int amount,
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.LivingEntity player,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBreak,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        dev.rinchan.keepmysword.item.SnapshotScope.call(
                () -> {
                    original.call(amount, level, player, onBreak);
                    return null;
                });
    }

    @Inject(
            method =
                    "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
            cancellable = true)
    private void keepMySword$preserveBeforeDestruction(
            int amount,
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.LivingEntity player,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBreak,
            CallbackInfo ci) {
        SealedItems.sealInPlace((ItemStack) (Object) this, level.registryAccess());
        if (dev.rinchan.keepmysword.KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }
}
