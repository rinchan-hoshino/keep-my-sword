package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.component.*;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Ordinary metadata edits cannot delete or overwrite the only original snapshot. */
@Mixin(ItemStack.class)
public abstract class CarrierDataProtectionMixin {
    @Unique
    private Tag kms$payload() {
        var stack = (ItemStack) (Object) this;
        return !SnapshotScope.active() && SealedItems.isSealed(stack)
                ? stack.get(DataComponents.CUSTOM_DATA).copyTag().get(CarrierSnapshot.KEY)
                : null;
    }

    @Unique
    private CustomData kms$preserve(CustomData data, Tag payload) {
        var tag = (data == null ? CustomData.EMPTY : data).copyTag();
        tag.put(CarrierSnapshot.KEY, payload.copy());
        return CustomData.of(tag);
    }

    @WrapMethod(method = "set")
    @SuppressWarnings("unchecked")
    private <T> T kms$set(DataComponentType<? super T> type, T value, Operation<T> original) {
        var payload = type == DataComponents.CUSTOM_DATA ? kms$payload() : null;
        return original.call(
                type, payload == null ? value : (T) kms$preserve((CustomData) value, payload));
    }

    @WrapMethod(method = "remove")
    @SuppressWarnings("unchecked")
    private <T> T kms$remove(DataComponentType<? extends T> type, Operation<T> original) {
        if (type == DataComponents.CUSTOM_DATA && kms$payload() != null)
            return (T) ((ItemStack) (Object) this).get(DataComponents.CUSTOM_DATA);
        return original.call(type);
    }

    @WrapMethod(
            method = {
                "applyComponents(Lnet/minecraft/core/component/DataComponentPatch;)V",
                "applyComponentsAndValidate(Lnet/minecraft/core/component/DataComponentPatch;)V"
            })
    private void kms$patch(DataComponentPatch patch, Operation<Void> original) {
        var payload = kms$payload();
        try {
            original.call(patch);
        } finally {
            kms$restore(payload);
        }
    }

    @WrapMethod(method = "applyComponents(Lnet/minecraft/core/component/DataComponentMap;)V")
    private void kms$map(DataComponentMap map, Operation<Void> original) {
        var payload = kms$payload();
        try {
            original.call(map);
        } finally {
            kms$restore(payload);
        }
    }

    @Unique
    private void kms$restore(Tag payload) {
        if (payload == null) return;
        var stack = (ItemStack) (Object) this;
        SnapshotScope.call(
                () ->
                        stack.set(
                                DataComponents.CUSTOM_DATA,
                                kms$preserve(stack.get(DataComponents.CUSTOM_DATA), payload)));
    }
}
