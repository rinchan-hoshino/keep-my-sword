package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public abstract class EquippedBreakMixin {
    @WrapMethod(
            method =
                    "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V")
    private void kms$refresh(
            int amount, LivingEntity owner, EquipmentSlot slot, Operation<Void> original) {
        original.call(amount, owner, slot);
        if (owner.level() instanceof net.minecraft.server.level.ServerLevel
                && SealedItems.isSealed((ItemStack) (Object) this))
            ((EquipmentRefresh) owner).kms$refreshEquipment();
    }
}
