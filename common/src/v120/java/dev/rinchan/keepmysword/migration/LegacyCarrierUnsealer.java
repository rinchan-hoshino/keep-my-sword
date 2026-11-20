package dev.rinchan.keepmysword.migration;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One-way lossless unsealer for 1.20.1 NBT legacy sealed recovery-compass carriers. */
public final class LegacyCarrierUnsealer {
    public static final String SEALED_KEY = "keep_my_sword:sealed";

    private LegacyCarrierUnsealer() {}

    public static boolean isLegacySealed(ItemStack stack) {
        if (stack == null || !stack.is(Items.RECOVERY_COMPASS)) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(SEALED_KEY, 10);
    }

    public static Optional<ItemStack> unseal(ItemStack carrier) {
        if (!isLegacySealed(carrier)) return Optional.empty();
        CompoundTag tag = carrier.getTag();
        if (tag == null) return Optional.empty();
        CompoundTag payload = tag.getCompound(SEALED_KEY);
        ItemStack restored = ItemStack.of(payload);
        if (restored.isEmpty() || isLegacySealed(restored)) return Optional.empty();
        if (restored.isDamageableItem() && restored.getMaxDamage() > 0) {
            restored.setDamageValue(restored.getMaxDamage());
        }
        return Optional.of(restored);
    }

    public static boolean migrateContainer(Container container) {
        if (container == null) return false;
        boolean changed = false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!isLegacySealed(stack)) continue;
            Optional<ItemStack> restored = unseal(stack);
            if (restored.isPresent()) {
                container.setItem(i, restored.get());
                changed = true;
            }
        }
        return changed;
    }
}
