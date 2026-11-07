package dev.rinchan.kmsprobe;

import dev.rinchan.keepmysword.item.SealedItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.nio.file.*;

public final class RepairViewProbe {
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println("REPAIR_VIEW_PASS " + name);
    }

    public static void run(MinecraftServer server) {
        try {
            var player = ProbePlayers.create(server, "RepairView", 720);
            var sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponents.CUSTOM_NAME, Component.literal("Generic keepsake"));
            sword.set(DataComponents.REPAIR_COST, 3);
            var custom = new net.minecraft.nbt.CompoundTag();
            custom.putString("other_mod_owner", "retained");
            sword.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(custom));
            sword.enchant(server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 3);
            sword.setDamageValue(sword.getMaxDamage() - 1);
            sword.hurtAndBreak(1, server.overworld(), null, ignored -> {});
            check(SealedItems.isSealed(sword), "real break creates sealed input");
            var menu = new WorkMenu(player);
            player.containerMenu = menu;
            menu.setCarried(sword.copy());
            menu.clicked(0, 0, ClickType.PICKUP, player);
            check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD)
                    && menu.getSlot(0).getItem().getDamageValue() == 1561
                    && menu.getCarried().isEmpty(),
                    "unmodified restricted workbench accepts sealed sword as exact exhausted original");
            var original = SealedItems.originalForRepair(sword, server.overworld());
            check(ItemStack.isSameItemSameComponents(menu.getSlot(0).getItem(), original),
                    "exposure preserves every original component");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            check(SealedItems.isSealed(menu.getCarried()), "pickup reseals broken cursor immediately");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
            check(menu.getSlot(0).getItem().isEmpty()
                    && SealedItems.isSealed(player.getInventory().getItem(0)), "shift extraction reseals inventory");
            menu.clicked(1, 0, ClickType.QUICK_MOVE, player);
            check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD)
                    && player.getInventory().isEmpty(), "shift insertion passes native item restrictions");
            menu.clicked(0, 0, ClickType.SWAP, player);
            check(SealedItems.isSealed(player.getInventory().getItem(0)) && menu.getSlot(0).getItem().isEmpty(),
                    "hotbar swap extraction reseals broken sword");
            menu.clicked(0, 0, ClickType.SWAP, player);
            check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD) && player.getInventory().getItem(0).isEmpty(),
                    "hotbar swap insertion exposes original");
            menu.clicked(0, 0, ClickType.PICKUP, player);
            menu.clicked(-999, 0, ClickType.QUICK_CRAFT, player);
            menu.clicked(0, 1, ClickType.QUICK_CRAFT, player);
            menu.clicked(-999, 2, ClickType.QUICK_CRAFT, player);
            check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD) && menu.getCarried().isEmpty(),
                    "drag placement passes restricted workbench slot");
            player.doCloseContainer();
            check(SealedItems.isSealed(menu.getSlot(0).getItem()), "closing persistent workbench reseals input");
            player.containerMenu = menu;
            player.tick();
            check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD), "tick exposes preexisting workbench input");
            menu.getSlot(0).getItem().setDamageValue(100);
            player.doCloseContainer();
            check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD)
                    && menu.getSlot(0).getItem().getDamageValue() == 100, "closing preserves partially repaired sword");
            player.containerMenu = menu;
            menu.getSlot(0).set(sword.copy());
            player.tick();
            menu.clicked(0, 0, ClickType.THROW, player);
            var drops = server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    player.getBoundingBox().inflate(10));
            check(drops.stream().anyMatch(e -> SealedItems.isSealed(e.getItem())), "throwing from workbench drops sealed item");
            player.doCloseContainer();
            player.getInventory().setItem(0, original.copy());
            player.tickCount = 20;
            player.tick();
            check(SealedItems.isSealed(player.getInventory().getItem(0)), "one-second inventory sweep seals escaped broken original");
            player.getInventory().clearContent();
            storageChecks(server, player, sword);
            vanillaAnvil(server, player, sword);
            infuser(server, player, sword);
            repairStation(server, player, sword);
            var victim = ProbePlayers.create(server, "RepairDeath", 721);
            victim.getInventory().setItem(0, original.copy());
            var deathMenu = new WorkMenu(victim);
            victim.containerMenu = deathMenu;
            deathMenu.getSlot(0).set(original.copy());
            var priorDrops = server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    victim.getBoundingBox().inflate(10)).stream().map(e -> e.getUUID()).collect(java.util.stream.Collectors.toSet());
            victim.die(server.overworld().damageSources().generic());
            var deathDrops = server.overworld().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    victim.getBoundingBox().inflate(10));
            check(victim.getInventory().isEmpty() && deathDrops.stream().anyMatch(e -> !priorDrops.contains(e.getUUID()) && SealedItems.isSealed(e.getItem()))
                    && SealedItems.isSealed(deathMenu.getSlot(0).getItem()),
                    "actual player death drops broken originals as sealed items");
            Files.writeString(Path.of("repair-view.pass"), "pass\n");
            System.out.println("REPAIR_VIEW_ALL_PASS");
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.out.println("REPAIR_VIEW_FAIL");
        } finally { server.halt(false); }
    }

    private static void storageChecks(MinecraftServer server, net.minecraft.server.level.ServerPlayer player, ItemStack sealed) {
        var menus = java.util.List.of(
                ChestMenu.oneRow(2, player.getInventory()),
                new HopperMenu(3, player.getInventory()),
                new DispenserMenu(4, player.getInventory()),
                new ShulkerBoxMenu(5, player.getInventory()));
        for (var storage : menus) {
            player.containerMenu = storage;
            storage.setCarried(sealed.copy());
            storage.clicked(0, 0, ClickType.PICKUP, player);
            player.tick();
            check(SealedItems.isSealed(storage.getSlot(0).getItem()),
                    storage.getClass().getSimpleName() + " keeps sealed carrier in storage");
            player.doCloseContainer();
        }
        var pos = new net.minecraft.core.BlockPos(6, 80, 0);
        server.overworld().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        var container = (net.minecraft.world.Container) server.overworld().getBlockEntity(pos);
        var custom = new AbstractContainerMenu(null, 6) {
            { addSlot(new Slot(container, 0, 0, 0)); addSlot(new Slot(player.getInventory(), 0, 0, 0)); }
            public boolean stillValid(Player p) { return true; }
            public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
        };
        player.containerMenu = custom;
        custom.getSlot(0).set(sealed.copy());
        player.tick();
        check(SealedItems.isSealed(custom.getSlot(0).getItem()), "unlisted ordinary-slot block storage is auto excluded");
        player.doCloseContainer();
    }

    private static void vanillaAnvil(MinecraftServer server, net.minecraft.server.level.ServerPlayer player, ItemStack sealed) {
        var menu = new AnvilMenu(7, player.getInventory(), ContainerLevelAccess.NULL);
        player.containerMenu = menu;
        player.experienceLevel = 30;
        menu.setCarried(sealed.copy());
        menu.clicked(0, 0, ClickType.PICKUP, player);
        menu.getSlot(1).set(new ItemStack(Items.DIAMOND));
        check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD)
                && menu.getSlot(2).getItem().is(Items.DIAMOND_SWORD), "vanilla anvil calculates against exposed original");
        int cost = menu.getCost();
        menu.clicked(2, 0, ClickType.PICKUP, player);
        check(menu.getCarried().is(Items.DIAMOND_SWORD) && menu.getCarried().getDamageValue() < 1561
                && menu.getSlot(0).getItem().isEmpty() && player.experienceLevel == 30 - cost,
                "vanilla anvil repairs one sword and charges exact XP");
        menu.setCarried(ItemStack.EMPTY);
        player.doCloseContainer();
    }

    private static void infuser(MinecraftServer server, net.minecraft.server.level.ServerPlayer player, ItemStack sealed) {
        var input = new SimpleContainer(1);
        var menu = new fuzs.enchantinginfuser.world.inventory.InfuserMenu(
                fuzs.enchantinginfuser.world.level.block.InfuserType.ADVANCED, 8, player.getInventory(), input,
                ContainerLevelAccess.create(server.overworld(), new net.minecraft.core.BlockPos(0, 80, 0)));
        player.containerMenu = menu;
        player.experienceLevel = 30;
        menu.setCarried(sealed.copy());
        menu.clicked(0, 0, ClickType.PICKUP, player);
        menu.slotsChanged(input);
        check(input.getItem(0).is(Items.DIAMOND_SWORD) && menu.getRepairCost() == 8
                && menu.clickMenuButton(player, fuzs.enchantinginfuser.world.inventory.InfuserMenu.REPAIR_BUTTON)
                && input.getItem(0).getDamageValue() == 0 && player.experienceLevel == 22,
                "Advanced Enchanting Infuser repairs exposed sword for eight levels");
        var repaired = input.getItem(0).copy();
        var expected = SealedItems.originalForRepair(sealed, server.overworld());
        expected.setDamageValue(0);
        check(ItemStack.isSameItemSameComponents(repaired, expected), "Infuser repair preserves full original metadata");
        player.doCloseContainer();
        player.getInventory().clearContent();
        player.containerMenu = menu;
        input.setItem(0, sealed.copy());
        player.tick();
        menu.slotsChanged(input);
        var sharpness = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
        menu.clickEnchantmentLevelButton(sharpness, level -> 0);
        check(menu.canEnchant(player) && menu.clickMenuButton(player, fuzs.enchantinginfuser.world.inventory.InfuserMenu.ENCHANT_BUTTON)
                && input.getItem(0).getDamageValue() == 1561 && !input.getItem(0).isEnchanted(),
                "exposed broken sword follows native Infuser enchantment changes");
        player.doCloseContainer();
        var returned = input.getItem(0);
        check(SealedItems.isSealed(returned)
                && !SealedItems.originalForRepair(returned, server.overworld()).isEnchanted()
                && SealedItems.originalForRepair(returned, server.overworld()).getDamageValue() == 1561,
                "closing after enchanting reseals broken sword and keeps changed enchantments");
        player.getInventory().clearContent();
        var unknown = sealed.copy();
        var tag = unknown.get(DataComponents.CUSTOM_DATA).copyTag();
        tag.getCompound("keep_my_sword:sealed").putInt("DataVersion", Integer.MAX_VALUE);
        unknown.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        var before = unknown.copy();
        player.containerMenu = menu;
        menu.setCarried(unknown);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        player.tick();
        menu.slotsChanged(input);
        check(!menu.canRepair(player) && !menu.canEnchant(player)
                && ItemStack.isSameItemSameComponents(input.getItem(0), before),
                "unknown snapshot stays sealed and cannot be repaired or enchanted");
        player.doCloseContainer();
        check(ItemStack.isSameItemSameComponents(input.getItem(0), before),
                "unknown snapshot survives close byte-for-byte in native components");
        player.getInventory().clearContent();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void repairStation(MinecraftServer server, net.minecraft.server.level.ServerPlayer player, ItemStack sealed) {
        var level = server.overworld();
        var pos = new net.minecraft.core.BlockPos(8, 80, 0);
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.parse("repairstation:repair_station"));
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var entity = level.getBlockEntity(pos);
        check(entity instanceof net.minecraft.world.MenuProvider, "third-party Repair Station block entity loaded");
        var menu = ((net.minecraft.world.MenuProvider) entity).createMenu(9, player.getInventory(), player);
        player.containerMenu = menu;
        menu.setCarried(sealed.copy());
        menu.clicked(0, 0, ClickType.PICKUP, player);
        check(menu.getSlot(0).getItem().is(Items.DIAMOND_SWORD), "Repair Station restricted slot accepts exposed original");
        menu.getSlot(1).set(new ItemStack(Items.FLINT, 64));
        var ticker = ((net.minecraft.world.level.block.EntityBlock) block).getTicker(level, block.defaultBlockState(), entity.getType());
        for (int i = 0; i < 40000 && menu.getSlot(2).getItem().isEmpty(); i++)
            ((net.minecraft.world.level.block.entity.BlockEntityTicker) ticker).tick(level, pos, level.getBlockState(pos), entity);
        check(menu.getSlot(2).getItem().is(Items.DIAMOND_SWORD) && menu.getSlot(2).getItem().getDamageValue() == 0
                && menu.getSlot(2).getItem().getCount() == 1
                && menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().getCount() < 64,
                "Repair Station performs native fueled repair and produces one healthy sword");
        var expected = SealedItems.originalForRepair(sealed, level);
        expected.setDamageValue(0);
        check(ItemStack.isSameItemSameComponents(menu.getSlot(2).getItem(), expected),
                "Repair Station retains full original metadata in repaired output");
        menu.clicked(2, 0, ClickType.PICKUP, player);
        check(menu.getCarried().is(Items.DIAMOND_SWORD), "repaired third-party output remains original on pickup");
        menu.setCarried(sealed.copy());
        menu.clicked(0, 0, ClickType.PICKUP, player);
        player.doCloseContainer();
        check(SealedItems.isSealed(menu.getSlot(0).getItem()), "closing Repair Station reseals unrepaired machine input");
    }

    static final class WorkMenu extends AbstractContainerMenu {
        WorkMenu(Player player) {
            super(null, 1);
            addSlot(new Slot(new SimpleContainer(1), 0, 0, 0) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.DIAMOND_SWORD); }
            });
            for (int i = 0; i < 36; i++) addSlot(new Slot(player.getInventory(), i, 0, 0));
        }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public ItemStack quickMoveStack(Player player, int index) {
            var source = getSlot(index);
            var stack = source.getItem();
            var before = stack.copy();
            if (!(index == 0 ? moveItemStackTo(stack, 1, 37, false) : moveItemStackTo(stack, 0, 1, false)))
                return ItemStack.EMPTY;
            source.setChanged();
            return before;
        }
    }
}
