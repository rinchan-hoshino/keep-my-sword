package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.config.KeepMySwordConfig;
import dev.rinchan.keepmysword.state.BrokenState;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Engine root #6: suppress combat/protection/movement enchantments on broken gear while gating Mending by config. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Unique
    private static final ThreadLocal<Boolean> KMS_MENDING_CONTEXT = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "modifyDurabilityToRepairFromXp", at = @At("HEAD"))
    private static void kms$enterMendingContext(
            ServerLevel level, ItemStack stack, int xp, CallbackInfoReturnable<Integer> cir) {
        if (KeepMySwordConfig.get().allowMendingWhenBroken()) {
            KMS_MENDING_CONTEXT.set(Boolean.TRUE);
        }
    }

    @Inject(method = "modifyDurabilityToRepairFromXp", at = @At("RETURN"))
    private static void kms$exitMendingContext(
            ServerLevel level, ItemStack stack, int xp, CallbackInfoReturnable<Integer> cir) {
        KMS_MENDING_CONTEXT.set(Boolean.FALSE);
    }

    @Inject(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void kms$skipBrokenItemEnchantments(
            ItemStack stack, @Coerce Object visitor, CallbackInfo ci) {
        if (Boolean.TRUE.equals(KMS_MENDING_CONTEXT.get())) return;
        if (BrokenState.isBroken(stack)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void kms$skipBrokenSlotEnchantments(
            ItemStack stack,
            EquipmentSlot slot,
            LivingEntity entity,
            @Coerce Object visitor,
            CallbackInfo ci) {
        if (BrokenState.isBroken(stack)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemStack;)I",
            at = @At("HEAD"),
            cancellable = true)
    private static void kms$gateBrokenItemEnchantmentLevel(
            Holder<Enchantment> enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (!BrokenState.isBroken(stack)) return;
        if (KeepMySwordConfig.get().allowMendingWhenBroken()
                && (Boolean.TRUE.equals(KMS_MENDING_CONTEXT.get())
                        || (enchantment != null
                                && enchantment.isBound()
                                && enchantment.value().effects().has(EnchantmentEffectComponents.REPAIR_WITH_XP)))) {
            return;
        }
        cir.setReturnValue(0);
    }

    @ModifyVariable(method = "getRandomItemWith", at = @At("HEAD"), argsOnly = true)
    private static Predicate<ItemStack> kms$filterRandomItemWith(
            Predicate<ItemStack> predicate,
            DataComponentType<?> componentType,
            LivingEntity entity) {
        if (componentType == EnchantmentEffectComponents.REPAIR_WITH_XP
                && KeepMySwordConfig.get().allowMendingWhenBroken()) {
            return predicate;
        }
        return stack -> !BrokenState.isBroken(stack) && predicate.test(stack);
    }
}
