package dev.rinchan.keepmysword.neoforge.mixin;

import java.util.function.Consumer;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.rinlib.item.DamageState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves a stack only at the vanilla destruction boundary. */
@Mixin(ItemStack.class)
public abstract class NeoForgeItemStackDamageMixin {
    @Inject(
        method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
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
        @Nullable LivingEntity entity,
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

    @Inject(method = "onItemUseFirst", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseFirst(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
