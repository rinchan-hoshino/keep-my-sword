package dev.rinchan.keepmysword.forge;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dev.rinchan.rinlib.item.DamageState;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public final class KeepMySwordForgeEvents {
    @SubscribeEvent
    public void keepBrokenItem(PlayerDestroyItemEvent event) {
        ItemStack original = event.original;
        if (original == null || !original.isItemStackDamageable()) return;
        ItemStack broken = original.copy();
        broken.stackSize = 1;
        broken.setItemDamage(DamageState.clampDamage(broken, broken.getMaxDamage()));
        event.entityPlayer.inventory.setInventorySlotContents(event.entityPlayer.inventory.currentItem, broken);
    }

    @SubscribeEvent
    public void blockUse(PlayerInteractEvent event) {
        ItemStack stack = event.entityPlayer.getCurrentEquippedItem();
        if (DamageState.isBroken(stack)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void blockAttack(AttackEntityEvent event) {
        ItemStack stack = event.entityPlayer.getCurrentEquippedItem();
        if (DamageState.isBroken(stack)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void normalizeBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack stack = event.entityPlayer.getCurrentEquippedItem();
        if (DamageState.isBroken(stack)) event.newSpeed = 1.0F;
    }
}
