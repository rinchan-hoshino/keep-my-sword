package dev.rinchan.keepmysword.item;

import net.minecraft.world.item.ItemStack;

/** Immutable ownership boundary: the stored stack is never exposed directly. */
public final class OriginalPayload {
    private final ItemStack original;

    public OriginalPayload(ItemStack original) {
        this.original = SnapshotScope.call(() -> original.copyWithCount(1));
    }

    public ItemStack copy() {
        return SnapshotScope.call(original::copy);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OriginalPayload p
                && ItemStack.isSameItemSameComponents(original, p.original);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(original);
    }
}
