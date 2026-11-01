package dev.rinchan.rinlibprobe;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.component.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.crafting.*;

import java.util.*;

public final class CarrierChecks {
    static void check(boolean ok, String name) {
        Fixtures.check(ok, name);
    }

    public static ItemStack invalid(ItemStack source, int version) {
        var result = source.copy();
        var data = result.get(DataComponents.CUSTOM_DATA).copyTag();
        var payload = (CompoundTag) data.get(CarrierSnapshot.KEY);
        payload.putInt("DataVersion", version == 0 ? -1 : Integer.MAX_VALUE);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return result;
    }

    public static void run(MinecraftServer s, ServerPlayer player, ItemStack sword)
            throws Exception {
        var ops = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var payloadTag =
                (CompoundTag)
                        sword.get(DataComponents.CUSTOM_DATA).copyTag().get(CarrierSnapshot.KEY);
        check(
                payloadTag.contains("DataVersion")
                        && payloadTag.contains("id")
                        && !payloadTag.contains("chunks"),
                "new snapshot stores native item compound and DataVersion directly");
        var edited = sword.copy();
        edited.remove(DataComponents.CUSTOM_DATA);
        check(!SealedItems.isSealed(edited), "ordinary custom data removal is not intercepted");
        edited = sword.copy();
        edited.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        check(!SealedItems.isSealed(edited), "ordinary custom data replacement is not intercepted");
        edited = sword.copy();
        edited.applyComponents(
                DataComponentPatch.builder().remove(DataComponents.CUSTOM_DATA).build());
        check(!SealedItems.isSealed(edited), "ordinary component patch is not intercepted");
        var ingredient = Ingredient.of(Items.RECOVERY_COMPASS);
        check(
                ingredient.test(new ItemStack(Items.RECOVERY_COMPASS)),
                "ordinary recovery compasses remain valid ingredients");
        check(!ingredient.test(sword), "carrier cannot be consumed as a vanilla ingredient");
        var normalInput = CraftingInput.of(1, 1, List.of(new ItemStack(Items.RECOVERY_COMPASS)));
        var carrierInput = CraftingInput.of(1, 1, List.of(sword));
        check(
                s.getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, normalInput, s.overworld())
                        .isPresent(),
                "consumption recipe control is installed");
        check(
                s.getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, carrierInput, s.overworld())
                        .isEmpty(),
                "recipe lookup refuses carrier consumption");
        for (int version : new int[] {0, 999}) {
            var broken = invalid(sword, version);
            var before = ItemStack.CODEC.encodeStart(ops, broken).getOrThrow();
            check(
                    SealedItems.isSealed(broken)
                            && SealedItems.originalForRepair(broken, s.overworld()).isEmpty(),
                    "unknown snapshot remains sealed");
            check(!ingredient.test(broken), "unknown snapshot still rejects ingredients");
            var anvil = new AnvilMenu(21, player.getInventory(), ContainerLevelAccess.NULL);
            anvil.getSlot(0).set(broken);
            anvil.getSlot(1).set(new ItemStack(Items.DIAMOND));
            check(anvil.getSlot(2).getItem().isEmpty(), "invalid snapshot has no anvil output");
            check(
                    ItemStack.CODEC
                                    .encodeStart(ops, anvil.getSlot(0).getItem())
                                    .getOrThrow()
                                    .equals(before)
                            && anvil.getSlot(1).getItem().getCount() == 1,
                    "failed repair leaves snapshot and material untouched");
            var grind = new GrindstoneMenu(22, player.getInventory(), ContainerLevelAccess.NULL);
            check(
                    !grind.getSlot(0).mayPlace(broken) && !grind.getSlot(1).mayPlace(broken),
                    "invalid snapshots fail both native grindstone input checks");
            grind.getSlot(0).set(broken.copy());
            grind.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));
            check(
                    grind.getSlot(2).getItem().isEmpty(),
                    "invalid snapshot cannot be consumed with a healthy grindstone input");
        }
        var corrupt = sword.copy();
        var corruptData = corrupt.get(DataComponents.CUSTOM_DATA).copyTag();
        var badLegacy = new CompoundTag();
        badLegacy.putInt("schema", 1);
        badLegacy.putInt("data_version", 3700);
        var badChunks = new ListTag();
        badChunks.add(StringTag.valueOf("not a compressed snapshot"));
        badLegacy.put("chunks", badChunks);
        corruptData.put(CarrierSnapshot.KEY, badLegacy);
        corrupt.set(DataComponents.CUSTOM_DATA, CustomData.of(corruptData));
        check(
                SealedItems.originalForRepair(corrupt, s.overworld()).isEmpty()
                        && SealedItems.visualCopy(corrupt, s.overworld()) == corrupt,
                "corrupted payload is retained and renders carrier fallback");
        var legacy = LegacyFixtures.original();
        var legacyCarrier = LegacyFixtures.carrier();
        var legacyData = legacyCarrier.get(DataComponents.CUSTOM_DATA).copyTag();
        var payload = (CompoundTag) legacyData.get(CarrierSnapshot.KEY);
        check(
                CarrierSnapshot.decode(payload, s.registryAccess()).result().isEmpty(),
                "native snapshot decoder rejects legacy envelopes");
        var migrated = LegacyCarrierMigration.decode(payload, s.registryAccess()).getOrThrow();
        check(
                migrated.is(Items.DIAMOND_SWORD)
                        && migrated.getDamageValue() == 1561
                        && migrated.get(DataComponents.CUSTOM_DATA)
                                .copyTag()
                                .getString("other_mod_owner")
                                .equals("legacy"),
                "old chunked snapshot upgrades through Minecraft data fixer");
        var anvil = new AnvilMenu(23, player.getInventory(), ContainerLevelAccess.NULL);
        player.experienceLevel = 30;
        anvil.getSlot(0).set(legacyCarrier);
        anvil.getSlot(1).set(new ItemStack(Items.DIAMOND));
        var upgradedData = legacyCarrier.get(DataComponents.CUSTOM_DATA);
        var upgradedPayload = (CompoundTag) upgradedData.copyTag().get(CarrierSnapshot.KEY);
        check(
                upgradedPayload.contains("DataVersion")
                        && upgradedPayload.contains("id")
                        && !upgradedPayload.contains("chunks")
                        && !upgradedPayload.contains("schema")
                        && !upgradedPayload.contains("data_version")
                        && upgradedData.copyTag().getString("other_mod_carrier").equals("retained"),
                "first legacy repair read immediately rewrites carrier as native snapshot");
        check(
                ItemStack.matches(
                                migrated,
                                SealedItems.originalForRepair(legacyCarrier, s.overworld()))
                        && legacyCarrier.get(DataComponents.CUSTOM_DATA) == upgradedData
                        && ItemStack.matches(
                                migrated,
                                SealedItems.originalForRepair(legacyCarrier.copy(), s.overworld())),
                "repeat and uncached reads use native snapshot without another migration write");
        var repaired = anvil.getSlot(2).getItem();
        check(
                repaired.is(Items.DIAMOND_SWORD)
                        && repaired.getDamageValue() < 1561
                        && repaired.get(DataComponents.CUSTOM_DATA)
                                .copyTag()
                                .getString("other_mod_owner")
                                .equals("legacy"),
                "old chunked carrier repairs through actual anvil");
        int cost = anvil.getCost();
        var taken = anvil.getSlot(2).remove(1);
        anvil.getSlot(2).onTake(player, taken);
        check(
                anvil.getSlot(0).getItem().isEmpty()
                        && anvil.getSlot(1).getItem().isEmpty()
                        && player.experienceLevel == 30 - cost,
                "legacy repair consumes carrier material and experience once");
        var readCarrier = LegacyFixtures.carrier();
        readCarrier.setCount(2);
        check(
                SealedItems.originalForRepair(readCarrier, s.overworld()).getCount() == 2
                        && ((CompoundTag)
                                        readCarrier
                                                .get(DataComponents.CUSTOM_DATA)
                                                .copyTag()
                                                .get(CarrierSnapshot.KEY))
                                .contains("DataVersion"),
                "first direct read migrates carrier and preserves its count");
        var poisonedData = readCarrier.get(DataComponents.CUSTOM_DATA).copyTag();
        var nativePayload = (CompoundTag) poisonedData.get(CarrierSnapshot.KEY);
        nativePayload.put("chunks", new ListTag());
        nativePayload.putInt("schema", 999);
        nativePayload.putInt("data_version", Integer.MAX_VALUE);
        readCarrier.set(DataComponents.CUSTOM_DATA, CustomData.of(poisonedData));
        check(
                SealedItems.originalForRepair(readCarrier.copy(), s.overworld())
                        .is(Items.DIAMOND_SWORD),
                "uncached native read never re-enters legacy decoder even with stale legacy"
                        + " fields");
        var nativeOld = legacy.copy();
        nativeOld.putInt("DataVersion", 3700);
        check(
                ItemStack.matches(
                        migrated,
                        CarrierSnapshot.decode(nativeOld, s.registryAccess()).getOrThrow()),
                "native compound snapshots also upgrade old DataVersion");
    }
}
