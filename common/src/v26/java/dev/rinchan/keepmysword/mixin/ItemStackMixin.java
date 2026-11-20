package dev.rinchan.keepmysword.mixin;

import dev.rinchan.keepmysword.state.BrokenState;
import dev.rinchan.keepmysword.state.DurabilityPolicy;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.x engine roots #1 (destruction/first-break), #3 (melee), #4 (mining), #5 (attributes/tick), and tooltip. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Unique
    private static final ThreadLocal<LivingEntity> KMS_HURT_OWNER = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<Integer> KMS_BEFORE_DAMAGE = new ThreadLocal<>();

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"))
    private void kms$captureOwner(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        KMS_HURT_OWNER.set(entity);
    }

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("RETURN"))
    private void kms$clearOwner(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        KMS_HURT_OWNER.remove();
    }

    @Inject(
            method = {
                "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
                "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"
            },
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void kms$skipIfAlreadyBroken(
            int amount, ServerLevel level, @Coerce LivingEntity entity, Consumer<Item> onBreak, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (amount > 0 && BrokenState.isBroken(self)) {
            if (self.getDamageValue() != self.getMaxDamage()) {
                self.setDamageValue(self.getMaxDamage());
            }
            ci.cancel();
        }
    }

    @Inject(
            method = {
                "applyDamage(ILnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
                "applyDamage(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"
            },
            at = @At("HEAD"),
            require = 1)
    private void kms$captureBeforeDamage(
            int newDamage, @Coerce LivingEntity entity, Consumer<Item> onBreak, CallbackInfo ci) {
        KMS_BEFORE_DAMAGE.set(((ItemStack) (Object) this).getDamageValue());
    }

    @Inject(
            method = {
                "applyDamage(ILnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
                "applyDamage(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
            cancellable = true,
            require = 1)
    private void kms$preventDestruction(
            int newDamage, @Coerce LivingEntity entity, Consumer<Item> onBreak, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (!BrokenState.isProtectable(self)) return;

        int maxDamage = self.getMaxDamage();
        Integer captured = KMS_BEFORE_DAMAGE.get();
        int beforeDamage = captured != null ? captured : Math.max(0, maxDamage - 1);
        int delta = Math.max(0, newDamage - beforeDamage);
        DurabilityPolicy.HurtOutcome outcome =
                DurabilityPolicy.evaluateHurt(beforeDamage, delta, maxDamage, true);

        self.setDamageValue(outcome.resultingDamage());
        if (outcome.firstBreak()) {
            LivingEntity owner = entity != null ? entity : KMS_HURT_OWNER.get();
            ServerLevel level = owner != null && owner.level() instanceof ServerLevel sl ? sl : null;
            BrokenState.onFirstBreak(self, level, owner);
        }
        KMS_BEFORE_DAMAGE.remove();
        ci.cancel();
    }

    @Inject(
            method = {
                "applyDamage(ILnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
                "applyDamage(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"
            },
            at = @At("RETURN"),
            require = 1)
    private void kms$clearBeforeDamage(
            int newDamage, @Coerce LivingEntity entity, Consumer<Item> onBreak, CallbackInfo ci) {
        KMS_BEFORE_DAMAGE.remove();
    }

    // --- Root #2: ItemStack self-use & continuous-use gates ---

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void kms$blockUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void kms$blockUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void kms$blockInteractLivingEntity(
            Player player,
            LivingEntity target,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "onUseTick", at = @At("HEAD"), cancellable = true)
    private void kms$blockOnUseTick(
            Level level, LivingEntity entity, int remainingUseDuration, CallbackInfo ci) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            if (entity != null && entity.isUsingItem()) {
                entity.stopUsingItem();
            }
            ci.cancel();
        }
    }

    // --- Root #3: Melee item hooks ---

    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void kms$blockHurtEnemy(
            LivingEntity target, LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "postHurtEnemy", at = @At("HEAD"), cancellable = true)
    private void kms$blockPostHurtEnemy(LivingEntity target, LivingEntity attacker, CallbackInfo ci) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    // --- Root #4: Mining gate ---

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void kms$neutralDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void kms$blockCorrectTool(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mineBlock", at = @At("HEAD"), cancellable = true)
    private void kms$blockMineBlock(
            Level level, BlockState state, BlockPos pos, Player player, CallbackInfo ci) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    // --- Root #5: Attributes & passive tick ---

    @Inject(
            method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void kms$clearSlotModifiers(
            EquipmentSlot slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            CallbackInfo ci) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void kms$clearGroupModifiers(
            EquipmentSlotGroup group,
            TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer,
            CallbackInfo ci) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    private void kms$blockInventoryTick(
            Level level, Entity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    // --- UI: Tooltip line 2 ---

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void kms$appendBrokenTooltip(
            Item.TooltipContext context,
            Player player,
            TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!BrokenState.isBroken(self)) return;
        List<Component> lines = cir.getReturnValue();
        if (lines != null && !lines.isEmpty()) {
            lines.add(1, Component.translatable("item.keep_my_sword.broken").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
