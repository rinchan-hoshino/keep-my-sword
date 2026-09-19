package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RepairItemRecipe.class)
public abstract class CraftingRepairMixin {
    @WrapOperation(
            method = "getItemsToCombine",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/crafting/CraftingInput;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack kms$repairSnapshot(
            CraftingInput input, int index, Operation<ItemStack> original) {
        return SealedItems.originalForRepair(original.call(input, index));
    }
}
