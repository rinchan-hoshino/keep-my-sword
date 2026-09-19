package dev.rinchan.rinlibprobe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class ProbeEntrypoint implements ModInitializer {
    public static ProbeEffectItem EFFECT_ITEM;

    public void onInitialize() {
        EFFECT_ITEM =
                net.minecraft.core.Registry.register(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                "kms_probe", "effect_item"),
                        new ProbeEffectItem());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> new RuntimeProbe().run(server));
    }
}
