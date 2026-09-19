package dev.rinchan.rinlibprobe;

import com.mojang.serialization.JsonOps;

import dev.rinchan.keepmysword.item.*;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

import java.nio.file.*;

public final class MoreSafetyTests {
    static void check(boolean ok, String name) {
        Fixtures.check(ok, name);
    }

    public static void run(
            MinecraftServer s,
            ServerPlayer player,
            ItemStack sword,
            ItemStack mutated,
            CompoundTag originalData)
            throws Exception {
        var ops = s.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var serialized = ItemStack.CODEC.encodeStart(ops, mutated).getOrThrow();
        var restored = ItemStack.CODEC.parse(ops, serialized).getOrThrow();
        check(
                restored.get(DataComponents.CUSTOM_DATA).copyTag().getBoolean("third_party_marker"),
                "shell metadata survives native disk roundtrip");
        var jsonBaseline =
                StorageBoundary.raw()
                        .parse(
                                ops,
                                StorageBoundary.raw()
                                        .encodeStart(ops, SealedItems.originalForSave(mutated))
                                        .getOrThrow())
                        .getOrThrow();
        check(
                SealedItems.originalForSave(restored)
                        .get(DataComponents.CUSTOM_DATA)
                        .copyTag()
                        .equals(jsonBaseline.get(DataComponents.CUSTOM_DATA).copyTag()),
                "JSON original components match unmodified vanilla conversion semantics");
        check(
                java.util.Arrays.equals(
                        restored.get(DataComponents.CUSTOM_DATA)
                                .copyTag()
                                .getLongArray("shell_longs"),
                        new long[] {1, Long.MAX_VALUE}),
                "JSON envelope preserves exact shell NBT numeric array types");
        var nbtOps = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var nbtRoundtrip =
                ItemStack.CODEC
                        .parse(nbtOps, ItemStack.CODEC.encodeStart(nbtOps, mutated).getOrThrow())
                        .getOrThrow();
        check(
                SealedItems.originalForSave(nbtRoundtrip)
                        .get(DataComponents.CUSTOM_DATA)
                        .copyTag()
                        .equals(originalData),
                "NBT storage preserves exact original numeric types and all opaque data");
        check(
                ItemStack.CODEC.encodeStart(ops, restored).getOrThrow().equals(serialized),
                "repeated encoding does not duplicate the shell envelope");
        // Same public storage API used by real Fabric automation mods, with its own independent
        // disk schema.
        var variant = ItemVariant.of(mutated);
        var variantJson = ItemVariant.CODEC.encodeStart(ops, variant).getOrThrow();
        check(
                !variantJson
                                .getAsJsonObject()
                                .get("item")
                                .getAsString()
                                .equals("keep_my_sword:broken_item")
                        && !variantJson
                                .getAsJsonObject()
                                .getAsJsonObject("components")
                                .has("keep_my_sword:sealed_original"),
                "Fabric Transfer stores the original registry identity and native component types");
        check(
                variantJson
                        .getAsJsonObject()
                        .get("item")
                        .getAsString()
                        .equals("minecraft:diamond_sword"),
                "Fabric Transfer retains its native item field format");
        var returned = ItemVariant.CODEC.parse(ops, variantJson).getOrThrow().toStack();
        check(
                SealedItems.isSealed(returned)
                        && returned.get(DataComponents.CUSTOM_DATA)
                                .copyTag()
                                .getBoolean("third_party_marker"),
                "Fabric Transfer disk roundtrip restores sealed metadata");
        check(
                ItemStack.CODEC.encodeStart(ops, returned).getOrThrow().equals(serialized),
                "Fabric Transfer preserves all canonical data");
        Files.writeString(Path.of("transfer-variant.json"), variantJson.toString());
        // Unfamiliar future data must remain recoverable instead of being thrown away.
        var future = SealedItems.originalForSave(sword);
        var futureData = future.get(DataComponents.CUSTOM_DATA).copyTag();
        var unknown = new CompoundTag();
        unknown.putInt("schema", 999);
        unknown.putString("opaque", "must survive");
        futureData.put(ShellPersistence.KEY, unknown);
        future.set(DataComponents.CUSTOM_DATA, CustomData.of(futureData));
        var rawFuture = StorageBoundary.raw().encodeStart(nbtOps, future).getOrThrow();
        var futureRead = ItemStack.CODEC.parse(nbtOps, rawFuture).getOrThrow();
        check(
                SealedItems.isSealed(futureRead),
                "unknown metadata does not reactivate original item");
        check(
                ItemStack.CODEC.encodeStart(nbtOps, futureRead).getOrThrow().equals(rawFuture),
                "unknown metadata survives unchanged");
        futureRead.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
        var editedFuture =
                ItemStack.CODEC
                        .parse(nbtOps, ItemStack.CODEC.encodeStart(nbtOps, futureRead).getOrThrow())
                        .getOrThrow();
        check(
                SealedItems.originalForSave(editedFuture)
                        .get(DataComponents.CUSTOM_DATA)
                        .copyTag()
                        .getCompound(ShellPersistence.KEY)
                        .equals(unknown),
                "editing shell preserves preexisting unknown metadata without failing save");
        check(
                editedFuture.has(DataComponents.CUSTOM_DATA),
                "explicit empty outer component survives with unknown original data");
        // Exercise the explicit migration path for nested shell snapshots.
        var legacy = SealedItems.originalForSave(sword);
        var legacyData = legacy.get(DataComponents.CUSTOM_DATA).copyTag();
        var legacyShell = new CompoundTag();
        legacyShell.putString("id", "minecraft:paper");
        legacyShell.putByte("Count", (byte) 1);
        var tag = new CompoundTag();
        tag.putString("legacy_marker", "preserved");
        legacyShell.put("tag", tag);
        var record = new CompoundTag();
        record.putInt("schema", 1);
        record.putInt("data_version", 3700);
        record.putBoolean("had_custom_data", true);
        record.put("shell", legacyShell);
        legacyData.put(ShellPersistence.KEY, record);
        legacy.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyData));
        var migrated =
                ItemStack.CODEC
                        .parse(
                                nbtOps,
                                StorageBoundary.raw().encodeStart(nbtOps, legacy).getOrThrow())
                        .getOrThrow();
        check(
                migrated.get(DataComponents.CUSTOM_DATA)
                        .copyTag()
                        .getString("legacy_marker")
                        .equals("preserved"),
                "legacy shell snapshot is upgraded through Minecraft data fixers");
        check(
                SealedItems.originalForSave(migrated)
                        .get(DataComponents.CUSTOM_DATA)
                        .copyTag()
                        .equals(originalData),
                "shell migration does not change original opaque data");
        // Actual input remains sealed while the menu calculates against private snapshots.
        var menu = new AnvilMenu(7, player.getInventory(), ContainerLevelAccess.NULL);
        player.experienceLevel = 30;
        menu.getSlot(0).set(mutated.copy());
        menu.getSlot(1).set(new ItemStack(Items.DIAMOND));
        menu.slotsChanged(menu.getSlot(0).container);
        check(
                SealedItems.isSealed(menu.getSlot(0).getItem()),
                "anvil preview leaves real input sealed");
        check(
                ItemStack.CODEC
                        .encodeStart(ops, menu.getSlot(0).getItem())
                        .getOrThrow()
                        .equals(serialized),
                "anvil preview leaves complete input data unchanged");
        var result = menu.getSlot(2).getItem();
        check(
                !result.isEmpty()
                        && result.is(Items.DIAMOND_SWORD)
                        && result.getDamageValue() < 1561,
                "actual sealed input produces native repair output");
        int cost = menu.getCost();
        check(
                cost > 0 && menu.getSlot(2).mayPickup(player),
                "native anvil output has a payable experience cost");
        var taken = menu.getSlot(2).remove(1);
        menu.getSlot(2).onTake(player, taken);
        check(
                menu.getSlot(0).getItem().isEmpty()
                        && menu.getSlot(1).getItem().isEmpty()
                        && player.experienceLevel == 30 - cost,
                "taking repair result consumes source material and exact experience cost");
        check(
                taken.get(DataComponents.CUSTOM_DATA)
                        .copyTag()
                        .getString("other_mod_owner")
                        .equals("retained"),
                "taken result keeps original metadata");
        check(
                taken.get(DataComponents.CUSTOM_DATA).copyTag().contains(ShellPersistence.KEY),
                "taken result retains shell annotations without overwriting original metadata");
        var crafting =
                net.minecraft.world.item.crafting.CraftingInput.of(
                        2, 1, java.util.List.of(sword.copy(), sword.copy()));
        var recipe =
                new net.minecraft.world.item.crafting.RepairItemRecipe(
                        net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        check(recipe.matches(crafting, s.overworld()), "crafting repair accepts two sealed swords");
        var crafted = recipe.assemble(crafting, s.registryAccess());
        check(
                crafted.is(Items.DIAMOND_SWORD)
                        && crafted.getDamageValue() < crafted.getMaxDamage(),
                "crafting repair returns usable original item");
        check(
                SealedItems.isSealed(crafting.getItem(0)),
                "crafting preview keeps original inputs sealed");
        var grind =
                new GrindstoneMenu(
                        8,
                        player.getInventory(),
                        ContainerLevelAccess.create(
                                s.overworld(), new net.minecraft.core.BlockPos(2, 80, 0)));
        check(
                grind.getSlot(0).mayPlace(sword) && grind.getSlot(1).mayPlace(sword),
                "both grindstone slots accept sealed items");
        grind.getSlot(0).set(mutated.copy());
        grind.getSlot(1).set(sword.copy());
        var ground = grind.getSlot(2).getItem().copy();
        check(
                ground.is(Items.DIAMOND_SWORD) && ground.getDamageValue() < ground.getMaxDamage(),
                "grindstone repairs two sealed items");
        check(
                SealedItems.isSealed(grind.getSlot(0).getItem()),
                "grindstone preview does not reactivate inputs");
        check(!ground.isEnchanted(), "grindstone removes non-curse enchantments normally");
        var resultSlot = grind.getSlot(2);
        var collected = resultSlot.remove(1);
        resultSlot.onTake(player, collected);
        check(
                grind.getSlot(0).getItem().isEmpty() && grind.getSlot(1).getItem().isEmpty(),
                "taking grindstone output consumes both inputs");
        var constructed =
                new ItemStack(
                        Items.DIAMOND_SWORD.builtInRegistryHolder(),
                        1,
                        net.minecraft.core.component.DataComponentPatch.builder()
                                .set(DataComponents.DAMAGE, 1561)
                                .build());
        check(
                SealedItems.isSealed(constructed),
                "construction from an exhausted component patch seals immediately");
        var renamedMenu = new AnvilMenu(9, player.getInventory(), ContainerLevelAccess.NULL);
        renamedMenu.getSlot(0).set(mutated.copy());
        renamedMenu.getSlot(1).set(new ItemStack(Items.DIAMOND));
        renamedMenu.setItemName("Reforged");
        var renamed = renamedMenu.getSlot(2).getItem().copy();
        check(
                renamed.is(Items.DIAMOND_SWORD)
                        && renamed.getHoverName().getString().equals("Reforged"),
                "anvil repair and rename produces original item with new name");
        renamed.setDamageValue(renamed.getMaxDamage() - 1);
        renamed.hurtAndBreak(1, s.overworld(), null, item -> {});
        check(
                SealedItems.isSealed(renamed)
                        && renamed.getHoverName().getString().equals("Reforged"),
                "new anvil name survives the next break with shell annotations");
        var excluded = new ItemStack(Items.IRON_SWORD);
        check(
                dev.rinchan.keepmysword.KeepMySword.isExcluded(excluded),
                "excluded tag loaded from test datapack");
        excluded.setDamageValue(excluded.getMaxDamage() - 1);
        excluded.hurtAndBreak(1, s.overworld(), null, item -> {});
        check(excluded.isEmpty(), "excluded item retains vanilla break behavior");
        var direct = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        direct.setDamageValue(direct.getMaxDamage());
        Fixtures.check(SealedItems.isSealed(direct), "direct durability setter seals immediately");
        var component = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        component.set(DataComponents.DAMAGE, component.getMaxDamage());
        Fixtures.check(
                SealedItems.isSealed(component), "direct damage component write seals immediately");
        var patchStack = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        patchStack.applyComponents(
                net.minecraft.core.component.DataComponentPatch.builder()
                        .set(DataComponents.DAMAGE, patchStack.getMaxDamage())
                        .build());
        Fixtures.check(SealedItems.isSealed(patchStack), "component patch seals immediately");
        taken.setDamageValue(taken.getMaxDamage() - 1);
        taken.hurtAndBreak(1, s.overworld(), null, item -> {});
        check(
                SealedItems.isSealed(taken)
                        && taken.get(DataComponents.CUSTOM_DATA)
                                .copyTag()
                                .getBoolean("third_party_marker"),
                "repair followed by another actual break restores shell annotations");
    }

    private MoreSafetyTests() {}
}
