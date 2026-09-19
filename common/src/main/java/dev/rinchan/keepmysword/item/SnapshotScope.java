package dev.rinchan.keepmysword.item;

import java.util.function.Supplier;

/** Suppresses live transitions only while decoding data or calculating a repair snapshot. */
public final class SnapshotScope {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    public static boolean active() {
        return DEPTH.get() != 0;
    }

    public static <T> T call(Supplier<T> action) {
        int depth = DEPTH.get();
        DEPTH.set(depth + 1);
        try {
            return action.get();
        } finally {
            if (depth == 0) DEPTH.remove();
            else DEPTH.set(depth);
        }
    }

    private static final ThreadLocal<net.minecraft.core.HolderLookup.Provider> REGISTRIES =
            new ThreadLocal<>();

    public static net.minecraft.core.HolderLookup.Provider registries() {
        return REGISTRIES.get();
    }

    public static <T> T damage(
            net.minecraft.core.HolderLookup.Provider registries, Supplier<T> action) {
        var previous = REGISTRIES.get();
        REGISTRIES.set(registries);
        try {
            return call(action);
        } finally {
            if (previous == null) REGISTRIES.remove();
            else REGISTRIES.set(previous);
        }
    }

    private SnapshotScope() {}
}
