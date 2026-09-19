package dev.rinchan.rinlibprobe;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.component.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class RuntimeProbe {
    static void check(boolean ok, String name) {
        Fixtures.check(ok, name);
    }

    public void run(MinecraftServer s) {
        if (Files.exists(Path.of("client-visual.flag"))) return;
        s.execute(
                () -> {
                    try {
                        var ops = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                        var player = ProbePlayers.create(s, "CarrierTest", 925);
                        if (Files.exists(Path.of("unloaded.pass"))) {
                            Fixtures.verify(s);
                            var item = Fixtures.chest(s).getItem(0);
                            check(
                                    SealedItems.isSealed(item),
                                    "reinstall recognizes native carrier");
                            check(
                                    ItemStack.CODEC
                                            .encodeStart(ops, SealedItems.originalForRepair(item))
                                            .getOrThrow()
                                            .equals(
                                                    TagParser.parseTag(
                                                            Files.readString(
                                                                    Path.of("original.snbt")))),
                                    "reinstall restores exact original data");
                            MoreSafetyTests.run(
                                    s,
                                    player,
                                    item,
                                    item,
                                    SealedItems.originalForRepair(item)
                                            .get(DataComponents.CUSTOM_DATA)
                                            .copyTag());
                            PlatformStorage.verify(s);
                            Files.writeString(Path.of("reinstalled.pass"), "pass\n");
                            System.out.println("SAFETY_REINSTALL_PASS");
                            return;
                        }
                        var sword = new ItemStack(Items.DIAMOND_SWORD);
                        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
                        sword.enchant(
                                s.registryAccess()
                                        .lookupOrThrow(Registries.ENCHANTMENT)
                                        .getOrThrow(Enchantments.SHARPNESS),
                                5);
                        var data = new CompoundTag();
                        data.putString("other_mod_owner", "retained");
                        data.putLongArray("longs", new long[] {1, Long.MAX_VALUE});
                        data.putByteArray("bytes", new byte[] {1, -1});
                        data.putIntArray("ints", new int[] {1, Integer.MAX_VALUE});
                        sword.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                        int max = sword.getMaxDamage();
                        sword.setDamageValue(max - 1);
                        var expected =
                                SnapshotScope.call(
                                        () -> {
                                            var original = sword.copy();
                                            original.setDamageValue(max);
                                            return ItemStack.CODEC
                                                    .encodeStart(ops, original)
                                                    .getOrThrow();
                                        });
                        Files.writeString(Path.of("original.snbt"), expected.toString());
                        player.getInventory().setItem(0, sword);
                        sword.hurtAndBreak(1, s.overworld(), null, item -> {});
                        check(
                                sword.getCount() == 1 && player.getInventory().getItem(0) == sword,
                                "break preserves slot reference and count");
                        check(
                                SealedItems.isSealed(sword) && sword.is(Items.RECOVERY_COMPASS),
                                "broken item uses native carrier identity");
                        check(
                                !sword.isEnchanted()
                                        && !sword.has(DataComponents.CONTAINER)
                                        && !sword.has(DataComponents.BUNDLE_CONTENTS),
                                "original effects and nested item identity are not exposed");
                        check(
                                ItemStack.CODEC
                                        .encodeStart(ops, SealedItems.originalForRepair(sword))
                                        .getOrThrow()
                                        .equals(expected),
                                "snapshot preserves exact original NBT");
                        var encoded = Fixtures.encode(s, sword);
                        check(
                                !encoded.contains("sealed_original")
                                        && !encoded.contains("broken_item"),
                                "disk needs no custom item or component registry");
                        var independent =
                                IndependentStorage.CODEC.encodeStart(ops, sword).getOrThrow();
                        Files.writeString(Path.of("independent.snbt"), independent.toString());
                        var jsonOps =
                                s.registryAccess()
                                        .createSerializationContext(
                                                com.mojang.serialization.JsonOps.INSTANCE);
                        Files.writeString(
                                Path.of("independent.json"),
                                IndependentStorage.CODEC
                                        .encodeStart(jsonOps, sword)
                                        .getOrThrow()
                                        .toString());
                        var jsonCopy =
                                IndependentStorage.CODEC
                                        .parse(
                                                jsonOps,
                                                com.google.gson.JsonParser.parseString(
                                                        Files.readString(
                                                                Path.of("independent.json"))))
                                        .getOrThrow();
                        check(
                                ItemStack.CODEC
                                        .encodeStart(ops, SealedItems.originalForRepair(jsonCopy))
                                        .getOrThrow()
                                        .equals(expected),
                                "independent JSON keeps original numeric and array types");
                        var copyByApi =
                                new ItemStack(
                                        sword.getItemHolder(),
                                        sword.getCount(),
                                        sword.getComponentsPatch());
                        check(
                                Fixtures.encode(s, copyByApi).equals(encoded),
                                "holder and patch reconstruction preserves carrier");
                        var buffer =
                                new net.minecraft.network.RegistryFriendlyByteBuf(
                                        io.netty.buffer.Unpooled.buffer(), s.registryAccess());
                        try {
                            ItemStack.STREAM_CODEC.encode(buffer, sword);
                            check(
                                    Fixtures.encode(s, ItemStack.STREAM_CODEC.decode(buffer))
                                            .equals(encoded),
                                    "network roundtrip preserves payload");
                        } finally {
                            buffer.release();
                        }
                        var splitSource = sword.copy();
                        var split = splitSource.split(1);
                        check(
                                splitSource.isEmpty() && Fixtures.encode(s, split).equals(encoded),
                                "split does not lose or duplicate item");
                        var originalCopy = SealedItems.originalForRepair(sword);
                        originalCopy.setDamageValue(0);
                        check(
                                SealedItems.originalForRepair(sword).getDamageValue() == max,
                                "cached original is never exposed mutably");
                        CarrierChecks.run(s, player, sword);
                        // A purpose-built item verifies both active and passive dispatch
                        // before/after break.
                        var effect = new ItemStack(ProbeEntrypoint.EFFECT_ITEM);
                        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, effect);
                        effect.inventoryTick(s.overworld(), player, 0, true);
                        effect.use(
                                s.overworld(),
                                player,
                                net.minecraft.world.InteractionHand.MAIN_HAND);
                        effect.hurtEnemy(player, player);
                        check(
                                ProbeEffectItem.activeCalls == 1
                                        && ProbeEffectItem.passiveCalls == 1
                                        && ProbeEffectItem.attackCalls == 1,
                                "custom active passive and attack controls execute before break");
                        effect.setDamageValue(9);
                        effect.hurtAndBreak(1, s.overworld(), null, item -> {});
                        effect.inventoryTick(s.overworld(), player, 0, true);
                        effect.use(
                                s.overworld(),
                                player,
                                net.minecraft.world.InteractionHand.MAIN_HAND);
                        effect.hurtEnemy(player, player);
                        check(
                                ProbeEffectItem.activeCalls == 1
                                        && ProbeEffectItem.passiveCalls == 1
                                        && ProbeEffectItem.attackCalls == 1,
                                "custom active passive and attack effects cease after break");
                        var armor = new ItemStack(Items.DIAMOND_CHESTPLATE);
                        armor.enchant(
                                s.registryAccess()
                                        .lookupOrThrow(Registries.ENCHANTMENT)
                                        .getOrThrow(Enchantments.THORNS),
                                3);
                        armor.setDamageValue(armor.getMaxDamage());
                        armor = SealedItems.seal(armor);
                        var attrs = new AtomicInteger();
                        armor.forEachModifier(
                                EquipmentSlot.CHEST, (a, b) -> attrs.incrementAndGet());
                        check(
                                attrs.get() == 0
                                        && !armor.isEnchanted()
                                        && !(armor.getItem() instanceof ArmorItem),
                                "broken armor has no armor attributes enchantments or armor class");
                        var shield = new ItemStack(Items.SHIELD);
                        shield.setDamageValue(shield.getMaxDamage());
                        shield = SealedItems.seal(shield);
                        check(
                                shield.getUseDuration(player) == 0,
                                "sealed shield has no use duration");
                        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, shield);
                        shield.use(
                                s.overworld(),
                                player,
                                net.minecraft.world.InteractionHand.MAIN_HAND);
                        check(!player.isUsingItem(), "sealed shield cannot block");

                        MoreSafetyTests.run(s, player, sword, sword, data);
                        check(
                                player.hasEffect(
                                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),
                                "preexisting timed effects remain");
                        var box = new ItemStack(Items.SHULKER_BOX);
                        box.set(
                                DataComponents.CONTAINER,
                                ItemContainerContents.fromItems(
                                        List.of(sword.copy(), new ItemStack(Items.DIAMOND, 7))));
                        var invalid = CarrierChecks.invalid(sword, 999);
                        var big =
                                SnapshotScope.call(
                                        () -> {
                                            var item = SealedItems.originalForRepair(sword);
                                            var tag = data.copy();
                                            var noise = new byte[180000];
                                            new java.util.Random(4815).nextBytes(noise);
                                            tag.putByteArray("large_payload", noise);
                                            item.set(
                                                    DataComponents.CUSTOM_DATA, CustomData.of(tag));
                                            item.setDamageValue(item.getMaxDamage());
                                            return item;
                                        });
                        var bigExpected = ItemStack.CODEC.encodeStart(ops, big).getOrThrow();
                        big = SealedItems.seal(big);
                        check(SealedItems.isSealed(big), "large payload seals successfully");
                        var bigDisk = ItemStack.CODEC.encodeStart(ops, big).getOrThrow();
                        var output = new java.io.ByteArrayOutputStream();
                        NbtIo.writeCompressed((CompoundTag) bigDisk, output);
                        var bigCopy =
                                ItemStack.CODEC
                                        .parse(
                                                ops,
                                                NbtIo.readCompressed(
                                                        new java.io.ByteArrayInputStream(
                                                                output.toByteArray()),
                                                        NbtAccounter.create(4000000)))
                                        .getOrThrow();
                        check(
                                ItemStack.CODEC
                                        .encodeStart(ops, SealedItems.originalForRepair(bigCopy))
                                        .getOrThrow()
                                        .equals(bigExpected),
                                "large snapshot writes actual NBT without string overflow");
                        Files.writeString(Path.of("large-original.snbt"), bigExpected.toString());
                        s.overworld()
                                .setBlockAndUpdate(Fixtures.POS, Blocks.CHEST.defaultBlockState());
                        s.overworld().setChunkForced(0, 0, true);
                        var chest = (ChestBlockEntity) s.overworld().getBlockEntity(Fixtures.POS);
                        var samples = List.of(sword, shield, box, invalid, big);
                        Files.writeString(
                                Path.of("fixture-count.txt"), Integer.toString(samples.size()));
                        for (int i = 0; i < samples.size(); i++) {
                            chest.setItem(i, samples.get(i).copy());
                            Files.writeString(
                                    Path.of("expected-" + i + ".snbt"),
                                    ItemStack.CODEC
                                            .encodeStart(ops, samples.get(i))
                                            .getOrThrow()
                                            .toString());
                            Files.writeString(
                                    Path.of("expected-" + i + ".json"),
                                    Fixtures.encode(s, samples.get(i)));
                        }
                        chest.setChanged();
                        PlatformStorage.save(s, sword);
                        s.saveEverything(false, true, true);
                        Files.writeString(Path.of("prototype-saved.pass"), "pass\n");
                        System.out.println("SAFETY_SAVE_PASS");
                    } catch (Throwable e) {
                        e.printStackTrace();
                        System.out.println("SAFETY_FAIL");
                    } finally {
                        s.halt(false);
                    }
                });
    }
}
