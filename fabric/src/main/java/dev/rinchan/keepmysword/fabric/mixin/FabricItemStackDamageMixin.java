package dev.rinchan.keepmysword.fabric.mixin;

import java.util.function.Consumer;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.rinlib.item.DamageState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves a stack only at the vanilla destruction boundary. */
@Mixin(ItemStack.class)
public abstract class FabricItemStackDamageMixin {
    @Inject(
        method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void keepMySword$preserveBeforeDestruction(
        int amount,
        ServerLevel level,
        @Nullable ServerPlayer player,
        Consumer<Item> onBroken,
        CallbackInfo ci
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        if (KeepMySword.isExcluded(stack)) {
            return;
        }

        stack.setDamageValue(DamageState.clampDamage(stack, stack.getDamageValue()));
        ci.cancel();
    }
}
