package dev.rinchan.keepmysword.item;

import net.minecraft.nbt.*;

public final class VersionSupport {
    public static boolean has(CompoundTag tag, String key, int type) {
        var value = tag.get(key);
        return value != null && (type == 99 ? value instanceof NumericTag : value.getId() == type);
    }

    private VersionSupport() {}
}
