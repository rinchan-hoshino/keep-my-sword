package dev.rinchan.keepmysword.neoforge;

import dev.rinchan.keepmysword.KeepMySword;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(KeepMySword.MOD_ID)
public final class KeepMySwordNeoForge {
    public KeepMySwordNeoForge() {
        KeepMySword.init(FMLPaths.CONFIGDIR.get());
    }
}
