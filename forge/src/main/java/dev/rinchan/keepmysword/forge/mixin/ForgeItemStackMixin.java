package dev.rinchan.keepmysword.forge.mixin;

import java.util.Random;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import dev.rinchan.rinlib.item.DamageState;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.UnbreakingEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.item.ItemUseContext;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ForgeItemStackMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void keepMySword$hurtWithoutDestroy(int amount, Random random, ServerPlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.isDamageableItem()) { cir.setReturnValue(false); return; }
        int damage = amount;
        int unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack);
        for (int i = 0; unbreakingLevel > 0 && i < amount; i++) {
            if (UnbreakingEnchantment.shouldIgnoreDurabilityDrop(stack, unbreakingLevel, random)) damage--;
        }
        if (damage <= 0) { cir.setReturnValue(false); return; }
        if (player != null) CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, stack, stack.getDamageValue() + damage);
        stack.setDamageValue(DamageState.clampDamage(stack, stack.getDamageValue() + damage));
        cir.setReturnValue(false);
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseOn(ItemUseContext context, CallbackInfoReturnable<ActionResultType> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(ActionResultType.PASS); }
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUse(World level, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult<ItemStack>> cir) { ItemStack stack=(ItemStack)(Object)this; if (DamageState.isBroken(stack)) cir.setReturnValue(ActionResult.pass(stack)); }
    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableEntityUse(PlayerEntity player, LivingEntity target, Hand hand, CallbackInfoReturnable<ActionResultType> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(ActionResultType.PASS); }
    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableFinishUsing(World level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) { ItemStack stack=(ItemStack)(Object)this; if (DamageState.isBroken(stack)) cir.setReturnValue(stack); }
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableReleaseUsing(World level, LivingEntity entity, int timeCharged, CallbackInfo ci) { if (DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method = "onUseTick", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseTick(World level, LivingEntity entity, int remainingUseDuration, CallbackInfo ci) { if (DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableInventoryTick(World level, Entity entity, int inventorySlot, boolean isCurrentItem, CallbackInfo ci) { if (DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseDuration(CallbackInfoReturnable<Integer> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(0); }
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseAnimation(CallbackInfoReturnable<UseAction> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(UseAction.NONE); }
    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(1.0F); }
    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableCorrectTool(BlockState state, CallbackInfoReturnable<Boolean> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(false); }
    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableHurtEnemy(LivingEntity target, PlayerEntity player, CallbackInfo ci) { if (DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method = "mineBlock", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableMineBlock(World level, BlockState state, BlockPos pos, PlayerEntity player, CallbackInfo ci) { if (DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method = "getAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableAttributeModifiers(EquipmentSlotType slot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) { if (DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(ImmutableMultimap.<Attribute, AttributeModifier>of()); }
}
