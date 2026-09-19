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
    private static final int SCHEMA = 1;
    private static final int CHUNK_SIZE = 16000;
    private static final int MAX_BYTES = 16 * 1024 * 1024;

    public static DataResult<CompoundTag> encode(
            ItemStack original, HolderLookup.Provider registries) {
        if (registries == null) return DataResult.error(() -> "No item registry context");
        try {
            return SnapshotScope.call(
                            () ->
                                    ItemStack.CODEC.encodeStart(
                                            registries.createSerializationContext(NbtOps.INSTANCE),
                                            original.copyWithCount(1)))
                    .flatMap(
                            tag -> {
                                try {
                                    if (!(tag instanceof CompoundTag compound))
                                        return DataResult.error(() -> "Invalid item snapshot");
                                    var bytes = new ByteArrayOutputStream();
                                    NbtIo.writeCompressed(compound, bytes);
                                    if (bytes.size() > MAX_BYTES)
                                        return DataResult.error(() -> "Item snapshot too large");
                                    var verified =
                                            NbtIo.readCompressed(
                                                    new ByteArrayInputStream(bytes.toByteArray()),
                                                    NbtAccounter.create(MAX_BYTES));
                                    if (!verified.equals(compound))
                                        return DataResult.error(
                                                () -> "Native NBT encoding changed item data");
                                    String encoded =
                                            Base64.getEncoder().encodeToString(bytes.toByteArray());
                                    var chunks = new ListTag();
                                    for (int i = 0; i < encoded.length(); i += CHUNK_SIZE)
                                        chunks.add(
                                                StringTag.valueOf(
                                                        encoded.substring(
                                                                i,
                                                                Math.min(
                                                                        i + CHUNK_SIZE,
                                                                        encoded.length()))));
                                    var envelope = new CompoundTag();
                                    envelope.putInt("schema", SCHEMA);
                                    envelope.putInt("data_version", VersionSupport.dataVersion());
                                    envelope.put("chunks", chunks);
                                    return DataResult.success(envelope);
                                } catch (IOException | RuntimeException failure) {
                                    return DataResult.error(
                                            () -> "Cannot preserve item: " + failure.getMessage());
                                }
                            });
        } catch (RuntimeException failure) {
            return DataResult.error(() -> "Cannot preserve item: " + failure.getMessage());
        }
    }

    public static DataResult<ItemStack> decode(
            CompoundTag envelope, HolderLookup.Provider registries) {
        if (registries == null) return DataResult.error(() -> "No item registry context");
        if (VersionSupport.integer(envelope, "schema") != SCHEMA
                || !VersionSupport.has(envelope, "data_version", 99))
            return DataResult.error(() -> "Unrecognized item snapshot");
        int version = VersionSupport.integer(envelope, "data_version");
        if (version < 0 || version > VersionSupport.dataVersion())
            return DataResult.error(() -> "Item snapshot is from an unsupported version");
        try {
            if (!(envelope.get("chunks") instanceof ListTag chunks) || chunks.isEmpty())
                return DataResult.error(() -> "Missing item snapshot");
            var encoded = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                if (!(chunks.get(i) instanceof StringTag))
                    return DataResult.error(() -> "Invalid snapshot chunk");
                String chunk = VersionSupport.chunk(chunks, i);
                if (chunk.length() > CHUNK_SIZE
                        || encoded.length() + chunk.length() > MAX_BYTES * 4 / 3 + 4)
                    return DataResult.error(() -> "Item snapshot too large");
                encoded.append(chunk);
            }
            var bytes = Base64.getDecoder().decode(encoded.toString());
            var tag =
                    NbtIo.readCompressed(
                            new ByteArrayInputStream(bytes), NbtAccounter.create(MAX_BYTES));
            var upgraded =
                    DataFixers.getDataFixer()
                            .update(
                                    References.ITEM_STACK,
                                    new Dynamic<>(NbtOps.INSTANCE, tag),
                                    version,
                                    VersionSupport.dataVersion());
            return SnapshotScope.call(
                            () ->
                                    ItemStack.CODEC.parse(
                                            registries.createSerializationContext(NbtOps.INSTANCE),
                                            upgraded.getValue()))
                    .flatMap(
                            stack ->
                                    stack.isEmpty() || SealedItems.isSealed(stack)
                                            ? DataResult.error(() -> "Invalid original item")
                                            : DataResult.success(stack));
        } catch (IOException | RuntimeException failure) {
            return DataResult.error(() -> "Cannot recover item: " + failure.getMessage());
        }
    }

    private CarrierSnapshot() {}
}
