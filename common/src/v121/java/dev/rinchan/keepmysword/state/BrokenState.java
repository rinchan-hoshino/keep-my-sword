package dev.rinchan.keepmysword.state;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.keepmysword.config.KeepMySwordConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 1.21.1 broken-state classification, exclusion matching, and first-break feedback. */
public final class BrokenState {
    public static final TagKey<Item> EXCLUDED_TAG = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath(KeepMySword.MOD_ID, "excluded"));

    private static final Map<String, TagKey<Item>> TAG_CACHE = new ConcurrentHashMap<>();

    private BrokenState() {}

    public static boolean isElytra(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ElytraItem;
    }

    public static boolean isExcluded(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(EXCLUDED_TAG)) return true;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemId = key != null ? key.toString() : null;
        return KeepMySwordConfig.get().exclusionMatcher().matches(itemId, tagId -> isInTag(stack, tagId));
    }

    public static boolean isProtectable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.isDamageableItem()
                && stack.getMaxDamage() > 0
                && !isElytra(stack)
                && !isExcluded(stack);
    }

    public static boolean isBroken(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return DurabilityPolicy.isBroken(
                false,
                stack.isDamageableItem(),
                stack.getMaxDamage(),
                stack.getDamageValue(),
                isExcluded(stack),
                isElytra(stack));
    }

    public static void onFirstBreak(ItemStack stack, ServerLevel level, LivingEntity owner) {
        if (owner != null) {
            if (owner.isUsingItem() && owner.getUseItem() == stack) {
                owner.stopUsingItem();
            }
            if (level != null) {
                level.playSound(
                        null,
                        owner.getX(),
                        owner.getY(),
                        owner.getZ(),
                        SoundEvents.ITEM_BREAK,
                        owner.getSoundSource(),
                        0.8F,
                        0.8F + level.random.nextFloat() * 0.4F);
                level.sendParticles(
                        new ItemParticleOption(ParticleTypes.ITEM, stack.copy()),
                        owner.getX(),
                        owner.getY() + owner.getBbHeight() * 0.5,
                        owner.getZ(),
                        8,
                        0.2,
                        0.2,
                        0.2,
                        0.08);
            }
            ((EquipmentRefresh) owner).kms$refreshEquipment();
            if (owner instanceof ServerPlayer player && KeepMySwordConfig.get().showBreakNotification()) {
                player.displayClientMessage(
                        Component.translatable("message.keep_my_sword.broken", stack.getHoverName())
                                .withStyle(ChatFormatting.RED),
                        true);
            }
        }
    }

    private static boolean isInTag(ItemStack stack, String tagId) {
        TagKey<Item> tag = TAG_CACHE.computeIfAbsent(tagId, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl != null ? TagKey.create(Registries.ITEM, rl) : null;
        });
        return tag != null && stack.is(tag);
    }
}
