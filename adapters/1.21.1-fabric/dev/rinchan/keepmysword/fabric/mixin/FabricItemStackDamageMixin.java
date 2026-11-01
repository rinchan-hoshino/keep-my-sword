package dev.rinchan.keepmysword.fabric.mixin;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class FabricItemStackDamageMixin {
    // Vanilla's level overload receives only ServerPlayer, so mobs need their caller to refresh.
    @Inject(
            method =
                    "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("TAIL"))
    private void kms$refreshMob(
            int amount,
            net.minecraft.world.entity.LivingEntity owner,
            net.minecraft.world.entity.EquipmentSlot slot,
            CallbackInfo ci) {
        if (!(owner instanceof net.minecraft.server.level.ServerPlayer)
                && owner.level() instanceof net.minecraft.server.level.ServerLevel
                && SealedItems.isSealed((ItemStack) (Object) this))
            ((dev.rinchan.keepmysword.item.EquipmentRefresh) owner).kms$refreshEquipment();
    }

    @Inject(
            method =
                    "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
            cancellable = true)
    private void keepMySword$preserveBeforeDestruction(
            int amount,
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.server.level.ServerPlayer player,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBreak,
            CallbackInfo ci) {
        if (SealedItems.sealInPlace((ItemStack) (Object) this, level.registryAccess())
                && player != null)
            ((dev.rinchan.keepmysword.item.EquipmentRefresh) player).kms$refreshEquipment();
        if (dev.rinchan.keepmysword.KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }
}
