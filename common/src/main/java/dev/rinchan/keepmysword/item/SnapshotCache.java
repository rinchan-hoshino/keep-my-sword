package dev.rinchan.keepmysword.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Runtime-only cache; the original is never exposed without copying. */
public interface SnapshotCache {
    ItemStack kms$original(Level level);
}
