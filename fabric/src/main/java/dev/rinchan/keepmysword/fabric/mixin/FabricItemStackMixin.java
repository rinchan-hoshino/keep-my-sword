package dev.rinchan.keepmysword.fabric.mixin;

import java.util.Random;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import dev.rinchan.rinlib.item.DamageState;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class FabricItemStackMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void keepMySword$hurtWithoutDestroy(int amount, Random random, ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.isDamageableItem()) { cir.setReturnValue(false); return; }
        int damage = amount;
        int unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack);
        for (int i = 0; unbreakingLevel > 0 && i < amount; i++) {
            if (DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(stack, unbreakingLevel, random)) damage--;
        }
        if (damage <= 0) { cir.setReturnValue(false); return; }
        if (player != null) CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, stack, stack.getDamageValue() + damage);
        stack.setDamageValue(DamageState.clampDamage(stack, stack.getDamageValue() + damage));
        cir.setReturnValue(false);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(InteractionResult.PASS); }
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) { ItemStack stack=(ItemStack)(Object)this; if (DamageState.isBroken(stack)) cir.setReturnValue(InteractionResultHolder.pass(stack)); }
    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableEntityUse(Player player, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(InteractionResult.PASS); }
    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableFinishUsing(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) { ItemStack stack=(ItemStack)(Object)this; if (DamageState.isBroken(stack)) cir.setReturnValue(stack); }
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableReleaseUsing(Level level, LivingEntity entity, int timeCharged, CallbackInfo ci) { if (DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method = "onUseTick", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseTick(Level level, LivingEntity entity, int remainingUseDuration, CallbackInfo ci) { if (DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableInventoryTick(Level level, Entity entity, int inventorySlot, boolean isCurrentItem, CallbackInfo ci) { if (DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseDuration(CallbackInfoReturnable<Integer> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(0); }
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseAnimation(CallbackInfoReturnable<UseAnim> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(UseAnim.NONE); }
    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(1.0F); }
    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableCorrectTool(BlockState state, CallbackInfoReturnable<Boolean> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(false); }
    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableHurtEnemy(LivingEntity target, Player player, CallbackInfo ci) { if (DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method = "mineBlock", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableMineBlock(Level level, BlockState state, BlockPos pos, Player player, CallbackInfo ci) { if (DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method = "getAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableAttributeModifiers(EquipmentSlot slot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(ImmutableMultimap.<Attribute, AttributeModifier>of()); }
}
