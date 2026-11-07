package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.item.RepairView;
import dev.rinchan.keepmysword.item.SealedItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class RepairViewPlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void kms$workTick(CallbackInfo ci) { RepairView.tick((ServerPlayer) (Object) this); }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"))
    private void kms$sealDrop(ItemStack stack, boolean scatter, boolean trace, CallbackInfoReturnable<?> cir) {
        var player = (ServerPlayer) (Object) this;
        SealedItems.sealInPlace(stack, player.level().registryAccess());
    }

    @Inject(method = {"doCloseContainer", "die", "disconnect"}, at = @At("HEAD"))
    private void kms$closeWork(CallbackInfo ci) {
        var player = (ServerPlayer) (Object) this;
        RepairView.close(player.containerMenu, player);
    }
}
