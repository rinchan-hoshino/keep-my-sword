package dev.rinchan.keepmysword.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class FabricItemStackDamageMixin {
    @WrapMethod(
            method =
                    "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V")
    private void kms$transaction(
            int amount,
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.server.level.ServerPlayer owner,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBreak,
            Operation<Void> original) {
        SnapshotScope.damage(
                level.registryAccess(),
                () -> {
                    original.call(amount, level, owner, onBreak);
                    return null;
                });
    }

    @Inject(
            method =
                    "applyDamage(ILnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
            cancellable = true)
    private void kms$preserve(CallbackInfo ci) {
        var stack = (ItemStack) (Object) this;
        var registries = SnapshotScope.registries();
        if (registries != null) SealedItems.sealInPlace(stack, registries);
        else {
            var sealed = SealedItems.seal(stack);
            if (sealed != stack) ((ReplaceableStack) (Object) stack).kms$replaceWith(sealed);
        }
        if (dev.rinchan.keepmysword.KeepMySword.isManagedBroken(stack)) ci.cancel();
    }
}
