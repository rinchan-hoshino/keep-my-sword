package dev.rinchan.keepmysword.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.keepmysword.item.*;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

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

    @Unique private ServerLevel kms$breakLevel;

    // 26.x delegates to a level-less applyDamage; retain the level only for that native call.
    @WrapOperation(
            method =
                    "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;applyDamage(ILnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V"))
    private void kms$withBreakLevel(
            ItemStack stack,
            int damage,
            net.minecraft.server.level.ServerPlayer owner,
            Consumer<Item> onBreak,
            Operation<Void> original,
            @Local(argsOnly = true) ServerLevel level) {
        var previous = kms$breakLevel;
        kms$breakLevel = level;
        try {
            original.call(stack, damage, owner, onBreak);
        } finally {
            kms$breakLevel = previous;
        }
    }

    @Inject(
            method =
                    "applyDamage(ILnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
            cancellable = true)
    private void kms$preserve(
            int damage,
            net.minecraft.server.level.ServerPlayer owner,
            Consumer<Item> onBreak,
            CallbackInfo ci) {
        if (kms$breakLevel == null) return;
        var stack = (ItemStack) (Object) this;
        if (SealedItems.sealInPlace(stack, kms$breakLevel.registryAccess()) && owner != null)
            ((EquipmentRefresh) owner).kms$refreshEquipment();
        if (KeepMySword.isManagedBroken(stack)) ci.cancel();
    }
}
