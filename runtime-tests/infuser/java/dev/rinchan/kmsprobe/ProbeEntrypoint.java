package dev.rinchan.kmsprobe;

import dev.rinchan.keepmysword.item.SealedItems;

import fuzs.enchantinginfuser.world.inventory.InfuserMenu;
import fuzs.enchantinginfuser.world.level.block.InfuserType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod("kms_probe")
public final class ProbeEntrypoint {
    public ProbeEntrypoint() {
        NeoForge.EVENT_BUS.addListener(
                (ServerStartedEvent e) -> e.getServer().execute(() -> run(e.getServer())));
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        System.out.println("INFUSER_PASS " + label);
    }

    private static void run(MinecraftServer server) {
        try {
            boolean baseline = Boolean.getBoolean("infuser.baseline");
            check(
                    ModList.get().isLoaded("keep_my_sword") != baseline,
                    "requested mod combination loaded");
            if (baseline)
                check(
                        !ModList.get().isLoaded("rinlib"),
                        "baseline loads only Infuser and its dependencies");
            var player = ProbePlayers.create(server, "InfuserTest", 719);
            player.experienceLevel = 30;
            var sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.set(DataComponents.CUSTOM_NAME, Component.literal("Infuser keepsake"));
            sword.enchant(
                    server.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.SHARPNESS),
                    3);
            sword.set(DataComponents.REPAIR_COST, 3);
            var data = new CompoundTag();
            data.putString("other_mod_owner", "retained");
            sword.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            var expected = sword.copy();
            expected.setDamageValue(0);
            sword.setDamageValue(1560);
            if (baseline) sword.setDamageValue(1561);
            else {
                player.getInventory().setItem(0, sword);
                sword.hurtAndBreak(1, server.overworld(), null, ignored -> {});
                check(
                        SealedItems.isSealed(sword)
                                && player.getInventory().getItem(0) == sword
                                && sword.getCount() == 1,
                        "breaking preserves sealed sword and slot");
                check(
                        !sword.isDamageableItem() && !sword.isEnchanted(),
                        "sealed input has no active original components");
            }
            var input = encode(server, sword);
            player.getInventory().setItem(0, ItemStack.EMPTY);
            Files.writeString(
                    Path.of("exhausted-sword.snbt"),
                    encode(
                            server,
                            baseline
                                    ? sword
                                    : SealedItems.originalForRepair(sword, server.overworld())));
            var container = new SimpleContainer(1);
            var menu = menu(server, player, container, InfuserType.ADVANCED);
            container.setItem(0, sword.copy());
            if (!baseline) {
                player.containerMenu = menu;
                player.tick();
            }
            menu.slotsChanged(container);
            var expectedInput = expected.copy();
            expectedInput.setDamageValue(1561);
            var preview = encode(server, expectedInput);
            System.out.println(
                    "INFUSER_STATE baseline="
                            + baseline
                            + " item="
                            + sword.getItem()
                            + " cost="
                            + menu.getRepairCost()
                            + " canRepair="
                            + menu.canRepair(player));
            check(menu.canRepair(player), "advanced infuser accepts exhausted sword");
            check(menu.getRepairCost() == 8, "full durability repair costs eight levels");
            check(
                    encode(server, container.getItem(0)).equals(preview),
                    "preview preserves exact exposed input");
            player.experienceLevel = 7;
            check(
                    !menu.canRepair(player)
                            && !menu.clickMenuButton(player, InfuserMenu.REPAIR_BUTTON),
                    "insufficient levels cannot repair");
            check(
                    player.experienceLevel == 7
                            && encode(server, container.getItem(0)).equals(preview),
                    "rejected repair preserves levels and input");
            player.experienceLevel = 30;
            check(
                    menu.clickMenuButton(player, InfuserMenu.REPAIR_BUTTON),
                    "repair button transaction succeeds");
            check(
                    player.experienceLevel == 22
                            && encode(server, container.getItem(0))
                                    .equals(encode(server, expected)),
                    "repair restores exact sword data and charges eight levels");
            check(
                    player.getInventory().isEmpty() && container.getItem(0).getCount() == 1,
                    "repair returns one item without inventory duplication");
            if (!baseline) extraChecks(server, player, sword, input);
            Files.writeString(Path.of("infuser.pass"), "pass\n");
            System.out.println("INFUSER_ALL_PASS");
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.out.println("INFUSER_FAIL");
        } finally {
            server.halt(false);
        }
    }

    private static InfuserMenu menu(
            MinecraftServer server,
            ServerPlayer player,
            SimpleContainer container,
            InfuserType type) {
        return new InfuserMenu(
                type,
                1,
                player.getInventory(),
                container,
                ContainerLevelAccess.create(server.overworld(), new BlockPos(0, 80, 0)));
    }

    private static String encode(MinecraftServer server, ItemStack stack) {
        return ItemStack.CODEC
                .encodeStart(
                        server.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack)
                .getOrThrow()
                .toString();
    }

    private static void extraChecks(
            MinecraftServer server, ServerPlayer player, ItemStack sealed, String input) {
        var container = new SimpleContainer(1);
        container.setItem(0, sealed.copy());
        var menu = menu(server, player, container, InfuserType.ADVANCED);
        menu.slotsChanged(container);
        var sharpness =
                server.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SHARPNESS);
        menu.clickEnchantmentLevelButton(sharpness, level -> 0);
        check(
                !menu.canEnchant(player)
                        && !menu.clickMenuButton(player, InfuserMenu.ENCHANT_BUTTON),
                "enchanting cannot reactivate a sealed sword");
        check(
                encode(server, container.getItem(0)).equals(input),
                "enchant attempt preserves sealed snapshot");
        player.containerMenu = menu;
        player.tick();
        menu.slotsChanged(container);
        var config = menu.getConfig();
        config.increaseAnvilRepairCost = true;
        try {
            player.experienceLevel = 30;
            check(
                    menu.clickMenuButton(player, InfuserMenu.REPAIR_BUTTON),
                    "repair with prior work penalty succeeds");
            check(
                    container.getItem(0).getOrDefault(DataComponents.REPAIR_COST, 0) == 7,
                    "infuser retains configured anvil penalty");
        } finally {
            config.increaseAnvilRepairCost = false;
        }
        container.setItem(0, sealed.copy());
        var normal = menu(server, player, container, InfuserType.NORMAL);
        player.containerMenu = normal;
        player.tick();
        normal.slotsChanged(container);
        check(
                !normal.canRepair(player)
                        && !normal.clickMenuButton(player, InfuserMenu.REPAIR_BUTTON),
                "normal infuser keeps repairs disabled");
        var invalid = sealed.copy();
        var tag = invalid.get(DataComponents.CUSTOM_DATA).copyTag();
        tag.getCompound("keep_my_sword:sealed").putInt("DataVersion", Integer.MAX_VALUE);
        invalid.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        var invalidBefore = encode(server, invalid);
        container.setItem(0, invalid);
        menu.slotsChanged(container);
        int levels = player.experienceLevel;
        check(
                !menu.canRepair(player) && !menu.clickMenuButton(player, InfuserMenu.REPAIR_BUTTON),
                "unknown snapshot cannot be repaired");
        check(
                player.experienceLevel == levels
                        && encode(server, container.getItem(0)).equals(invalidBefore),
                "unknown snapshot and levels survive rejected repair");
        var damaged = new ItemStack(Items.DIAMOND_SWORD);
        damaged.setDamageValue(100);
        container.setItem(0, damaged);
        menu.slotsChanged(container);
        player.experienceLevel = 30;
        check(
                menu.getRepairCost() == 2
                        && menu.clickMenuButton(player, InfuserMenu.REPAIR_BUTTON)
                        && container.getItem(0).getDamageValue() == 0
                        && player.experienceLevel == 28,
                "ordinary damaged sword repair is unchanged");
    }
}
