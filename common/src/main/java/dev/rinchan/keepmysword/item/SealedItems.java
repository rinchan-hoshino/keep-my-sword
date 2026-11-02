package dev.rinchan.keepmysword.item;

import com.mojang.serialization.DataResult;

import dev.rinchan.keepmysword.KeepMySword;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;

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
                || !dev.rinchan.keepmysword.item.DamageState.isBroken(source)) return source;
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

    public static ItemStack originalForRepair(ItemStack stack, Level level) {
        if (!isSealed(stack)) return stack;
        return ((SnapshotCache) (Object) stack).kms$original(level);
    }

    public static ItemStack visualCopy(ItemStack stack, Level level) {
        var original = originalForRepair(stack, level);
        return original.isEmpty() ? stack : original;
    }

    /** Shared cache miss path for repair, rendering and tooltips. */
    public static DataResult<ItemStack> decodeSnapshot(ItemStack stack, Level level) {
        var data = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        if (!(data.get(CarrierSnapshot.KEY) instanceof CompoundTag payload))
            return DataResult.error(() -> "Invalid item snapshot");
        var registries = level.registryAccess();
        if (!payload.contains("DataVersion") && payload.contains("chunks")) {
            return LegacyCarrierMigration.decode(payload, registries)
                    .flatMap(
                            original -> {
                                if (!(level instanceof ServerLevel))
                                    return DataResult.success(original);
                                return CarrierSnapshot.encode(original, registries)
                                        .map(
                                                upgraded -> {
                                                    data.put(CarrierSnapshot.KEY, upgraded);
                                                    stack.set(
                                                            DataComponents.CUSTOM_DATA,
                                                            CustomData.of(data));
                                                    return original;
                                                });
                            });
        }
        return CarrierSnapshot.decode(payload, registries);
    }

    public static boolean sealInPlace(ItemStack stack, HolderLookup.Provider registries) {
        var replacement = seal(stack, registries);
        if (replacement == stack) return false;
        ((ReplaceableStack) (Object) stack).kms$replaceWith(replacement);
        return true;
    }

    private SealedItems() {}
}
