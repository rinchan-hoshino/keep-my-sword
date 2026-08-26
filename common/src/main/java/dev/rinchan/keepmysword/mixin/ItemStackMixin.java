package dev.rinchan.keepmysword.mixin;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.function.TriConsumer;

import dev.rinchan.keepmysword.KeepMySword;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes broken stacks behave like model-only items. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (KeepMySword.isManagedBroken(stack)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableFinishUsing(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (KeepMySword.isManagedBroken(stack)) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableReleaseUsing(Level level, LivingEntity entity, int timeCharged, CallbackInfo ci) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "onUseTick", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseTick(Level level, LivingEntity entity, int remainingUseDuration, CallbackInfo ci) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableInventoryTick(Level level, Entity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseDuration(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableUseAnimation(CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(ItemUseAnimation.NONE);
        }
    }

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableCorrectTool(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableHurtEnemy(LivingEntity target, LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "postHurtEnemy", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disablePostHurtEnemy(LivingEntity target, LivingEntity attacker, CallbackInfo ci) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "mineBlock", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableMineBlock(Level level, BlockState state, BlockPos pos, Player player, CallbackInfo ci) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableSlotGroupModifiers(EquipmentSlotGroup slotGroup, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer, CallbackInfo ci) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), cancellable = true)
    private void keepMySword$disableSlotModifiers(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, CallbackInfo ci) {
        if (KeepMySword.isManagedBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }
}
