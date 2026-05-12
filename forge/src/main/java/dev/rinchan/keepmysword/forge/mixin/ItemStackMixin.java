package dev.rinchan.keepmysword.forge.mixin;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import dev.rinchan.rinlib.item.DamageState;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.enchantment.EnchantmentDurability;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "damageItem", at = @At("HEAD"), cancellable = true)
    private void keepMySword$damageItem(int amount, EntityLivingBase entity, CallbackInfo ci) {
        ItemStack stack = (ItemStack)(Object)this;
        if (!stack.isItemStackDamageable()) return;
        int damage = amount;
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, stack);
        for (int i = 0; unbreaking > 0 && i < amount; i++) if (EnchantmentDurability.negateDamage(stack, unbreaking, entity.getRNG())) damage--;
        if (damage > 0) stack.setItemDamage(DamageState.clampDamage(stack, stack.getItemDamage() + damage));
        ci.cancel();
    }
    @Inject(method="onItemUse", at=@At("HEAD"), cancellable=true) private void kms$onItemUse(EntityPlayer p, World w, BlockPos pos, EnumHand hand, net.minecraft.util.EnumFacing facing, float hitX, float hitY, float hitZ, CallbackInfoReturnable<EnumActionResult> cir){ if(DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(EnumActionResult.PASS); }
    @Inject(method="useItemRightClick", at=@At("HEAD"), cancellable=true) private void kms$rightClick(World w, EntityPlayer p, EnumHand h, CallbackInfoReturnable<ActionResult<ItemStack>> cir){ ItemStack s=(ItemStack)(Object)this; if(DamageState.isBroken(s)) cir.setReturnValue(new ActionResult<ItemStack>(EnumActionResult.PASS,s)); }
    @Inject(method="interactWithEntity", at=@At("HEAD"), cancellable=true) private void kms$entity(EntityPlayer p, EntityLivingBase e, EnumHand h, CallbackInfoReturnable<EnumActionResult> cir){ if(DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(EnumActionResult.PASS); }
    @Inject(method="onItemUseFinish", at=@At("HEAD"), cancellable=true) private void kms$finish(World w, EntityLivingBase e, CallbackInfoReturnable<ItemStack> cir){ ItemStack s=(ItemStack)(Object)this; if(DamageState.isBroken(s)) cir.setReturnValue(s); }
    @Inject(method="onPlayerStoppedUsing", at=@At("HEAD"), cancellable=true) private void kms$stop(World w, EntityLivingBase e, int t, CallbackInfo ci){ if(DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method="onUsingTick", at=@At("HEAD"), cancellable=true) private void kms$tick(EntityLivingBase e, int count, CallbackInfo ci){ if(DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method="updateAnimation", at=@At("HEAD"), cancellable=true) private void kms$inventory(World w, Entity e, int slot, boolean selected, CallbackInfo ci){ if(DamageState.isBroken((ItemStack)(Object)this)) ci.cancel(); }
    @Inject(method="getMaxItemUseDuration", at=@At("HEAD"), cancellable=true) private void kms$duration(CallbackInfoReturnable<Integer> cir){ if(DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(0); }
    @Inject(method="getItemUseAction", at=@At("HEAD"), cancellable=true) private void kms$action(CallbackInfoReturnable<EnumAction> cir){ if(DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(EnumAction.NONE); }
    @Inject(method="getDestroySpeed", at=@At("HEAD"), cancellable=true) private void kms$speed(IBlockState state, CallbackInfoReturnable<Float> cir){ if(DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(1.0F); }
    @Inject(method="canHarvestBlock", at=@At("HEAD"), cancellable=true) private void kms$harvest(IBlockState state, CallbackInfoReturnable<Boolean> cir){ if(DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(false); }
    @Inject(method="hitEntity", at=@At("HEAD"), cancellable=true) private void kms$hit(EntityLivingBase target, EntityPlayer player, CallbackInfoReturnable<Boolean> cir){ if(DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(false); }
    @Inject(method="onBlockDestroyed", at=@At("HEAD"), cancellable=true) private void kms$block(World world, IBlockState state, BlockPos pos, EntityPlayer player, CallbackInfoReturnable<Boolean> cir){ if(DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(false); }
    @Inject(method="getAttributeModifiers", at=@At("HEAD"), cancellable=true) private void kms$attrs(EntityEquipmentSlot slot, CallbackInfoReturnable<Multimap<String, AttributeModifier>> cir){ if(DamageState.isBroken((ItemStack)(Object)this)) cir.setReturnValue(ImmutableMultimap.<String, AttributeModifier>of()); }
}
