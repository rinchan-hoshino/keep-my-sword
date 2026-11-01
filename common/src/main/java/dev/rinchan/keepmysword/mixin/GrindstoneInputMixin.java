package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(
        targets = {
            "net.minecraft.world.inventory.GrindstoneMenu$2",
            "net.minecraft.world.inventory.GrindstoneMenu$3"
        })
public abstract class GrindstoneInputMixin implements RepairSlot {
    @Unique private Level kms$level;

    public void kms$setLevel(Level level) {
        kms$level = level;
    }

    @WrapMethod(method = "mayPlace")
    private boolean kms$acceptBroken(ItemStack stack, Operation<Boolean> original) {
        return original.call(SealedItems.originalForRepair(stack, kms$level));
    }
}
