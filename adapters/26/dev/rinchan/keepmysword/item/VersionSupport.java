package dev.rinchan.keepmysword.item;

import net.minecraft.nbt.*;

public final class VersionSupport {
    public static int dataVersion() {
        return net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version();
    }

    public static int integer(CompoundTag tag, String key) {
        return tag.getIntOr(key, 0);
    }

    public static boolean has(CompoundTag tag, String key, int type) {
        var value = tag.get(key);
        return value != null && (type == 99 ? value instanceof NumericTag : value.getId() == type);
    }

    public static String chunk(ListTag list, int index) {
        return list.getString(index).orElseThrow();
    }

    private VersionSupport() {}
}
