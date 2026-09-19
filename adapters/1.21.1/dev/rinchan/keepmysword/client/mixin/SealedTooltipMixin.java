package dev.rinchan.keepmysword.client.mixin;

import dev.rinchan.keepmysword.item.SealedItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class SealedTooltipMixin {
    @Inject(method = "getTooltipLines", at = @At("HEAD"), cancellable = true)
    private void kms$originalTooltip(
            Item.TooltipContext context,
            Player player,
            TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir) {
        var stack = (ItemStack) (Object) this;
        if (!SealedItems.isSealed(stack)
                || !stack.has(dev.rinchan.keepmysword.item.SealedRegistry.ORIGINAL)) return;
        var lines =
                new ArrayList<>(
                        SealedItems.originalForSave(stack).getTooltipLines(context, player, flag));
        lines.add(
                1,
                Component.translatable("item.keep_my_sword.broken").withStyle(ChatFormatting.RED));
        cir.setReturnValue(lines);
    }
}
