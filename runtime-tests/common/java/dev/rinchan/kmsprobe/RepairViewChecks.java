package dev.rinchan.kmsprobe;

import dev.rinchan.keepmysword.item.SealedItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Exercise the common menu boundary on every supported game/loader target. */
public final class RepairViewChecks {
    public static void run(MinecraftServer server, ItemStack sealed) {
        var player = ProbePlayers.create(server, "CrossRepair", 926);
        var menu = new WorkMenu(player);
        player.containerMenu = menu;
        menu.setCarried(sealed.copy());
        menu.clicked(0, 0, ClickType.PICKUP, player);
        Fixtures.check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD) && menu.getCarried().isEmpty(),
                "generic restricted workbench accepts exposed sword");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        Fixtures.check(menu.getSlot(0).getItem().isEmpty() && SealedItems.isSealed(player.getInventory().getItem(0)),
                "generic shift extraction seals inventory");
        menu.clicked(1, 0, ClickType.QUICK_MOVE, player);
        Fixtures.check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD) && player.getInventory().isEmpty(),
                "generic shift insertion exposes original");
        player.doCloseContainer();
        Fixtures.check(SealedItems.isSealed(menu.getSlot(0).getItem()), "generic close seals machine slot");
        player.containerMenu = menu;
        player.tick();
        Fixtures.check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD), "generic tick exposes existing machine input");
        menu.getSlot(0).getItem().setDamageValue(100);
        player.doCloseContainer();
        Fixtures.check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD), "generic close preserves repaired item");
        player.getInventory().setItem(0, SealedItems.originalForRepair(sealed, server.overworld()));
        player.tickCount = 20;
        player.tick();
        Fixtures.check(SealedItems.isSealed(player.getInventory().getItem(0)), "inventory sweep seals escaped broken item");
        player.getInventory().clearContent();
        var dropped = player.drop(SealedItems.originalForRepair(sealed, server.overworld()), false);
        Fixtures.check(dropped != null && SealedItems.isSealed(dropped.getItem()), "player drop seals broken original before spawning");
        var anvil = new AnvilMenu(52, player.getInventory(), ContainerLevelAccess.NULL);
        player.containerMenu = anvil;
        player.experienceLevel = 30;
        anvil.setCarried(sealed.copy());
        anvil.clicked(0, 0, ClickType.PICKUP, player);
        anvil.getSlot(1).set(new ItemStack(Items.DIAMOND));
        int cost = anvil.getCost();
        anvil.clicked(2, 0, ClickType.PICKUP, player);
        Fixtures.check(anvil.getCarried().is(Items.DIAMOND_SWORD)
                && anvil.getCarried().getDamageValue() < 1561 && player.experienceLevel == 30 - cost
                && anvil.getSlot(0).getItem().isEmpty(), "open anvil uses generic view and native repair transaction");
        player.doCloseContainer();
    }

    private static final class WorkMenu extends AbstractContainerMenu {
        WorkMenu(Player player) {
            super(null, 51);
            addSlot(new Slot(new SimpleContainer(1), 0, 0, 0) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.DIAMOND_SWORD); }
            });
            for (int i = 0; i < 36; i++) addSlot(new Slot(player.getInventory(), i, 0, 0));
        }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public ItemStack quickMoveStack(Player player, int index) {
            var stack = getSlot(index).getItem();
            var before = stack.copy();
            if (!(index == 0 ? moveItemStackTo(stack, 1, 37, false) : moveItemStackTo(stack, 0, 1, false)))
                return ItemStack.EMPTY;
            return before;
        }
    }

    private RepairViewChecks() {}
}
