package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class CraftingRepairMixin {
    // Assembly uses the same input that was matched; keep its decoded copies local to each stack.
    @Inject(
            method =
                    "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"))
    private void kms$decodeInputs(
            CraftingInput input,
            net.minecraft.world.level.Level level,
            CallbackInfoReturnable<Boolean> cir) {
        for (int i = 0; i < input.size(); i++)
            SealedItems.originalForRepair(input.getItem(i), level);
    }

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
