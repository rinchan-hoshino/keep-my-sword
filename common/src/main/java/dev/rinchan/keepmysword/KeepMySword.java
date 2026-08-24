package dev.rinchan.keepmysword;

import dev.rinchan.rinlib.item.DamageState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class KeepMySword {
    public static final String MOD_ID = "keep_my_sword";
    public static final TagKey<Item> EXCLUDED_ITEMS = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "excluded")
    );

    private KeepMySword() {
    }

    public static boolean isExcluded(ItemStack stack) {
        return stack.is(EXCLUDED_ITEMS);
    }

    public static boolean isManagedBroken(ItemStack stack) {
        return !isExcluded(stack) && DamageState.isBroken(stack);
    }
}
