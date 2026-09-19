package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.item.ReplaceableStack;

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.*;

@Mixin(ItemStack.class)
public abstract class StackReplacementMixin implements ReplaceableStack {
    @Shadow @Final @Mutable private Item item;
    @Shadow @Final @Mutable private PatchedDataComponentMap components;
    @Shadow private int count;

    @Override
    public void kms$replaceWith(ItemStack replacement) {
        // Construct all replacement data before touching the live stack.
        var newItem = replacement.getItem();
        var newComponents =
                PatchedDataComponentMap.fromPatch(
                        replacement.getPrototype(), replacement.getComponentsPatch());
        int newCount = replacement.getCount();
        item = newItem;
        components = newComponents;
        count = newCount;
    }
}
