package dev.rinchan.keepmysword.item;

import net.minecraft.core.HolderLookup;

import java.util.function.Supplier;

/** Registry ownership follows the running server or the current client frame. */
public final class RegistryContext {
    private static final ThreadLocal<HolderLookup.Provider> CURRENT = new ThreadLocal<>();
    private static volatile HolderLookup.Provider server;

    public static HolderLookup.Provider current() {
        var scoped = CURRENT.get();
        return scoped != null ? scoped : server;
    }

    public static <T> T call(HolderLookup.Provider registries, Supplier<T> action) {
        var previous = CURRENT.get();
        if (registries != null) CURRENT.set(registries);
        try {
            return action.get();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static void runServer(HolderLookup.Provider registries, Runnable action) {
        server = registries;
        try {
            call(
                    registries,
                    () -> {
                        action.run();
                        return null;
                    });
        } finally {
            if (server == registries) server = null;
        }
    }

    private RegistryContext() {}
}
