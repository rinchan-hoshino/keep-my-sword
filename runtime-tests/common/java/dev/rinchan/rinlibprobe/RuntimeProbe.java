package dev.rinchan.rinlibprobe;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
                        if (Files.exists(Path.of("unloaded.pass"))) {
                            Fixtures.verify(s);
                            check(
                                    SealedItems.isSealed(Fixtures.chest(s).getItem(0)),
                                    "reinstall restores sealed runtime item");
                            check(
                                    Fixtures.chest(s)
                                            .getItem(3)
                                            .get(DataComponents.CUSTOM_DATA)
                                            .copyTag()
                                            .getBoolean("third_party_marker"),
                                    "reinstall restores persisted shell annotations");
                            var variantOps =
                                    s.registryAccess()
                                            .createSerializationContext(
                                                    com.mojang.serialization.JsonOps.INSTANCE);
                            var variant =
                                    net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.CODEC
                                            .parse(
                                                    variantOps,
                                                    com.google.gson.JsonParser.parseString(
                                                            Files.readString(
                                                                    Path.of(
                                                                            "transfer-variant.json"))))
                                            .getOrThrow();
                            check(
                                    SealedItems.isSealed(variant.toStack())
                                            && variant.toStack()
                                                    .get(DataComponents.CUSTOM_DATA)
                                                    .copyTag()
                                                    .getBoolean("third_party_marker"),
                                    "reinstall restores Fabric Transfer variant with shell"
                                            + " annotations");
                            var nbtOps =
                                    s.registryAccess()
                                            .createSerializationContext(
                                                    net.minecraft.nbt.NbtOps.INSTANCE);
                            var nativeVariantTag =
                                    net.minecraft.nbt.TagParser.parseTag(
                                            Files.readString(Path.of("transfer-native.snbt")));
                            var nativeVariant =
                                    net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.CODEC
                                            .parse(nbtOps, nativeVariantTag)
                                            .getOrThrow();
                            check(
                                    SealedItems.isSealed(nativeVariant.toStack()),
                                    "Fabric Transfer NBT reactivates sealed form after reinstall");
                            check(
                                    net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.CODEC
                                            .encodeStart(nbtOps, nativeVariant)
                                            .getOrThrow()
                                            .equals(nativeVariantTag),
                                    "Fabric Transfer NBT preserves exact original and annotation"
                                            + " types");
                            s.saveEverything(false, true, true);
                            Files.writeString(Path.of("reinstalled.pass"), "pass\n");
                            System.out.println("SAFETY_REINSTALL_PASS");
                            return;
                        }
                        var player = ProbePlayers.create(s, "SealedTest", 923);
                        var sword = new ItemStack(Items.DIAMOND_SWORD);
                        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
                        sword.enchant(
                                s.registryAccess()
                                        .lookupOrThrow(Registries.ENCHANTMENT)
                                        .getOrThrow(Enchantments.SHARPNESS),
                                5);
                        var data = new CompoundTag();
                        data.putString("other_mod_owner", "retained");
                        var nested = new CompoundTag();
                        nested.putInt("energy", 451);
                        nested.putLongArray("opaque", new long[] {1, Long.MAX_VALUE});
                        data.put("nested", nested);
                        sword.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                        int max = sword.getMaxDamage();
                        sword.setDamageValue(max - 1);
                        player.getInventory().setItem(0, sword);
                        sword.hurtAndBreak(1, s.overworld(), null, item -> {});
                        check(
                                sword.getCount() == 1 && player.getInventory().getItem(0) == sword,
                                "actual final durability loss preserves original slot reference");
                        check(
                                SealedItems.isSealed(sword),
                                "zero durability becomes a real inert item");
                        check(
                                sword.getItemHolder()
                                                .is(SealedRegistry.INERT.builtInRegistryHolder())
                                        && !sword.is(Items.DIAMOND_SWORD),
                                "runtime identity is consistently inert");
                        check(!sword.isEnchanted(), "original enchantments are inactive");
                        check(
                                !sword.has(DataComponents.CUSTOM_DATA),
                                "original mod data is sealed");
                        var attrs = new AtomicInteger();
                        sword.forEachModifier(
                                EquipmentSlot.MAINHAND, (a, b) -> attrs.incrementAndGet());
                        check(attrs.get() == 0, "original weapon attributes are inactive");
                        var original = SealedItems.originalForSave(sword);
                        check(
                                original.getDamageValue() == max
                                        && original.get(DataComponents.CUSTOM_DATA)
                                                .copyTag()
                                                .equals(data),
                                "original durability and opaque data preserved");
                        var encoded = Fixtures.encode(s, sword);
                        check(
                                encoded.contains("minecraft:diamond_sword")
                                        && !encoded.contains("keep_my_sword"),
                                "native disk codec stores only original item format");
                        check(
                                encoded.contains("sharpness")
                                        && encoded.contains("other_mod_owner"),
                                "disk data includes original passive and opaque components");
                        check(
                                Fixtures.encode(s, sword.copy()).equals(encoded),
                                "ordinary copy retains sealed data");
                        var copyByApi =
                                new ItemStack(
                                        sword.getItemHolder(),
                                        sword.getCount(),
                                        sword.getComponentsPatch());
                        check(
                                Fixtures.encode(s, copyByApi).equals(encoded)
                                        && SealedItems.isSealed(copyByApi),
                                "holder plus component patch reconstruction is lossless");
                        var mutated = sword.copy();
                        mutated.update(
                                DataComponents.CUSTOM_DATA,
                                CustomData.EMPTY,
                                old -> {
                                    var n = old.copyTag();
                                    n.putBoolean("third_party_marker", true);
                                    n.putLongArray("shell_longs", new long[] {1, Long.MAX_VALUE});
                                    return CustomData.of(n);
                                });
                        check(
                                SealedItems.originalForSave(mutated)
                                        .get(DataComponents.CUSTOM_DATA)
                                        .copyTag()
                                        .equals(data),
                                "outer component update cannot overwrite sealed original");
                        var repairCopy = SealedItems.originalForSave(sword);
                        repairCopy.setDamageValue(0);
                        check(
                                SealedItems.originalForSave(sword).getDamageValue() == max,
                                "original snapshot cannot be mutated through returned copy");
                        var buffer =
                                new net.minecraft.network.RegistryFriendlyByteBuf(
                                        io.netty.buffer.Unpooled.buffer(), s.registryAccess());
                        try {
                            ItemStack.STREAM_CODEC.encode(buffer, sword);
                            var synced = ItemStack.STREAM_CODEC.decode(buffer);
                            check(
                                    SealedItems.isSealed(synced)
                                            && Fixtures.encode(s, synced).equals(encoded),
                                    "network roundtrip keeps consistent sealed item");
                        } finally {
                            buffer.release();
                        }
                        var half = sword.copy();
                        var split = half.split(1);
                        check(
                                half.isEmpty() && Fixtures.encode(s, split).equals(encoded),
                                "split is lossless and count is not duplicated");
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
                        attrs.set(0);
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
                        var box = new ItemStack(Items.SHULKER_BOX);
                        box.set(
                                DataComponents.CONTAINER,
                                ItemContainerContents.fromItems(
                                        List.of(mutated.copy(), new ItemStack(Items.DIAMOND, 7))));
                        s.overworld()
                                .setBlockAndUpdate(Fixtures.POS, Blocks.CHEST.defaultBlockState());
                        s.overworld().setChunkForced(0, 0, true);
                        var chest = (ChestBlockEntity) s.overworld().getBlockEntity(Fixtures.POS);
                        var samples = List.of(sword, shield, box, mutated);
                        Files.writeString(
                                Path.of("fixture-count.txt"), Integer.toString(samples.size()));
                        for (int i = 0; i < samples.size(); i++) {
                            chest.setItem(i, samples.get(i).copy());
                            Files.writeString(
                                    Path.of("expected-" + i + ".snbt"),
                                    ItemStack.CODEC
                                            .encodeStart(
                                                    s.registryAccess()
                                                            .createSerializationContext(
                                                                    net.minecraft.nbt.NbtOps
                                                                            .INSTANCE),
                                                    samples.get(i))
                                            .getOrThrow()
                                            .toString());
                            Files.writeString(
                                    Path.of("expected-" + i + ".json"),
                                    Fixtures.encode(s, samples.get(i)));
                        }
                        chest.setChanged();
                        var anvil =
                                new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
                        player.experienceLevel = 30;
                        anvil.getSlot(0).set(sword.copy());
                        anvil.getSlot(1).set(new ItemStack(Items.DIAMOND));
                        anvil.slotsChanged(anvil.getSlot(0).container);
                        var result = anvil.getSlot(2).getItem();
                        check(
                                !result.isEmpty()
                                        && result.getItem() == Items.DIAMOND_SWORD
                                        && result.getDamageValue() < max,
                                "unsealed copy produces native anvil repair result");
                        check(
                                Fixtures.encode(s, sword).equals(encoded)
                                        && result.get(DataComponents.CUSTOM_DATA)
                                                .copyTag()
                                                .equals(data),
                                "repair preview preserves source and opaque data");
                        MoreSafetyTests.run(s, player, sword, mutated, data);
                        var observations = new ArrayList<String>();
                        check(
                                player.hasEffect(
                                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),
                                "preexisting effects are intentionally left to expire naturally");
                        var ops =
                                s.registryAccess()
                                        .createSerializationContext(
                                                com.mojang.serialization.JsonOps.INSTANCE);
                        var reload =
                                ItemStack.CODEC
                                        .parse(
                                                ops,
                                                com.google.gson.JsonParser.parseString(
                                                        Fixtures.encode(s, mutated)))
                                        .getOrThrow();
                        check(
                                reload.get(DataComponents.CUSTOM_DATA)
                                        .copyTag()
                                        .getBoolean("third_party_marker"),
                                "new outer metadata is persisted");
                        var rawSave =
                                StorageBoundary.raw()
                                        .encodeStart(ops, sword)
                                        .getOrThrow()
                                        .toString();
                        if (rawSave.contains("broken_item"))
                            observations.add(
                                    "ALTERNATE_SERIALIZER_RISK: serialization bypassing native"
                                            + " boundary persists custom wrapper identity");
                        Files.writeString(
                                Path.of("open-issues.txt"), String.join("\n", observations) + "\n");
                        for (var f : observations) System.out.println("SAFETY_OPEN " + f);
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
