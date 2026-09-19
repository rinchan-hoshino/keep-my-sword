package dev.rinchan.rinlibprobe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
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

/** Research only: tests vanilla carrier persistence with Keep My Sword completely absent. */
public final class RuntimeProbe {
    static final BlockPos POS = new BlockPos(0, 80, 0);
    static final Codec<ItemStack> INDEPENDENT =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            BuiltInRegistries.ITEM
                                                    .byNameCodec()
                                                    .fieldOf("type")
                                                    .forGetter(ItemStack::getItem),
                                            Codec.INT
                                                    .fieldOf("amount")
                                                    .forGetter(ItemStack::getCount),
                                            DataComponentPatch.CODEC
                                                    .fieldOf("patch")
                                                    .forGetter(ItemStack::getComponentsPatch))
                                    .apply(
                                            i,
                                            (item, count, patch) ->
                                                    new ItemStack(
                                                            item.builtInRegistryHolder(),
                                                            count,
                                                            patch)));

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        System.out.println("CARRIER_PASS " + message);
    }

    static ItemStack wrap(ItemStack original) {
        var stack = new ItemStack(Items.BUNDLE);
        stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(original.copy())));
        return stack;
    }

    static ItemStack opaque(ItemStack original, net.minecraft.resources.RegistryOps<Tag> ops) {
        var carrier = new ItemStack(Items.PAPER);
        var envelope = new CompoundTag();
        envelope.putInt("schema", 1);
        envelope.putInt(
                "data_version",
                net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        envelope.putString(
                "original_snbt",
                ItemStack.CODEC.encodeStart(ops, original).getOrThrow().toString());
        var data = new CompoundTag();
        data.put("keep_my_sword:sealed", envelope);
        carrier.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        carrier.set(DataComponents.MAX_STACK_SIZE, 1);
        return carrier;
    }

    static ItemStack unpackOpaque(ItemStack carrier, net.minecraft.resources.RegistryOps<Tag> ops)
            throws Exception {
        var data =
                carrier.get(DataComponents.CUSTOM_DATA)
                        .copyTag()
                        .getCompound("keep_my_sword:sealed");
        var original = TagParser.parseTag(data.getString("original_snbt"));
        return ItemStack.CODEC.parse(ops, original).getOrThrow();
    }

    public void run(MinecraftServer s) {
        s.execute(
                () -> {
                    try {
                        check(
                                !net.fabricmc.loader.api.FabricLoader.getInstance()
                                        .isModLoaded("keep_my_sword"),
                                "Keep My Sword not loaded");
                        try {
                            Class.forName("dev.rinchan.keepmysword.item.SealedItems");
                            throw new AssertionError("Keep My Sword classes present");
                        } catch (ClassNotFoundException expected) {
                        }
                        check(true, "Keep My Sword classes absent");
                        var ops = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                        var player = ProbePlayers.create(s, "CarrierTest", 924);
                        s.overworld().getChunkAt(POS);
                        boolean reopen = Files.exists(Path.of("save.pass"));
                        if (!reopen) {
                            s.overworld().setBlockAndUpdate(POS, Blocks.CHEST.defaultBlockState());
                            var chest = (ChestBlockEntity) s.overworld().getBlockEntity(POS);
                            var original = new ItemStack(Items.DIAMOND_SWORD);
                            original.setDamageValue(original.getMaxDamage());
                            original.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
                            original.enchant(
                                    s.registryAccess()
                                            .lookupOrThrow(Registries.ENCHANTMENT)
                                            .getOrThrow(Enchantments.SHARPNESS),
                                    5);
                            var data = new CompoundTag();
                            data.putByte("byte", (byte) 7);
                            data.putLongArray("longs", new long[] {1, Long.MAX_VALUE});
                            data.putIntArray("ints", new int[] {2, Integer.MAX_VALUE});
                            data.putByteArray("bytes", new byte[] {3, -1});
                            data.putString("other_mod_owner", "retained");
                            original.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                            Files.writeString(
                                    Path.of("original.snbt"),
                                    ItemStack.CODEC
                                            .encodeStart(ops, original)
                                            .getOrThrow()
                                            .toString());
                            var carrier = wrap(original);
                            check(
                                    carrier.isItemEnabled(s.overworld().enabledFeatures()),
                                    "bundle enabled in normal world");
                            check(
                                    !carrier.is(Items.DIAMOND_SWORD)
                                            && !carrier.isEnchanted()
                                            && !carrier.has(DataComponents.CUSTOM_DATA),
                                    "original identity and gameplay components absent on carrier");
                            var attributes = new AtomicInteger();
                            carrier.forEachModifier(
                                    EquipmentSlot.MAINHAND, (a, b) -> attributes.incrementAndGet());
                            check(attributes.get() == 0, "original attack attributes absent");
                            var effect = new ItemStack(ProbeEntrypoint.EFFECT_ITEM);
                            effect.setDamageValue(effect.getMaxDamage());
                            effect.inventoryTick(s.overworld(), player, 0, true);
                            check(
                                    ProbeEffectItem.passiveCalls == 1,
                                    "custom original callback control runs");
                            wrap(effect).inventoryTick(s.overworld(), player, 0, true);
                            check(
                                    ProbeEffectItem.passiveCalls == 1,
                                    "vanilla carrier does not tick original callback");
                            var hidden = opaque(effect, ops);
                            hidden.inventoryTick(s.overworld(), player, 0, true);
                            check(
                                    ProbeEffectItem.passiveCalls == 1,
                                    "opaque carrier does not tick original callback");
                            check(
                                    !hidden.has(DataComponents.CONTAINER)
                                            && !hidden.has(DataComponents.BUNDLE_CONTENTS),
                                    "opaque carrier exposes no nested item container");
                            check(
                                    !hidden.is(ProbeEntrypoint.EFFECT_ITEM),
                                    "direct identity scanner cannot see original in opaque"
                                        + " carrier");
                            var visible =
                                    new BundleContents.Mutable(
                                                    wrap(effect)
                                                            .get(DataComponents.BUNDLE_CONTENTS))
                                            .removeOne();
                            check(
                                    visible.is(ProbeEntrypoint.EFFECT_ITEM),
                                    "counterexample: recursive scanner can identify original inside"
                                        + " bundle");
                            var paper = opaque(original, ops);
                            Files.writeString(
                                    Path.of("opaque-independent.snbt"),
                                    INDEPENDENT.encodeStart(ops, paper).getOrThrow().toString());
                            var jsonOps =
                                    s.registryAccess()
                                            .createSerializationContext(
                                                    com.mojang.serialization.JsonOps.INSTANCE);
                            Files.writeString(
                                    Path.of("opaque-independent.json"),
                                    INDEPENDENT
                                            .encodeStart(jsonOps, paper)
                                            .getOrThrow()
                                            .toString());
                            Files.writeString(
                                    Path.of("independent.snbt"),
                                    INDEPENDENT.encodeStart(ops, carrier).getOrThrow().toString());
                            chest.setItem(0, carrier);
                            var shulker = new ItemStack(Items.SHULKER_BOX);
                            shulker.set(
                                    DataComponents.CONTAINER,
                                    ItemContainerContents.fromItems(List.of(carrier.copy())));
                            chest.setItem(1, shulker);
                            chest.setItem(2, paper);
                            chest.setChanged();
                            s.saveEverything(false, true, true);
                            Files.writeString(Path.of("save.pass"), "pass\n");
                        } else {
                            var chest = (ChestBlockEntity) s.overworld().getBlockEntity(POS);
                            var expected =
                                    TagParser.parseTag(Files.readString(Path.of("original.snbt")));
                            var paperNbt =
                                    INDEPENDENT
                                            .parse(
                                                    ops,
                                                    TagParser.parseTag(
                                                            Files.readString(
                                                                    Path.of(
                                                                            "opaque-independent.snbt"))))
                                            .getOrThrow();
                            var paperJson =
                                    INDEPENDENT
                                            .parse(
                                                    s.registryAccess()
                                                            .createSerializationContext(
                                                                    com.mojang.serialization.JsonOps
                                                                            .INSTANCE),
                                                    com.google.gson.JsonParser.parseString(
                                                            Files.readString(
                                                                    Path.of(
                                                                            "opaque-independent.json"))))
                                            .getOrThrow();
                            for (var paper : List.of(chest.getItem(2), paperNbt, paperJson)) {
                                check(
                                        paper.is(Items.PAPER),
                                        "opaque carrier survives without custom registry identity");
                                var restored = unpackOpaque(paper, ops);
                                check(
                                        ItemStack.CODEC
                                                .encodeStart(ops, restored)
                                                .getOrThrow()
                                                .equals(expected),
                                        "opaque snapshot restores exact original NBT after restart"
                                            + " or independent serialization");
                                restored.setDamageValue(0);
                                check(
                                        restored.is(Items.DIAMOND_SWORD)
                                                && restored.getDamageValue() == 0
                                                && restored.isEnchanted(),
                                        "recovered original can be repaired without losing"
                                            + " enchantments");
                            }
                            var independent =
                                    INDEPENDENT
                                            .parse(
                                                    ops,
                                                    TagParser.parseTag(
                                                            Files.readString(
                                                                    Path.of("independent.snbt"))))
                                            .getOrThrow();
                            var nested =
                                    chest.getItem(1)
                                            .get(DataComponents.CONTAINER)
                                            .nonEmptyItemCopyStream()
                                            .findFirst()
                                            .orElseThrow();
                            for (var carrier : List.of(chest.getItem(0), independent, nested)) {
                                check(
                                        carrier.is(Items.BUNDLE),
                                        "carrier survives save and restart");
                                var contents =
                                        new BundleContents.Mutable(
                                                carrier.get(DataComponents.BUNDLE_CONTENTS));
                                var original = contents.removeOne();
                                check(
                                        ItemStack.CODEC
                                                .encodeStart(ops, original)
                                                .getOrThrow()
                                                .equals(expected),
                                        "exact original data and NBT types recovered");
                                var inventory = new SimpleContainer(1);
                                check(
                                        carrier.overrideStackedOnOther(
                                                new Slot(inventory, 0, 0, 0),
                                                ClickAction.SECONDARY,
                                                player),
                                        "vanilla inventory extraction handles carrier");
                                check(
                                        ItemStack.CODEC
                                                .encodeStart(ops, inventory.getItem(0))
                                                .getOrThrow()
                                                .equals(expected),
                                        "vanilla extraction returns exact original");
                                check(
                                        carrier.get(DataComponents.BUNDLE_CONTENTS).isEmpty(),
                                        "extraction leaves no duplicate original");
                            }
                            Files.writeString(Path.of("reopen.pass"), "pass\n");
                        }
                        System.out.println("CARRIER_PHASE_PASS");
                    } catch (Throwable e) {
                        e.printStackTrace();
                        System.out.println("CARRIER_FAIL");
                    } finally {
                        s.halt(false);
                    }
                });
    }
}
