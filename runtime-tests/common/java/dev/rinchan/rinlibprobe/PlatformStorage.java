package dev.rinchan.rinlibprobe;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.nio.file.*;

public final class PlatformStorage {
    public static void save(MinecraftServer s, ItemStack stack) throws Exception {
        var ops =
                s.registryAccess()
                        .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var encoded = ItemVariant.CODEC.encodeStart(ops, ItemVariant.of(stack)).getOrThrow();
        Files.writeString(Path.of("platform.json"), encoded.toString());
        Fixtures.check(
                Fixtures.encode(s, ItemVariant.CODEC.parse(ops, encoded).getOrThrow().toStack())
                        .equals(Fixtures.encode(s, stack)),
                "platform storage preserves native carrier without adapters");
    }

    public static void verify(MinecraftServer s) throws Exception {
        var ops =
                s.registryAccess()
                        .createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var stack =
                ItemVariant.CODEC
                        .parse(
                                ops,
                                com.google.gson.JsonParser.parseString(
                                        Files.readString(Path.of("platform.json"))))
                        .getOrThrow()
                        .toStack();
        Fixtures.check(
                com.google.gson.JsonParser.parseString(Fixtures.encode(s, stack))
                        .equals(
                                com.google.gson.JsonParser.parseString(
                                        Fixtures.encode(s, Fixtures.chest(s).getItem(0)))),
                "platform storage readback preserves complete carrier");
    }
}
