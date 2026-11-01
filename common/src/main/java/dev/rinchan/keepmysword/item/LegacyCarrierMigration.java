package dev.rinchan.keepmysword.item;

import com.mojang.serialization.DataResult;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.util.Base64;

/** Read-only adapter for the schema-1 compressed snapshot envelope. */
public final class LegacyCarrierMigration {
    public static DataResult<ItemStack> decode(
            CompoundTag payload, HolderLookup.Provider registries) {
        if (registries == null) return DataResult.error(() -> "No item registry context");
        if (!VersionSupport.has(payload, "data_version", 99)
                || VersionSupport.integer(payload, "schema") != 1)
            return DataResult.error(() -> "Unrecognized item snapshot");
        int version = VersionSupport.integer(payload, "data_version");
        if (version < 0 || version > VersionSupport.dataVersion())
            return DataResult.error(() -> "Item snapshot is from an unsupported version");
        try {
            var tag = readLegacy(payload);
            tag.putInt("DataVersion", version);
            return CarrierSnapshot.decode(tag, registries);
        } catch (IOException | RuntimeException failure) {
            return DataResult.error(() -> "Cannot recover item: " + failure.getMessage());
        }
    }

    private static CompoundTag readLegacy(CompoundTag payload) throws IOException {
        if (!(payload.get("chunks") instanceof ListTag chunks) || chunks.isEmpty())
            throw new IOException("Missing item snapshot");
        var encoded = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            if (!(chunks.get(i) instanceof StringTag))
                throw new IOException("Invalid snapshot chunk");
            encoded.append(VersionSupport.chunk(chunks, i));
        }
        return NbtIo.readCompressed(
                new ByteArrayInputStream(Base64.getDecoder().decode(encoded.toString())),
                NbtAccounter.unlimitedHeap());
    }

    private LegacyCarrierMigration() {}
}
