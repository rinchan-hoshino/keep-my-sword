package dev.rinchan.keepmysword.forge;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.keepmysword.client.KeepMySwordConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(KeepMySword.MOD_ID)
public final class KeepMySwordForge {
    public KeepMySwordForge() {
        KeepMySword.init(FMLPaths.CONFIGDIR.get());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> KeepMySwordForge::registerConfigScreen);
    }

    private static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new KeepMySwordConfigScreen(parent)));
    }
}
