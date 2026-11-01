package dev.rinchan.keepmysword.item;

import dev.rinchan.keepmysword.KeepMySword;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;

import java.util.List;

public final class SealedItems {
    public static boolean isSealed(ItemStack stack) {
        if (!stack.is(Items.RECOVERY_COMPASS)) return false;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        // Unknown/malformed payloads remain protected and are never silently consumed.
        return data != null && data.copyTag().contains(CarrierSnapshot.KEY);
    }

    public static ItemStack seal(ItemStack source, HolderLookup.Provider registries) {
        if (source.isEmpty()
                || isSealed(source)
                || KeepMySword.isExcluded(source)
                || !dev.rinchan.rinlib.item.DamageState.isBroken(source)) return source;
        var payload = CarrierSnapshot.encode(source, registries);
        if (payload.result().isEmpty()) return source;
        var carrier = new ItemStack(Items.RECOVERY_COMPASS, source.getCount());
        var data = new CompoundTag();
        data.put(CarrierSnapshot.KEY, payload.result().orElseThrow());
        carrier.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        carrier.set(DataComponents.MAX_STACK_SIZE, 1);
        carrier.set(DataComponents.CUSTOM_NAME, source.getHoverName());
        carrier.set(
                DataComponents.LORE,
                new ItemLore(
                        List.of(
                                Component.translatableWithFallback(
                                        "item.keep_my_sword.recovery",
                                        "Broken item — install Keep My Sword to repair it."))));
        return carrier;
    }

    /** Only explicit rendering/repair integrations may obtain a private original copy. */
    public static ItemStack originalForRepair(ItemStack stack) {
        if (!isSealed(stack)) return stack;
        return originalForRepair(stack, null);
    }

    public static ItemStack originalForRepair(ItemStack stack, HolderLookup.Provider registries) {
        if (!isSealed(stack)) return stack;
        return ((SnapshotCache) (Object) stack).kms$original(registries);
    }

    public static ItemStack visualCopy(ItemStack stack, HolderLookup.Provider registries) {
        var original = originalForRepair(stack, registries);
        return original.isEmpty() ? stack : original;
    }

    public static boolean sealInPlace(ItemStack stack, HolderLookup.Provider registries) {
        var replacement = seal(stack, registries);
        if (replacement == stack) return false;
        ((ReplaceableStack) (Object) stack).kms$replaceWith(replacement);
        return true;
    }

    private SealedItems() {}
}
