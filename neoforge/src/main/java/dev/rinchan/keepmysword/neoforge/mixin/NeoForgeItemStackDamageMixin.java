package dev.rinchan.keepmysword.neoforge.mixin;

import java.util.function.Consumer;

import dev.rinchan.rinlib.item.DamageState;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps zero-durability stacks present on NeoForge. */
@Mixin(ItemStack.class)
public abstract class NeoForgeItemStackDamageMixin {
    @Inject(
        method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void keepMySword$hurtWithoutDestroy(int amount, ServerLevel level, @Nullable LivingEntity entity, Consumer<Item> onBroken, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.isDamageableItem()) {
            return;
        }

        ServerPlayer player = entity instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (player != null && player.hasInfiniteMaterials()) {
            ci.cancel();
            return;
        }

        int damage = amount;
        if (damage > 0) {
            damage = EnchantmentHelper.processDurabilityChange(level, stack, damage);
            if (damage <= 0) {
                ci.cancel();
                return;
            }
        }

        if (player != null && damage != 0) {
            CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, stack, stack.getDamageValue() + damage);
        }

        stack.setDamageValue(DamageState.clampDamage(stack, stack.getDamageValue() + damage));
        ci.cancel();
    }

    @Inject(method = "onItemUseFirst", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseFirst(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (DamageState.isBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
