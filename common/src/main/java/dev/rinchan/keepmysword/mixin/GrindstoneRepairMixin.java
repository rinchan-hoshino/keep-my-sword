package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneRepairMixin {
    @Unique private net.minecraft.world.level.Level kms$level;

    @Inject(
            method =
                    "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("RETURN"))
    private void kms$level(
            int id,
            net.minecraft.world.entity.player.Inventory inventory,
            net.minecraft.world.inventory.ContainerLevelAccess access,
            CallbackInfo ci) {
        kms$level = inventory.player.level();
        var menu = (GrindstoneMenu) (Object) this;
        for (int i = 0; i < 2; i++) ((RepairSlot) menu.getSlot(i)).kms$setLevel(kms$level);
    }

    @WrapMethod(method = "computeResult")
    private ItemStack kms$repair(ItemStack first, ItemStack second, Operation<ItemStack> original) {
        if (!SealedItems.isSealed(first) && !SealedItems.isSealed(second))
            return original.call(first, second);
        var a = SealedItems.originalForRepair(first, kms$level);
        var b = SealedItems.originalForRepair(second, kms$level);
        if ((!first.isEmpty() && a.isEmpty()) || (!second.isEmpty() && b.isEmpty()))
            return ItemStack.EMPTY;
        return SealedItems.seal(original.call(a, b), kms$level.registryAccess());
    }
}
