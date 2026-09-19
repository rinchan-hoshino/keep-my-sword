package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneExperienceMixin {
    @WrapMethod(method = "getExperienceFromItem")
    private int kms$originalEnchantments(ItemStack stack, Operation<Integer> original) {
        return original.call(SealedItems.originalForRepair(stack));
    }
}
