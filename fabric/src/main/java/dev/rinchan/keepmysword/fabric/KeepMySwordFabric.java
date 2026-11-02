package dev.rinchan.keepmysword.fabric;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.rinlib.v2.RinFabric;
import net.fabricmc.api.ModInitializer;

public final class KeepMySwordFabric implements ModInitializer {
    @Override public void onInitialize() {
        new RinFabric("keep_my_sword").start(new KeepMySword());
    }
}
