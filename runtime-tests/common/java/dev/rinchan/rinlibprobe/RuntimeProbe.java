package dev.rinchan.rinlibprobe;

import dev.rinchan.keepmysword.KeepMySword;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class RuntimeProbe {
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println("KMS_ASSERT_PASS " + name);
    }

    public void run(MinecraftServer server) {
        server.execute(
                () -> {
                    try {
                        var level = server.overworld();
                        var player = ProbePlayers.create(server, "SwordTest", 21);
                        var sword = new ItemStack(Items.DIAMOND_SWORD);
                        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
                        sword.setDamageValue(sword.getMaxDamage() - 1);
                        player.getInventory().setItem(0, sword);
                        var callbacks = new AtomicInteger();
                        sword.hurtAndBreak(1, level, null, item -> callbacks.incrementAndGet());
                        check(
                                player.getInventory().getItem(0) == sword && sword.getCount() == 1,
                                "original slot and stack preserved");
                        check(
                                sword.getDamageValue() == sword.getMaxDamage(),
                                "zero durability retained");
                        check(
                                sword.getHoverName().getString().equals("Keepsake"),
                                "custom name preserved");
                        check(KeepMySword.isManagedBroken(sword), "broken state recognized");
                        check(
                                sword.getDestroySpeed(Blocks.COBWEB.defaultBlockState()) == 1f,
                                "broken sword loses mining bonus");
                        var modifiers = new AtomicInteger();
                        sword.forEachModifier(
                                EquipmentSlot.MAINHAND, (a, m) -> modifiers.incrementAndGet());
                        check(modifiers.get() == 0, "broken sword loses attributes");
                        check(callbacks.get() <= 1, "break callback not duplicated");
                        player.experienceLevel = 30;
                        level.setBlockAndUpdate(
                                new BlockPos(0, 79, 0), Blocks.STONE.defaultBlockState());
                        level.setBlockAndUpdate(
                                new BlockPos(0, 80, 0), Blocks.ANVIL.defaultBlockState());
                        var anvil =
                                new AnvilMenu(
                                        1,
                                        player.getInventory(),
                                        ContainerLevelAccess.create(level, new BlockPos(0, 80, 0)));
                        anvil.getSlot(0).set(sword.copy());
                        anvil.getSlot(1).set(new ItemStack(Items.DIAMOND));
                        anvil.setItemName("Keepsake");
                        anvil.createResult();
                        var repaired = anvil.getSlot(2).getItem();
                        check(
                                !repaired.isEmpty()
                                        && repaired.getDamageValue() < repaired.getMaxDamage(),
                                "native anvil repairs broken sword");
                        check(
                                !KeepMySword.isManagedBroken(repaired),
                                "repair restores usable state");
                        modifiers.set(0);
                        repaired.forEachModifier(
                                EquipmentSlot.MAINHAND, (a, m) -> modifiers.incrementAndGet());
                        check(modifiers.get() > 0, "repair restores sword attributes");
                        check(
                                repaired.getHoverName().getString().equals("Keepsake"),
                                "anvil retains name");
                        int cost = anvil.getCost();
                        var taken = anvil.getSlot(2).remove(1);
                        anvil.getSlot(2).onTake(player, taken);
                        check(
                                !taken.isEmpty() && !KeepMySword.isManagedBroken(taken),
                                "native anvil result can be taken");
                        check(
                                anvil.getSlot(0).getItem().isEmpty()
                                        && anvil.getSlot(1).getItem().isEmpty(),
                                "repair consumes inputs");
                        check(
                                cost > 0 && player.experienceLevel == 30 - cost,
                                "repair charges native experience cost");
                        var excluded = new ItemStack(Items.IRON_SWORD);
                        excluded.setDamageValue(excluded.getMaxDamage() - 1);
                        check(KeepMySword.isExcluded(excluded), "datapack exclusion loaded");
                        excluded.hurtAndBreak(1, level, null, item -> {});
                        check(excluded.isEmpty(), "excluded sword breaks normally");
                        var ordinary = new ItemStack(Items.DIAMOND_SWORD);
                        ordinary.hurtAndBreak(1, level, null, item -> {});
                        check(
                                ordinary.getDamageValue() == 1
                                        && !KeepMySword.isManagedBroken(ordinary),
                                "ordinary durability unchanged");
                        check(
                                !KeepMySword.isManagedBroken(new ItemStack(Items.STONE)),
                                "non-damageable items unaffected");
                        var overkill = new ItemStack(Items.DIAMOND_PICKAXE);
                        overkill.setDamageValue(overkill.getMaxDamage() - 1);
                        check(
                                !KeepMySword.isManagedBroken(overkill),
                                "last durability point remains usable");
                        overkill.hurtAndBreak(100, level, null, item -> {});
                        check(
                                overkill.getCount() == 1
                                        && overkill.getDamageValue() == overkill.getMaxDamage(),
                                "excess damage is clamped without deleting item");
                        overkill.hurtAndBreak(100, level, null, item -> {});
                        check(
                                overkill.getCount() == 1
                                        && overkill.getDamageValue() == overkill.getMaxDamage(),
                                "repeated damage retains broken item");
                        check(
                                !overkill.isCorrectToolForDrops(
                                        Blocks.DIAMOND_ORE.defaultBlockState()),
                                "broken pickaxe cannot harvest ore");
                        overkill.setDamageValue(overkill.getMaxDamage() - 1);
                        check(
                                overkill.isCorrectToolForDrops(
                                        Blocks.DIAMOND_ORE.defaultBlockState()),
                                "repair restores harvesting capability");
                        var shield = new ItemStack(Items.SHIELD);
                        shield.setDamageValue(shield.getMaxDamage());
                        check(
                                shield.getUseDuration(player) == 0,
                                "broken shield cannot sustain use");
                        check(
                                shield.getUseAnimation().name().equals("NONE"),
                                "broken shield has no use animation");
                        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, shield);
                        shield.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
                        check(!player.isUsingItem(), "broken shield does not start blocking");
                        var excludedBroken = new ItemStack(Items.IRON_SWORD);
                        excludedBroken.setDamageValue(excludedBroken.getMaxDamage());
                        check(
                                !KeepMySword.isManagedBroken(excludedBroken),
                                "excluded broken items bypass disabled behavior");
                        check(
                                !KeepMySword.isManagedBroken(ItemStack.EMPTY),
                                "empty stacks unaffected");
                        Files.writeString(Path.of("kms-runtime.pass"), "pass\n");
                        System.out.println("KMS_RUNTIME_PASS");
                    } catch (Throwable e) {
                        e.printStackTrace();
                        System.out.println("KMS_RUNTIME_FAIL");
                    } finally {
                        server.halt(false);
                    }
                });
    }
}
