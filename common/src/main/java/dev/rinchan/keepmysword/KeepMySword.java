package dev.rinchan.keepmysword;

import dev.rinchan.rinlib.item.DamageState;

import net.minecraft.world.item.ItemStack;

public final class KeepMySword {
    public static final String MOD_ID = "keep_my_sword";

    private KeepMySword() {}

    public static boolean isExcluded(ItemStack stack) {
        return stack.is(VersionTags.EXCLUDED_ITEMS);
    }

    public static boolean isManagedBroken(ItemStack stack) {
        return !isExcluded(stack) && DamageState.isBroken(stack);
    }

    /** Called after vanilla damage processing, immediately before stack destruction. */
    public static boolean preserveAtBreak(ItemStack stack) {
        if (isExcluded(stack)) {
            return false;
        }
        stack.setDamageValue(DamageState.clampDamage(stack, stack.getDamageValue()));
        return true;
    }
}
