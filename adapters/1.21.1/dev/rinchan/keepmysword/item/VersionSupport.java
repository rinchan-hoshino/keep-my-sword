package dev.rinchan.keepmysword.item;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class VersionSupport {
    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath("keep_my_sword", name);
    }

    public static Item inertItem() {
        return new Item(new Item.Properties().stacksTo(1));
    }

    public static int dataVersion() {
        return net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    }

    public static int integer(CompoundTag t, String k) {
        return t.getInt(k);
    }

    public static boolean bool(CompoundTag t, String k) {
        return t.getBoolean(k);
    }

    public static String string(CompoundTag t, String k) {
        return t.getString(k);
    }

    public static CompoundTag compound(CompoundTag t, String k) {
        return t.getCompound(k);
    }

    public static boolean has(CompoundTag t, String k, int type) {
        var v = t.get(k);
        return v != null && (type == 99 ? v instanceof NumericTag : v.getId() == type);
    }

    public static CompoundTag parse(String text)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return TagParser.parseTag(text);
    }

    private VersionSupport() {}
}
