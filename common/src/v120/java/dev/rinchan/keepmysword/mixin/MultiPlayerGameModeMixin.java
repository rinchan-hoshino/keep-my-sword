package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.state.BrokenState;
import dev.rinchan.keepmysword.state.HandMask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.20.1 engine root #2 (client): mirror server-side broken-item right-click gate. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void kms$blockClientUseItem(
            Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (BrokenState.isBroken(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "performUseItemOn", at = @At("HEAD"), cancellable = true)
    private void kms$blockClientUseItemOn(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BrokenState.isBroken(stack)) return;
        if (player.isSpectator()) return;

        Level level = this.minecraft.level;
        if (level == null) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.getBlock().isEnabled(level.enabledFeatures())) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (!player.isSecondaryUseActive()) {
            HandMask.set(hand);
            try {
                InteractionResult result = state.use(level, player, hand, hitResult);
                cir.setReturnValue(result.consumesAction() ? result : InteractionResult.PASS);
            } finally {
                HandMask.clear();
            }
            return;
        }
        cir.setReturnValue(InteractionResult.PASS);
    }
}
