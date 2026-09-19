package dev.rinchan.keepmysword.item;

import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class VersionSupport {
    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("keep_my_sword", name);
    }

    public static Item inertItem() {
        return new Item(
                new Item.Properties()
                        .stacksTo(1)
                        .setId(
                                net.minecraft.resources.ResourceKey.create(
                                        net.minecraft.core.registries.Registries.ITEM,
                                        id("broken_item"))));
    }

    public static int dataVersion() {
        return net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version();
    }

    public static int integer(CompoundTag t, String k) {
        return t.getIntOr(k, 0);
    }

    public static boolean bool(CompoundTag t, String k) {
        return t.getBooleanOr(k, false);
    }

    public static String string(CompoundTag t, String k) {
        return t.getStringOr(k, "");
    }

    public static CompoundTag compound(CompoundTag t, String k) {
        return t.getCompoundOrEmpty(k);
    }

    public static boolean has(CompoundTag t, String k, int type) {
        var v = t.get(k);
        return v != null && (type == 99 ? v instanceof NumericTag : v.getId() == type);
    }

    public static CompoundTag parse(String text)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return TagParser.parseCompoundFully(text);
    }

    private VersionSupport() {}
}
