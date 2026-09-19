package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneRepairMixin {
    @WrapMethod(method = "computeResult")
    private ItemStack kms$repair(ItemStack first, ItemStack second, Operation<ItemStack> original) {
        if (!SealedItems.isSealed(first) && !SealedItems.isSealed(second))
            return original.call(first, second);
        var a = SealedItems.originalForRepair(first);
        var b = SealedItems.originalForRepair(second);
        if ((!first.isEmpty() && a.isEmpty()) || (!second.isEmpty() && b.isEmpty()))
            return ItemStack.EMPTY;
        return SealedItems.seal(SnapshotScope.call(() -> original.call(a, b)));
    }
}
