package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Compute on snapshots; vanilla keeps ownership of inputs, XP and the output transaction. */
@Mixin(AnvilMenu.class)
public abstract class AnvilSnapshotMixin extends ItemCombinerMenu {
    protected AnvilSnapshotMixin(
            MenuType<?> type,
            int id,
            Inventory inventory,
            ContainerLevelAccess access,
            ItemCombinerMenuSlotDefinition definition) {
        super(type, id, inventory, access, definition);
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "createResult")
    private void kms$repairScope(Operation<Void> original) {
        for (int i = 0; i < inputSlots.getContainerSize(); i++) {
            var input = inputSlots.getItem(i);
            if (SealedItems.isSealed(input) && SealedItems.originalForRepair(input).isEmpty()) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                return;
            }
        }
        SnapshotScope.call(
                () -> {
                    original.call();
                    return null;
                });
    }

    @WrapOperation(
            method = "createResult",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/Container;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack kms$snapshot(Container container, int slot, Operation<ItemStack> original) {
        var stack = original.call(container, slot);
        return SealedItems.originalForRepair(stack);
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void kms$keepBrokenOutputSealed(CallbackInfo ci) {
        var result = resultSlots.getItem(0);
        var sealed = SealedItems.seal(result);
        if (sealed != result) resultSlots.setItem(0, sealed);
    }
}
