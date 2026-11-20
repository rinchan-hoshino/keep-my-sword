package dev.rinchan.keepmysword.mixin;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import dev.rinchan.keepmysword.state.BrokenState;
import dev.rinchan.keepmysword.state.DurabilityPolicy;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.20.1 engine roots #1 (destruction/first-break), #2 (item use), #3 (melee), #4 (mining), #5 (attributes/tick), and tooltip. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Unique
    private static final ThreadLocal<Boolean> KMS_IN_HURT_AND_BREAK = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "hurtAndBreak", at = @At("HEAD"), cancellable = true)
    private <T extends LivingEntity> void kms$skipIfAlreadyBroken(
            int amount, T entity, Consumer<T> onBreak, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (amount > 0 && BrokenState.isBroken(self)) {
            if (self.getDamageValue() != self.getMaxDamage()) {
                self.setDamageValue(self.getMaxDamage());
            }
            ci.cancel();
            return;
        }
        KMS_IN_HURT_AND_BREAK.set(Boolean.TRUE);
    }

    @Inject(
            method = "hurtAndBreak",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"),
            cancellable = true)
    private <T extends LivingEntity> void kms$preventDestruction(
            int amount, T entity, Consumer<T> onBreak, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (!BrokenState.isProtectable(self)) return;

        int maxDamage = self.getMaxDamage();
        int beforeDamage = Math.max(0, self.getDamageValue() - Math.max(0, amount));
        DurabilityPolicy.HurtOutcome outcome =
                DurabilityPolicy.evaluateHurt(beforeDamage, Math.max(0, amount), maxDamage, true);

        self.setDamageValue(outcome.resultingDamage());
        if (outcome.firstBreak()) {
            ServerLevel level = entity != null && entity.level() instanceof ServerLevel sl ? sl : null;
            BrokenState.onFirstBreak(self, level, entity);
        }
        KMS_IN_HURT_AND_BREAK.set(Boolean.FALSE);
        ci.cancel();
    }

    @Inject(method = "hurtAndBreak", at = @At("RETURN"))
    private <T extends LivingEntity> void kms$exitHurtAndBreak(
            int amount, T entity, Consumer<T> onBreak, CallbackInfo ci) {
        KMS_IN_HURT_AND_BREAK.set(Boolean.FALSE);
    }

    // Protect direct callers of ItemStack.hurt (e.g., dispensers using shears or flint and steel) from zeroing count.
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void kms$skipDirectHurtIfAlreadyBroken(
            int amount, RandomSource random, ServerPlayer user, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(KMS_IN_HURT_AND_BREAK.get())) return;
        ItemStack self = (ItemStack) (Object) this;
        if (amount > 0 && BrokenState.isBroken(self)) {
            if (self.getDamageValue() != self.getMaxDamage()) {
                self.setDamageValue(self.getMaxDamage());
            }
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At("RETURN"), cancellable = true)
    private void kms$clampDirectHurtDestruction(
            int amount, RandomSource random, ServerPlayer user, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(KMS_IN_HURT_AND_BREAK.get())) return;
        if (!cir.getReturnValueZ()) return;
        ItemStack self = (ItemStack) (Object) this;
        if (!BrokenState.isProtectable(self)) return;
        self.setDamageValue(self.getMaxDamage());
        cir.setReturnValue(false);
    }

    // --- Root #2: ItemStack self-use & continuous-use gates ---

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void kms$blockUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (BrokenState.isBroken(self)) {
            cir.setReturnValue(InteractionResultHolder.pass(self));
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

    // --- Root #3: Melee item hook ---

    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void kms$blockHurtEnemy(LivingEntity target, Player player, CallbackInfo ci) {
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

    @Inject(method = "getAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private void kms$clearAttributeModifiers(
            EquipmentSlot slot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            cir.setReturnValue(ImmutableMultimap.of());
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    private void kms$blockInventoryTick(
            Level level, Entity entity, int slotId, boolean isSelected, CallbackInfo ci) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    // Forge 1.20.1 armor tick extension (optional so Fabric 1.20.1 skips cleanly).
    @Inject(method = "onArmorTick", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kms$blockArmorTick(Level level, Player player, CallbackInfo ci) {
        if (BrokenState.isBroken((ItemStack) (Object) this)) {
            ci.cancel();
        }
    }

    // --- UI: Tooltip line 2 ---

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void kms$appendBrokenTooltip(
            Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!BrokenState.isBroken(self)) return;
        List<Component> lines = cir.getReturnValue();
        if (lines != null && !lines.isEmpty()) {
            lines.add(1, Component.translatable("item.keep_my_sword.broken").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
