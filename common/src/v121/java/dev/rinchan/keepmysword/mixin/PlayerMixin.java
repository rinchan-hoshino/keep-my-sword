package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.state.BrokenState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Engine roots #2 (entity interaction before mobInteract) & #3 (melee gate in Player.attack). */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void kms$blockInteractOn(
            Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player self = (Player) (Object) this;
        if (BrokenState.isBroken(self.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getWeaponItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack kms$hideBrokenWeaponInAttack(Player player) {
        ItemStack stack = player.getWeaponItem();
        return BrokenState.isBroken(stack) ? ItemStack.EMPTY : stack;
    }

    @Redirect(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack kms$hideBrokenHandInAttack(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return BrokenState.isBroken(stack) ? ItemStack.EMPTY : stack;
    }
}
