package dev.rinchan.keepmysword.neoforge.client;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.keepmysword.client.KeepMySwordConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = KeepMySword.MOD_ID, dist = Dist.CLIENT)
public final class KeepMySwordNeoForgeClient {
    public KeepMySwordNeoForgeClient(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new KeepMySwordConfigScreen(parent));
    }
}
