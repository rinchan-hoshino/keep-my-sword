package dev.rinchan.keepmysword.neoforge;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(KeepMySword.MOD_ID)
public final class KeepMySwordNeoForge {
    public KeepMySwordNeoForge(IEventBus bus) {
        bus.addListener(KeepMySwordNeoForge::register);
    }

    private static void register(RegisterEvent event) {
        event.register(
                Registries.ITEM,
                helper -> {
                    SealedRegistry.INERT = VersionSupport.inertItem();
                    helper.register(VersionSupport.id("broken_item"), SealedRegistry.INERT);
                });
        event.register(
                Registries.DATA_COMPONENT_TYPE,
                helper -> {
                    SealedRegistry.ORIGINAL = SealedRegistry.originalType();
                    helper.register(VersionSupport.id("sealed_original"), SealedRegistry.ORIGINAL);
                });
    }
}
