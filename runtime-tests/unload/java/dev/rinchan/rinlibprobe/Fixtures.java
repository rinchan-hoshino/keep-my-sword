package dev.rinchan.rinlibprobe;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.nio.file.*;

public final class Fixtures {
    public static final BlockPos POS = new BlockPos(0, 80, 0);

    public static String encode(MinecraftServer s, ItemStack stack) {
        return ItemStack.CODEC
                .encodeStart(s.registryAccess().createSerializationContext(JsonOps.INSTANCE), stack)
                .getOrThrow()
                .toString();
    }

    public static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println("SAFETY_PASS " + name);
    }

    public static ChestBlockEntity chest(MinecraftServer s) {
        s.overworld().getChunkAt(POS);
        return (ChestBlockEntity) s.overworld().getBlockEntity(POS);
    }

    public static void verify(MinecraftServer s) throws Exception {
        var c = chest(s);
        check(c != null, "saved chest exists");
        for (int i = 0; i < Integer.parseInt(Files.readString(Path.of("fixture-count.txt"))); i++) {
            var expected =
                    com.google.gson.JsonParser.parseString(
                            Files.readString(Path.of("expected-" + i + ".json")));
            var actual = com.google.gson.JsonParser.parseString(encode(s, c.getItem(i)));
            check(expected.equals(actual), "persisted item " + i + " complete component equality");
            var expectedNbt =
                    net.minecraft.nbt.TagParser.parseTag(
                            Files.readString(Path.of("expected-" + i + ".snbt")));
            var actualNbt =
                    ItemStack.CODEC
                            .encodeStart(
                                    s.registryAccess()
                                            .createSerializationContext(
                                                    net.minecraft.nbt.NbtOps.INSTANCE),
                                    c.getItem(i))
                            .getOrThrow();
            check(
                    expectedNbt.equals(actualNbt),
                    "persisted item " + i + " exact NBT type equality");
        }
    }
}
