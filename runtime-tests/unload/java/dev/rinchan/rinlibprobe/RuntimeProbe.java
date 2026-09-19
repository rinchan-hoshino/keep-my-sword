package dev.rinchan.rinlibprobe;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Items;

import java.nio.file.*;

public final class RuntimeProbe {
    public void run(MinecraftServer s) {
        s.execute(
                () -> {
                    try {
                        Fixtures.check(
                                !FabricLoader.getInstance().isModLoaded("keep_my_sword"),
                                "prototype mod absent");
                        try {
                            Class.forName("dev.rinchan.keepmysword.item.SealedItems");
                            throw new AssertionError("prototype classes present");
                        } catch (ClassNotFoundException expected) {
                        }
                        Fixtures.check(true, "prototype classes absent");
                        Fixtures.verify(s);
                        var variantOps =
                                s.registryAccess()
                                        .createSerializationContext(
                                                com.mojang.serialization.JsonOps.INSTANCE);
                        var variant =
                                net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.CODEC
                                        .parse(
                                                variantOps,
                                                com.google.gson.JsonParser.parseString(
                                                        Files.readString(
                                                                Path.of("transfer-variant.json"))))
                                        .getOrThrow();
                        Fixtures.check(
                                variant.getItem() == Items.DIAMOND_SWORD,
                                "unmodified Fabric Transfer decodes original item without"
                                        + " prototype");
                        var expected =
                                com.google.gson.JsonParser.parseString(
                                        Files.readString(Path.of("expected-3.json")));
                        var actual =
                                net.minecraft.world.item.ItemStack.CODEC
                                        .encodeStart(variantOps, variant.toStack())
                                        .getOrThrow();
                        Fixtures.check(
                                expected.equals(actual),
                                "unmodified Fabric Transfer preserves original and shell metadata"
                                        + " without prototype");
                        Fixtures.check(
                                Fixtures.chest(s).getItem(0).getItem() == Items.DIAMOND_SWORD,
                                "original sword type after uninstall");
                        Fixtures.check(
                                Fixtures.chest(s).getItem(1).getItem() == Items.SHIELD,
                                "original shield type after uninstall");
                        var nbtOps =
                                s.registryAccess()
                                        .createSerializationContext(
                                                net.minecraft.nbt.NbtOps.INSTANCE);
                        var nbtVariant =
                                net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(
                                        Fixtures.chest(s).getItem(3));
                        Files.writeString(
                                Path.of("transfer-native.snbt"),
                                net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.CODEC
                                        .encodeStart(nbtOps, nbtVariant)
                                        .getOrThrow()
                                        .toString());
                        Fixtures.chest(s).setChanged();
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
