package dev.rinchan.keepmysword.item;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Registry objects shared by both loader adapters. */
public final class SealedRegistry {
    public static Item INERT;
    public static DataComponentType<OriginalPayload> ORIGINAL;

    public static DataComponentType<OriginalPayload> originalType() {
        return DataComponentType.<OriginalPayload>builder()
                .persistent(
                        Codec.lazyInitialized(
                                () ->
                                        StorageBoundary.raw()
                                                .xmap(OriginalPayload::new, OriginalPayload::copy)))
                .networkSynchronized(
                        new net.minecraft.network.codec.StreamCodec<
                                net.minecraft.network.RegistryFriendlyByteBuf, OriginalPayload>() {
                            public OriginalPayload decode(
                                    net.minecraft.network.RegistryFriendlyByteBuf buffer) {
                                return SnapshotScope.call(
                                        () ->
                                                new OriginalPayload(
                                                        ItemStack.STREAM_CODEC.decode(buffer)));
                            }

                            public void encode(
                                    net.minecraft.network.RegistryFriendlyByteBuf buffer,
                                    OriginalPayload value) {
                                SnapshotScope.call(
                                        () -> {
                                            ItemStack.STREAM_CODEC.encode(buffer, value.copy());
                                            return null;
                                        });
                            }
                        })
                .build();
    }

    private SealedRegistry() {}
}
