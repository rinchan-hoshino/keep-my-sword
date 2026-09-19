package dev.rinchan.keepmysword.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

/** Runtime-only cache; the original is never exposed without copying. */
public interface SnapshotCache {
    ItemStack kms$original(HolderLookup.Provider registries);
}
