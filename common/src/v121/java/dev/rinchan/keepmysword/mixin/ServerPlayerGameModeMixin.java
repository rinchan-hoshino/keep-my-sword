package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.state.BrokenState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Engine root #2 (server): block right-click item use while still allowing bare-hand block menus. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void kms$blockUseItem(
            ServerPlayer player,
            Level level,
            ItemStack stack,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (BrokenState.isBroken(stack)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void kms$blockUseItemOn(
            ServerPlayer player,
            Level level,
            ItemStack stack,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (!BrokenState.isBroken(stack)) return;
        if (player.isSpectator()) return;

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.getBlock().isEnabled(level.enabledFeatures())) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (!player.isSecondaryUseActive()) {
            InteractionResult result = state.useWithoutItem(level, player, hitResult);
            cir.setReturnValue(result.consumesAction() ? result : InteractionResult.PASS);
            return;
        }
        cir.setReturnValue(InteractionResult.PASS);
    }
}
