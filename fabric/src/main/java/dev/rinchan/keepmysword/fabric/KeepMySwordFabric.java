package dev.rinchan.keepmysword.fabric;

import dev.rinchan.keepmysword.KeepMySword;
import java.io.UncheckedIOException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class KeepMySwordFabric implements ModInitializer {
    @Override public void onInitialize() {
        try {
            KeepMySword.initialize(FabricLoader.getInstance().getConfigDir());
        } catch (java.io.IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
