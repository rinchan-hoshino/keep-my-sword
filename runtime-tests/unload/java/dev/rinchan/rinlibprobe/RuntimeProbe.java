package dev.rinchan.rinlibprobe;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;

import java.nio.file.*;
import java.util.*;

public final class RuntimeProbe {
    public void run(MinecraftServer s) {
        s.execute(
                () -> {
                    try {
                        Fixtures.check(
                                !FabricLoader.getInstance().isModLoaded("keep_my_sword"),
                                "mod absent");
                        try {
                            Class.forName("dev.rinchan.keepmysword.item.SealedItems");
                            throw new AssertionError("mod classes present");
                        } catch (ClassNotFoundException expected) {
                        }
                        Fixtures.check(true, "mod classes absent");
                        Fixtures.verify(s);
                        var ops = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                        var jsonOps =
                                s.registryAccess()
                                        .createSerializationContext(
                                                com.mojang.serialization.JsonOps.INSTANCE);
                        var disk =
                                IndependentStorage.CODEC
                                        .parse(
                                                ops,
                                                TagParser.parseTag(
                                                        Files.readString(
                                                                Path.of("independent.snbt"))))
                                        .getOrThrow();
                        var json =
                                IndependentStorage.CODEC
                                        .parse(
                                                jsonOps,
                                                com.google.gson.JsonParser.parseString(
                                                        Files.readString(
                                                                Path.of("independent.json"))))
                                        .getOrThrow();
                        for (var stack :
                                List.of(
                                        Fixtures.chest(s).getItem(0),
                                        disk,
                                        json,
                                        Fixtures.chest(s).getItem(4))) {
                            Fixtures.check(
                                    stack.is(Items.RECOVERY_COMPASS),
                                    "native carrier survives uninstall");
                            var data = stack.get(DataComponents.CUSTOM_DATA).copyTag();
                            var envelope = data.getCompound("keep_my_sword:sealed");
                            var chunks = (ListTag) envelope.get("chunks");
                            var encoded = new StringBuilder();
                            for (int i = 0; i < chunks.size(); i++)
                                encoded.append(chunks.getString(i));
                            var bytes = java.util.Base64.getDecoder().decode(encoded.toString());
                            var original =
                                    NbtIo.readCompressed(
                                            new java.io.ByteArrayInputStream(bytes),
                                            NbtAccounter.create(16000000));
                            var expected =
                                    TagParser.parseTag(
                                            Files.readString(
                                                    Path.of(
                                                            stack == Fixtures.chest(s).getItem(4)
                                                                    ? "large-original.snbt"
                                                                    : "original.snbt")));
                            Fixtures.check(
                                    original.equals(expected),
                                    "independent recovery without mod retains exact original NBT");
                            var decoded = ItemStack.CODEC.parse(ops, original).getOrThrow();
                            Fixtures.check(
                                    decoded.is(Items.DIAMOND_SWORD) && decoded.isEnchanted(),
                                    "original decodes without Keep My Sword classes");
                        }
                        PlatformStorage.verify(s);
                        s.saveEverything(false, true, true);
                        Files.writeString(Path.of("unloaded.pass"), "pass\n");
                        System.out.println("SAFETY_UNLOAD_PASS");
                    } catch (Throwable e) {
                        e.printStackTrace();
                        System.out.println("SAFETY_UNLOAD_FAIL");
                    } finally {
                        s.halt(false);
                    }
                });
    }
}
