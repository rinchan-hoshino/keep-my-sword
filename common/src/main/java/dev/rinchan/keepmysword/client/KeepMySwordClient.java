package dev.rinchan.keepmysword.client;

import dev.rinchan.keepmysword.KeepMySword;
import dev.rinchan.keepmysword.item.SealedItems;
import dev.rinchan.rinlib.v2.ClientEvents;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class KeepMySwordClient {
    private KeepMySwordClient() {}

    public static void initialize() {
        ClientEvents.TOOLTIP.register((stack, context, player, flag) -> {
            if (!SealedItems.isSealed(stack)) return null;
            var level = Minecraft.getInstance().level;
            if (level == null) return null;
            var original = SealedItems.originalForRepair(stack, level);
            if (original.isEmpty()) return null;
            var lines = new ArrayList<>(original.getTooltipLines(context, player, flag));
            lines.add(1, Component.translatable("item.keep_my_sword.broken").withStyle(ChatFormatting.RED));
            return lines;
        });
        ClientEvents.RENDER_STACK.register(s -> {
            var level = Minecraft.getInstance().level;
            return level == null ? s : SealedItems.visualCopy(s, level);
        });
        ClientEvents.SLOT_OVERLAY.register(s -> KeepMySword.isManagedBroken(s) ? BrokenVisualStyle.SLOT_OVERLAY_ARGB : 0);
    }
}
