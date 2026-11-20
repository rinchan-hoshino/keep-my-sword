package dev.rinchan.keepmysword.migration;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** One-way lossless unsealer migrating v1.x recovery-compass sealed carriers to native broken ItemStacks on 26.x. */
public final class LegacyCarrierUnsealer {
    public static final String SEALED_KEY = "keep_my_sword:sealed";

    private LegacyCarrierUnsealer() {}

    public static boolean isLegacySealed(ItemStack stack) {
        if (stack == null || !stack.is(Items.RECOVERY_COMPASS)) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(SEALED_KEY);
    }

    public static Optional<ItemStack> unseal(ItemStack carrier, HolderLookup.Provider registries) {
        if (!isLegacySealed(carrier) || registries == null) return Optional.empty();
        CustomData data = carrier.get(DataComponents.CUSTOM_DATA);
        if (data == null) return Optional.empty();
        CompoundTag root = data.copyTag();
        if (!(root.get(SEALED_KEY) instanceof CompoundTag payload)) return Optional.empty();

        DataResult<ItemStack> decoded = (!payload.contains("DataVersion") && payload.contains("chunks"))
                ? decodeSchema1(payload, registries)
                : decodeNative(payload, registries);

        return decoded.result().map(restored -> {
            if (restored.isDamageableItem() && restored.getMaxDamage() > 0) {
                restored.setDamageValue(restored.getMaxDamage());
            }
            return restored;
        });
    }

    public static boolean migrateContainer(Container container, HolderLookup.Provider registries) {
        if (container == null || registries == null) return false;
        boolean changed = false;
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (!isLegacySealed(stack)) continue;
            Optional<ItemStack> restored = unseal(stack, registries);
            if (restored.isPresent()) {
                container.setItem(i, restored.get());
                changed = true;
            }
        }
        return changed;
    }

    public static int currentDataVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    private static DataResult<ItemStack> decodeNative(CompoundTag payload, HolderLookup.Provider registries) {
        int version = payload.getIntOr("DataVersion", -1);
        int currentVersion = currentDataVersion();
        if (version < 0 || version > currentVersion) return DataResult.error(() -> "Unsupported DataVersion");
        try {
            Dynamic<Tag> upgraded = DataFixers.getDataFixer()
                    .update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, payload), version, currentVersion);
            return ItemStack.CODEC
                    .parse(registries.createSerializationContext(NbtOps.INSTANCE), upgraded.getValue())
                    .flatMap(stack -> stack.isEmpty() || isLegacySealed(stack)
                            ? DataResult.error(() -> "Invalid restored item")
                            : DataResult.success(stack));
        } catch (RuntimeException e) {
            return DataResult.error(() -> "Decode failed: " + e.getMessage());
        }
    }

    private static DataResult<ItemStack> decodeSchema1(CompoundTag payload, HolderLookup.Provider registries) {
        if (payload.getIntOr("schema", -1) != 1) {
            return DataResult.error(() -> "Unrecognized schema-1 snapshot");
        }
        int version = payload.getIntOr("data_version", -1);
        if (version < 0) return DataResult.error(() -> "Missing schema-1 data_version");
        if (!(payload.get("chunks") instanceof ListTag chunks) || chunks.isEmpty()) {
            return DataResult.error(() -> "Missing schema-1 chunks");
        }
        try {
            StringBuilder encoded = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                if (!(chunks.get(i) instanceof StringTag)) return DataResult.error(() -> "Invalid chunk");
                encoded.append(chunks.getStringOr(i, ""));
            }
            CompoundTag tag = NbtIo.readCompressed(
                    new ByteArrayInputStream(Base64.getDecoder().decode(encoded.toString())),
                    NbtAccounter.unlimitedHeap());
            tag.putInt("DataVersion", version);
            return decodeNative(tag, registries);
        } catch (IOException | RuntimeException e) {
            return DataResult.error(() -> "Schema-1 decode failed: " + e.getMessage());
        }
    }
}
