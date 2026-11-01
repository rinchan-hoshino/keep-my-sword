package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(
        targets = {
            "net.minecraft.world.inventory.GrindstoneMenu$2",
            "net.minecraft.world.inventory.GrindstoneMenu$3"
        })
public abstract class GrindstoneInputMixin implements RepairSlot {
    @Unique private HolderLookup.Provider kms$registries;

    public void kms$setRegistries(HolderLookup.Provider registries) {
        kms$registries = registries;
    }

    @WrapMethod(method = "mayPlace")
    private boolean kms$acceptBroken(ItemStack stack, Operation<Boolean> original) {
        return original.call(SealedItems.originalForRepair(stack, kms$registries));
    }
}
