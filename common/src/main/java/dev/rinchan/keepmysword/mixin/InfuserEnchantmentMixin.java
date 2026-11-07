package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "fuzs.enchantinginfuser.world.inventory.InfuserMenu", remap = false)
public abstract class InfuserEnchantmentMixin {
    @Shadow @Final private Container enchantSlots;

    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void kms$keepBrokenSealed(Player player, CallbackInfoReturnable<Boolean> cir) {
        // Unreadable/excluded inputs stay sealed; exposed originals use the native enchanting transaction.
        if (SealedItems.isSealed(enchantSlots.getItem(0))) cir.setReturnValue(false);
    }
}
