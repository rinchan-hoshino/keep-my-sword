package dev.rinchan.keepmysword.fabric;

import dev.rinchan.keepmysword.item.*;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class KeepMySwordFabric implements ModInitializer {
    public void onInitialize() {
        SealedRegistry.INERT =
                Registry.register(
                        BuiltInRegistries.ITEM,
                        VersionSupport.id("broken_item"),
                        VersionSupport.inertItem());
        SealedRegistry.ORIGINAL =
                Registry.register(
                        BuiltInRegistries.DATA_COMPONENT_TYPE,
                        VersionSupport.id("sealed_original"),
                        SealedRegistry.originalType());
    }
}
