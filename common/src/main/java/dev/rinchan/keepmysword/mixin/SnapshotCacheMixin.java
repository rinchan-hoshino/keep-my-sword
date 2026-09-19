package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import org.spongepowered.asm.mixin.*;

@Mixin(ItemStack.class)
public abstract class SnapshotCacheMixin implements SnapshotCache {
    @Unique private CustomData kms$data;
    @Unique private HolderLookup.Provider kms$registries;
    @Unique private ItemStack kms$decoded;

    public ItemStack kms$original(HolderLookup.Provider registries) {
        var stack = (ItemStack) (Object) this;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (kms$decoded == null || kms$data != data || kms$registries != registries) {
            kms$data = data;
            kms$registries = registries;
            var tag = data == null ? null : data.copyTag().get(CarrierSnapshot.KEY);
            kms$decoded =
                    tag instanceof CompoundTag payload
                            ? CarrierSnapshot.decode(payload, registries)
                                    .result()
                                    .orElse(ItemStack.EMPTY)
                            : ItemStack.EMPTY;
        }
        return SnapshotScope.call(() -> kms$decoded.copyWithCount(stack.getCount()));
    }
}
