package studio.cattail.noitembreak.mixin;

import java.util.function.Consumer;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps damageable items at zero durability instead of shrinking the stack. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(
        method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void noItemBreak$hurtAndBreakWithoutDestroy(int amount, ServerLevel level, @Nullable LivingEntity entity, Consumer<Item> onBroken, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.isDamageableItem()) {
            return;
        }

        int damage = stack.getItem().damageItem(stack, amount, entity, onBroken);
        if (entity != null && entity.hasInfiniteMaterials()) {
            ci.cancel();
            return;
        }

        if (damage > 0) {
            damage = EnchantmentHelper.processDurabilityChange(level, stack, damage);
            if (damage <= 0) {
                ci.cancel();
                return;
            }
        }

        if (entity instanceof ServerPlayer player && damage != 0) {
            CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, stack, stack.getDamageValue() + damage);
        }

        int newDamage = stack.getDamageValue() + damage;
        stack.setDamageValue(Math.max(0, Math.min(newDamage, stack.getMaxDamage())));
        ci.cancel();
    }
}
