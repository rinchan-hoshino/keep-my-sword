package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.*;

@Mixin(ItemStack.class)
public abstract class SnapshotCacheMixin implements SnapshotCache {
    @Unique private CustomData kms$data;
    @Unique private Level kms$level;
    @Unique private ItemStack kms$decoded;

    public ItemStack kms$original(Level level) {
        var stack = (ItemStack) (Object) this;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (level == null)
            return kms$data == data && kms$decoded != null
                    ? kms$decoded.copyWithCount(stack.getCount())
                    : ItemStack.EMPTY;
        if (kms$decoded == null || kms$data != data || kms$level != level) {
            kms$level = level;
            kms$decoded = SealedItems.decodeSnapshot(stack, level).result().orElse(ItemStack.EMPTY);
            kms$data = stack.get(DataComponents.CUSTOM_DATA);
        }
        return kms$decoded.copyWithCount(stack.getCount());
    }
}
