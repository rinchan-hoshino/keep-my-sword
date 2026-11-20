package dev.rinchan.keepmysword.fabric;

import dev.rinchan.keepmysword.KeepMySword;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class KeepMySwordFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        KeepMySword.init(FabricLoader.getInstance().getConfigDir());
    }
}
