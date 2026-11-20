package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.migration.LegacyCarrierUnsealer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Automatically unseals legacy 1.20.1 sealed recovery-compass carriers into native broken ItemStacks. */
@Mixin(ServerPlayer.class)
public abstract class LegacyMigrationPlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void kms$migrateLegacySealedItems(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.tickCount % 20 != 0) return;

        LegacyCarrierUnsealer.migrateContainer(self.getInventory());
        if (self.containerMenu != null && self.containerMenu != self.inventoryMenu) {
            for (Slot slot : self.containerMenu.slots) {
                ItemStack stack = slot.getItem();
                if (!LegacyCarrierUnsealer.isLegacySealed(stack)) continue;
                LegacyCarrierUnsealer.unseal(stack).ifPresent(slot::set);
            }
        }
    }
}
