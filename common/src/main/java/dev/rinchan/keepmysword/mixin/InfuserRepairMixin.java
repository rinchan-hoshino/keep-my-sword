package dev.rinchan.keepmysword.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "fuzs.enchantinginfuser.world.inventory.InfuserMenu", remap = false)
public abstract class InfuserRepairMixin {
    @Shadow @Final private Player player;
    @Shadow @Final private Container enchantSlots;

    @ModifyReturnValue(method = "getEnchantableStack", at = @At("RETURN"))
    private ItemStack kms$repairView(ItemStack stack) {
        return SealedItems.originalForRepair(stack, player.level());
    }

    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void kms$keepBrokenSealed(Player player, CallbackInfoReturnable<Boolean> cir) {
        // Only a successful repair may replace the carrier with an active original item.
        if (SealedItems.isSealed(enchantSlots.getItem(0))) cir.setReturnValue(false);
    }
}
