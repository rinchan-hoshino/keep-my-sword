package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneRepairMixin {
    @Unique private HolderLookup.Provider kms$registries;

    @Inject(
            method =
                    "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("RETURN"))
    private void kms$context(
            int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        kms$registries = inventory.player.registryAccess();
    }

    @WrapMethod(method = "computeResult")
    private ItemStack kms$repair(ItemStack first, ItemStack second, Operation<ItemStack> original) {
        if (!SealedItems.isSealed(first) && !SealedItems.isSealed(second))
            return original.call(first, second);
        var ops = kms$registries.createSerializationContext(NbtOps.INSTANCE);
        var result =
                SnapshotScope.call(
                        () ->
                                original.call(
                                        ShellPersistence.encode(first, ops).getOrThrow(),
                                        ShellPersistence.encode(second, ops).getOrThrow()));
        return ShellPersistence.decode(result, ops);
    }
}
