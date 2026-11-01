package dev.rinchan.rinlibprobe;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

public final class TriggerChecks {
    public static void run(MinecraftServer server) throws Exception {
        var level = server.overworld();
        var player = ProbePlayers.create(server, "BreakTest", 926);
        var grace =
                net.minecraft.server.level.ServerPlayer.class.getDeclaredField(
                        "spawnInvulnerableTime");
        grace.setAccessible(true);
        grace.setInt(player, 0);
        var target = new Zombie(level);
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(sword.getMaxDamage() - 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        ((EquipmentRefresh) player).kms$refreshEquipment();
        double armed = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        player.attack(target);
        Fixtures.check(
                SealedItems.isSealed(sword)
                        && player.getMainHandItem() == sword
                        && sword.getCount() == 1,
                "actual sword attack seals the exhausted sword in its equipment slot");
        Fixtures.check(
                player.getAttributeValue(Attributes.ATTACK_DAMAGE) < armed,
                "sword attack removes equipment attributes immediately");

        var armor = new ItemStack(Items.DIAMOND_CHESTPLATE);
        armor.setDamageValue(armor.getMaxDamage() - 1);
        player.setItemSlot(EquipmentSlot.CHEST, armor);
        ((EquipmentRefresh) player).kms$refreshEquipment();
        Fixtures.check(
                player.getAttributeValue(Attributes.ARMOR) > 0,
                "armor control contributes attributes before damage");
        Fixtures.check(
                player.hurt(player.damageSources().mobAttack(target), 4.0F),
                "incoming armor attack is accepted after player load and spawn grace");
        Fixtures.check(
                SealedItems.isSealed(armor)
                        && player.getItemBySlot(EquipmentSlot.CHEST) == armor
                        && player.getAttributeValue(Attributes.ARMOR) == 0,
                "actual incoming attack seals armor and removes attributes immediately");

        var elytra = new ItemStack(Items.ELYTRA);
        elytra.setDamageValue(elytra.getMaxDamage() - 2);
        player.setItemSlot(EquipmentSlot.CHEST, elytra);
        ((EquipmentRefresh) player).kms$refreshEquipment();
        player.setOnGround(false);
        player.startFallFlying();
        var flightTicks = LivingEntity.class.getDeclaredField("fallFlyTicks");
        flightTicks.setAccessible(true);
        flightTicks.setInt(player, 19);
        var flight = LivingEntity.class.getDeclaredMethod("updateFallFlying");
        flight.setAccessible(true);
        flight.invoke(player);
        Fixtures.check(
                elytra.is(Items.ELYTRA) && elytra.getDamageValue() == elytra.getMaxDamage() - 1,
                "actual elytra flight consumes durability up to vanilla's last usable point");
        ((EquipmentRefresh) player).kms$refreshEquipment();
        flight.invoke(player);
        Fixtures.check(
                !player.isFallFlying() && elytra.is(Items.ELYTRA) && !SealedItems.isSealed(elytra),
                "vanilla elytra stops flying without destruction or sealing");
        elytra.hurtAndBreak(1, player, EquipmentSlot.CHEST);
        Fixtures.check(
                SealedItems.isSealed(elytra) && player.getItemBySlot(EquipmentSlot.CHEST) == elytra,
                "elytra seals only when hurtAndBreak reaches actual destruction");

        var mobArmor = new ItemStack(Items.DIAMOND_CHESTPLATE);
        mobArmor.setDamageValue(mobArmor.getMaxDamage() - 1);
        target.setItemSlot(EquipmentSlot.CHEST, mobArmor);
        ((EquipmentRefresh) target).kms$refreshEquipment();
        double mobArmorBefore = target.getAttributeValue(Attributes.ARMOR);
        mobArmor.hurtAndBreak(1, target, EquipmentSlot.CHEST);
        Fixtures.check(
                SealedItems.isSealed(mobArmor)
                        && target.getAttributeValue(Attributes.ARMOR) < mobArmorBefore,
                "non-player equipped break refreshes attributes immediately");

        var direct = new ItemStack(Items.DIAMOND_PICKAXE);
        direct.setDamageValue(direct.getMaxDamage() - 1);
        int[] callbacks = {0};
        direct.hurtAndBreak(1, level, null, item -> callbacks[0]++);
        Fixtures.check(
                SealedItems.isSealed(direct) && direct.getCount() == 1 && callbacks[0] == 0,
                "entityless hurtAndBreak seals without invoking destruction callback");

        var pos = new BlockPos(5, 80, 0);
        var state =
                Blocks.DISPENSER
                        .defaultBlockState()
                        .setValue(DispenserBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(pos, state);
        level.setBlockAndUpdate(
                pos.east(),
                Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 5));
        var dispenser = (DispenserBlockEntity) level.getBlockEntity(pos);
        var shears = new ItemStack(Items.SHEARS);
        shears.setDamageValue(shears.getMaxDamage() - 1);
        dispenser.setItem(0, shears);
        var dispense =
                DispenserBlock.class.getDeclaredMethod(
                        "dispenseFrom",
                        ServerLevel.class,
                        net.minecraft.world.level.block.state.BlockState.class,
                        BlockPos.class);
        dispense.setAccessible(true);
        dispense.invoke(Blocks.DISPENSER, level, state, pos);
        Fixtures.check(
                level.getBlockState(pos.east()).getValue(BeehiveBlock.HONEY_LEVEL) == 0,
                "dispenser actually shears a full bee nest");
        Fixtures.check(
                dispenser.getItem(0) == shears
                        && SealedItems.isSealed(shears)
                        && shears.getCount() == 1,
                "dispenser shears seal in their existing inventory slot");
        level.removeBlock(pos, false);
        level.removeBlock(pos.east(), false);
    }

    private TriggerChecks() {}
}
