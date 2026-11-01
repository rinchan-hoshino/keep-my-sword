package dev.rinchan.keepmysword.item;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.util.Base64;

/** Native custom data only: no registered item/component is required to keep a snapshot. */
public final class CarrierSnapshot {
    public static final String KEY = "keep_my_sword:sealed";

    public static DataResult<CompoundTag> encode(
            ItemStack original, HolderLookup.Provider registries) {
        try {
            return ItemStack.CODEC
                    .encodeStart(
                            registries.createSerializationContext(NbtOps.INSTANCE),
                            original.copyWithCount(1))
                    .flatMap(
                            tag -> {
                                if (!(tag instanceof CompoundTag compound))
                                    return DataResult.error(() -> "Invalid item snapshot");
                                compound.putInt("DataVersion", VersionSupport.dataVersion());
                                return DataResult.success(compound);
                            });
        } catch (RuntimeException failure) {
            return DataResult.error(() -> "Cannot preserve item: " + failure.getMessage());
        }
    }

    public static DataResult<ItemStack> decode(
            CompoundTag payload, HolderLookup.Provider registries) {
        if (registries == null) return DataResult.error(() -> "No item registry context");
        boolean legacy = payload.contains("chunks");
        String versionKey = legacy ? "data_version" : "DataVersion";
        if (!VersionSupport.has(payload, versionKey, 99)
                || (legacy && VersionSupport.integer(payload, "schema") != 1))
            return DataResult.error(() -> "Unrecognized item snapshot");
        int version = VersionSupport.integer(payload, versionKey);
        if (version < 0 || version > VersionSupport.dataVersion())
            return DataResult.error(() -> "Item snapshot is from an unsupported version");
        try {
            var tag = legacy ? readLegacy(payload) : payload;
            var upgraded =
                    DataFixers.getDataFixer()
                            .update(
                                    References.ITEM_STACK,
                                    new Dynamic<>(NbtOps.INSTANCE, tag),
                                    version,
                                    VersionSupport.dataVersion());
            return ItemStack.CODEC
                    .parse(
                            registries.createSerializationContext(NbtOps.INSTANCE),
                            upgraded.getValue())
                    .flatMap(
                            stack ->
                                    stack.isEmpty() || SealedItems.isSealed(stack)
                                            ? DataResult.error(() -> "Invalid original item")
                                            : DataResult.success(stack));
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

    private CarrierSnapshot() {}
}
