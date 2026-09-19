package dev.rinchan.keepmysword.item;

import com.mojang.serialization.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

/** Versioned shell annotations are kept separate from the original component values. */
public final class ShellPersistence {
    public static final String KEY = "keep_my_sword:sealed_shell";
    private static final int SCHEMA = 3;

    private static int version() {
        return VersionSupport.dataVersion();
    }

    private static ItemStack shellOnly(ItemStack stack) {
        return SnapshotScope.call(
                () -> {
                    var shell = new ItemStack(Items.PAPER);
                    shell.applyComponents(stack.getComponents());
                    shell.remove(SealedRegistry.ORIGINAL);
                    return shell;
                });
    }

    public static <T> DataResult<ItemStack> encode(ItemStack stack, DynamicOps<T> ops) {
        if (!SealedItems.isSealed(stack)) return DataResult.success(stack);
        ItemStack original = SealedItems.originalForSave(stack);
        var baseline = SealedItems.seal(original);
        var shell = shellOnly(stack);
        if (ItemStack.isSameItemSameComponents(shell, shellOnly(baseline)))
            return DataResult.success(original);
        boolean inheritName =
                java.util.Objects.equals(
                        stack.get(DataComponents.CUSTOM_NAME), original.getHoverName());
        if (inheritName) shell.remove(DataComponents.CUSTOM_NAME);
        var data = original.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return StorageBoundary.raw()
                .encodeStart(nbtOps(ops), shell)
                .map(
                        encoded -> {
                            var envelope = new CompoundTag();
                            envelope.putInt("schema", SCHEMA);
                            envelope.putBoolean("inherit_name", inheritName);
                            envelope.putInt("data_version", version());
                            envelope.putBoolean(
                                    "had_custom_data", original.has(DataComponents.CUSTOM_DATA));
                            if (data.contains(KEY)) envelope.put("previous", data.get(KEY).copy());
                            envelope.putString("shell_snbt", encoded.toString());
                            data.put(KEY, envelope);
                            original.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                            return original;
                        });
    }

    public static <T> ItemStack decode(ItemStack source, DynamicOps<T> ops) {
        if (SealedRegistry.ORIGINAL == null || source.isEmpty()) return source;
        // Repaired items retain the reserved annotation until the next break; no active abilities
        // are copied from it.
        if (!source.isDamageableItem() || source.getDamageValue() < source.getMaxDamage())
            return source;
        var data = source.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!VersionSupport.has(data, KEY, Tag.TAG_COMPOUND)) return SealedItems.seal(source);
        var envelope = VersionSupport.compound(data, KEY);
        int schema = VersionSupport.integer(envelope, "schema");
        if ((schema != 1 && schema != 2 && schema != SCHEMA)
                || !VersionSupport.has(envelope, "data_version", 99)
                || VersionSupport.integer(envelope, "data_version") < 0
                || VersionSupport.integer(envelope, "data_version") > version())
            return SealedItems.seal(source);
        CompoundTag shellTag;
        try {
            if (schema == 1 && VersionSupport.has(envelope, "shell", Tag.TAG_COMPOUND))
                shellTag = VersionSupport.compound(envelope, "shell");
            else if ((schema == 2 || schema == SCHEMA)
                    && VersionSupport.has(envelope, "shell_snbt", Tag.TAG_STRING))
                shellTag = VersionSupport.parse(VersionSupport.string(envelope, "shell_snbt"));
            else return SealedItems.seal(source);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return SealedItems.seal(source);
        }
        var upgraded =
                DataFixers.getDataFixer()
                        .update(
                                References.ITEM_STACK,
                                new Dynamic<>(NbtOps.INSTANCE, shellTag),
                                VersionSupport.integer(envelope, "data_version"),
                                version());
        var decoded = StorageBoundary.raw().parse(nbtOps(ops), upgraded.getValue());
        if (decoded.error().isPresent() || decoded.result().isEmpty())
            return SealedItems.seal(source);
        var shell = decoded.result().orElseThrow();
        if (!shell.is(Items.PAPER) || shell.has(SealedRegistry.ORIGINAL))
            return SealedItems.seal(source);
        var original = SnapshotScope.call(source::copy);
        data.remove(KEY);
        if (envelope.contains("previous")) data.put(KEY, envelope.get("previous").copy());
        if (data.isEmpty() && !VersionSupport.bool(envelope, "had_custom_data"))
            original.remove(DataComponents.CUSTOM_DATA);
        else original.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        var sealed = SealedItems.seal(original);
        if (!SealedItems.isSealed(sealed)) return source;
        var restored = new ItemStack(SealedRegistry.INERT, source.getCount());
        restored.applyComponents(shell.getComponents());
        restored.set(DataComponents.MAX_STACK_SIZE, 1);
        restored.set(SealedRegistry.ORIGINAL, new OriginalPayload(original));
        if (schema >= 3 && VersionSupport.bool(envelope, "inherit_name"))
            restored.set(DataComponents.CUSTOM_NAME, original.getHoverName());
        return restored;
    }

    private static DynamicOps<Tag> nbtOps(DynamicOps<?> ops) {
        return ops instanceof net.minecraft.resources.RegistryOps<?> registryOps
                ? registryOps.withParent(NbtOps.INSTANCE)
                : NbtOps.INSTANCE;
    }

    private ShellPersistence() {}
}
