package dev.rinchan.keepmysword.forge;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.keepmysword.forge.client.KeepMySwordForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(KeepMySword.MOD_ID)
public final class KeepMySwordForge {
    public KeepMySwordForge() {
        KeepMySword.init(FMLPaths.CONFIGDIR.get());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> KeepMySwordForgeClient::registerConfigScreen);
    }
}
