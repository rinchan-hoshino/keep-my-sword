package dev.rinchan.keepmysword.item;

import net.minecraft.world.item.ItemStack;

/** Shared item-durability state helpers for RinChan mods. */
public final class DamageState {
    /** ARGB overlay used when a broken item is rendered inside a GUI slot. */
    public static final int BROKEN_SLOT_OVERLAY_ARGB = 0x66FF0000;

    /** RGB tint used when a broken item is rendered as an in-world or in-hand model. */
    public static final float BROKEN_SURFACE_RED = 1.0F;
    public static final float BROKEN_SURFACE_GREEN = 0.18F;
    public static final float BROKEN_SURFACE_BLUE = 0.18F;

    private DamageState() {
    }

    public static boolean isBroken(ItemStack stack) {
        return !stack.isEmpty()
            && stack.isDamageableItem()
            && stack.getMaxDamage() > 0
            && stack.getDamageValue() >= stack.getMaxDamage();
    }

    public static int clampDamage(ItemStack stack, int damage) {
        if (!stack.isDamageableItem()) {
            return damage;
        }
        return Math.max(0, Math.min(damage, stack.getMaxDamage()));
    }
}
