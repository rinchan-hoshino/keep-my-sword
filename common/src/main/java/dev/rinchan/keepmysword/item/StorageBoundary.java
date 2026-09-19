package dev.rinchan.keepmysword.item;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;

import net.minecraft.world.item.ItemStack;

/** Native disk serialization sees original items and a versioned record of shell-only metadata. */
public final class StorageBoundary {
    private static Codec<ItemStack> rawCodec;

    public static Codec<ItemStack> full(Codec<ItemStack> raw) {
        rawCodec =
                new Codec<>() {
                    public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
                        return SnapshotScope.call(() -> raw.decode(ops, input));
                    }

                    public <T> DataResult<T> encode(ItemStack value, DynamicOps<T> ops, T prefix) {
                        return raw.encode(value, ops, prefix);
                    }
                };
        return adapt(raw);
    }

    public static Codec<ItemStack> adapt(Codec<ItemStack> raw) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
                return SnapshotScope.call(() -> raw.decode(ops, input))
                        .map(pair -> pair.mapFirst(stack -> ShellPersistence.decode(stack, ops)));
            }

            @Override
            public <T> DataResult<T> encode(ItemStack value, DynamicOps<T> ops, T prefix) {
                return ShellPersistence.encode(value, ops)
                        .flatMap(stack -> raw.encode(stack, ops, prefix));
            }
        };
    }

    public static Codec<ItemStack> raw() {
        if (rawCodec == null) throw new IllegalStateException("ItemStack codec not initialized");
        return rawCodec;
    }

    public static MapCodec<ItemStack> map(MapCodec<ItemStack> raw) {
        full(raw.codec());
        return new MapCodec<>() {
            public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops) {
                return raw.keys(ops);
            }

            public <T> DataResult<ItemStack> decode(DynamicOps<T> ops, MapLike<T> input) {
                return SnapshotScope.call(() -> raw.decode(ops, input))
                        .map(stack -> ShellPersistence.decode(stack, ops));
            }

            public <T> RecordBuilder<T> encode(
                    ItemStack input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                var result = ShellPersistence.encode(input, ops);
                return result.map(stack -> raw.encode(stack, ops, prefix))
                        .result()
                        .orElseGet(() -> prefix.withErrorsFrom(result));
            }
        };
    }

    public static <R> Codec<R> resources(
            Codec<R> raw,
            java.util.function.Function<R, ItemStack> toStack,
            java.util.function.Function<ItemStack, R> fromStack) {
        return new Codec<>() {
            public <T> DataResult<Pair<R, T>> decode(DynamicOps<T> ops, T input) {
                return SnapshotScope.call(() -> raw.decode(ops, input))
                        .map(
                                pair ->
                                        pair.mapFirst(
                                                value -> {
                                                    var stack =
                                                            SnapshotScope.call(
                                                                    () -> toStack.apply(value));
                                                    var sealed =
                                                            ShellPersistence.decode(stack, ops);
                                                    return SnapshotScope.call(
                                                            () -> fromStack.apply(sealed));
                                                }));
            }

            public <T> DataResult<T> encode(R value, DynamicOps<T> ops, T prefix) {
                var stack = SnapshotScope.call(() -> toStack.apply(value));
                return ShellPersistence.encode(stack, ops)
                        .flatMap(
                                original ->
                                        SnapshotScope.call(
                                                () ->
                                                        raw.encode(
                                                                fromStack.apply(original),
                                                                ops,
                                                                prefix)));
            }
        };
    }

    private StorageBoundary() {}
}
