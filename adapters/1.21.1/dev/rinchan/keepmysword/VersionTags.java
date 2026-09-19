package dev.rinchan.keepmysword;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

final class VersionTags {
    public static final TagKey<Item> EXCLUDED_ITEMS =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(KeepMySword.MOD_ID, "excluded"));

    private VersionTags() {}
}
