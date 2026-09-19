package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(
        targets = {
            "net.minecraft.world.inventory.GrindstoneMenu$2",
            "net.minecraft.world.inventory.GrindstoneMenu$3"
        })
public abstract class GrindstoneInputMixin {
    @WrapMethod(method = "mayPlace")
    private boolean kms$acceptBroken(ItemStack stack, Operation<Boolean> original) {
        return original.call(SealedItems.originalForRepair(stack));
    }
}
