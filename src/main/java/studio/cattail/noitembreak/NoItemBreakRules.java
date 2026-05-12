package studio.cattail.noitembreak;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

/** Runtime rules for zero-durability items. */
public final class NoItemBreakRules {
    private static final Component BROKEN_TOOLTIP = Component.literal("已损坏：修理后才能再次使用");

    private NoItemBreakRules() {
    }

    public static boolean isBroken(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && stack.getMaxDamage() > 0 && stack.getDamageValue() >= stack.getMaxDamage();
    }

    @SubscribeEvent
    public static void clearBrokenItemAttributes(ItemAttributeModifierEvent event) {
        if (isBroken(event.getItemStack())) {
            event.clearModifiers();
        }
    }

    @SubscribeEvent
    public static void stopBrokenToolMining(PlayerEvent.BreakSpeed event) {
        if (isBroken(event.getEntity().getMainHandItem())) {
            event.setNewSpeed(0.0F);
        }
    }

    @SubscribeEvent
    public static void stopBrokenToolHarvest(PlayerEvent.HarvestCheck event) {
        if (isBroken(event.getEntity().getMainHandItem())) {
            event.setCanHarvest(false);
        }
    }

    @SubscribeEvent
    public static void stopBrokenLeftClickItemUse(PlayerInteractEvent.LeftClickBlock event) {
        if (isBroken(event.getItemStack())) {
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void stopBrokenRightClickItemUseOnBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isBroken(event.getItemStack())) {
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void stopBrokenUseOnBlockPhase(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK && isBroken(event.getItemStack())) {
            event.cancelWithResult(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
        }
    }

    @SubscribeEvent
    public static void stopBrokenRightClickItemUse(PlayerInteractEvent.RightClickItem event) {
        if (isBroken(event.getItemStack())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void stopBrokenContinuousItemUse(LivingEntityUseItemEvent.Start event) {
        if (isBroken(event.getItem())) {
            event.setCanceled(true);
            event.setDuration(0);
        }
    }

    @SubscribeEvent
    public static void addBrokenTooltip(ItemTooltipEvent event) {
        if (isBroken(event.getItemStack())) {
            event.getToolTip().add(BROKEN_TOOLTIP);
        }
    }
}
