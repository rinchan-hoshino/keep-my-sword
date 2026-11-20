package dev.rinchan.keepmysword.forge.client;

import dev.rinchan.keepmysword.client.KeepMySwordConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public final class KeepMySwordForgeClient {
    private KeepMySwordForgeClient() {}

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new KeepMySwordConfigScreen(parent)));
    }
}
