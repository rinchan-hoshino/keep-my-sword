package dev.rinchan.keepmysword.item;

import net.minecraft.world.item.ItemStack;

public interface ReplaceableStack {
    void kms$replaceWith(ItemStack replacement);
}
