package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.config.KeepMySwordConfig;
import dev.rinchan.keepmysword.state.BrokenState;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.20.1 engine root #6: suppress combat/protection/movement enchantments on broken gear while gating Mending by config. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(
            method = "runIterationOnItem(Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void kms$skipBrokenItemEnchantments(
            @Coerce Object visitor, ItemStack stack, CallbackInfo ci) {
        if (BrokenState.isBroken(stack)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "getItemEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/item/ItemStack;)I",
            at = @At("HEAD"),
            cancellable = true)
    private static void kms$gateBrokenItemEnchantmentLevel(
            Enchantment enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!BrokenState.isBroken(stack)) return;
        if (enchantment == Enchantments.MENDING && KeepMySwordConfig.get().allowMendingWhenBroken()) {
            return;
        }
        cir.setReturnValue(0);
    }

    @Inject(
            method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I",
            at = @At("HEAD"),
            cancellable = true)
    private static void kms$skipBrokenEquipmentLevel(
            Enchantment enchantment, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        Map<EquipmentSlot, ItemStack> slots = enchantment.getSlotItems(entity);
        if (slots.isEmpty()) return;
        boolean anyBroken = false;
        for (ItemStack stack : slots.values()) {
            if (BrokenState.isBroken(stack)) {
                anyBroken = true;
                break;
            }
        }
        if (!anyBroken) return;
        int best = 0;
        for (ItemStack stack : slots.values()) {
            if (BrokenState.isBroken(stack)) continue;
            int level = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
            if (level > best) best = level;
        }
        cir.setReturnValue(best);
    }

    @ModifyVariable(
            method = "getRandomItemWith(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Map$Entry;",
            at = @At("HEAD"),
            argsOnly = true)
    private static Predicate<ItemStack> kms$filterRandomItemWith(
            Predicate<ItemStack> predicate,
            Enchantment enchantment,
            LivingEntity entity) {
        if (enchantment == Enchantments.MENDING && KeepMySwordConfig.get().allowMendingWhenBroken()) {
            return predicate;
        }
        return stack -> !BrokenState.isBroken(stack) && predicate.test(stack);
    }
}
