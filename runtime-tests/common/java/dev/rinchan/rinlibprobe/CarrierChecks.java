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

    public static ItemStack invalid(ItemStack source, int schema) {
        return SnapshotScope.call(
                () -> {
                    var result = source.copy();
                    var data = result.get(DataComponents.CUSTOM_DATA).copyTag();
                    var envelope = ((CompoundTag) data.get(CarrierSnapshot.KEY));
                    envelope.putInt("schema", schema);
                    data.put(CarrierSnapshot.KEY, envelope);
                    result.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                    return result;
                });
    }

    public static void run(MinecraftServer s, ServerPlayer player, ItemStack sword)
            throws Exception {
        var ops = s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var snapshot =
                ItemStack.CODEC.encodeStart(ops, SealedItems.originalForRepair(sword)).getOrThrow();
        var edited = sword.copy();
        edited.remove(DataComponents.CUSTOM_DATA);
        check(SealedItems.isSealed(edited), "removing custom data cannot discard original");
        edited.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        check(SealedItems.isSealed(edited), "overwriting custom data cannot discard original");
        edited.applyComponents(
                DataComponentPatch.builder().remove(DataComponents.CUSTOM_DATA).build());
        check(SealedItems.isSealed(edited), "component patch cannot remove snapshot");
        check(
                ItemStack.CODEC
                        .encodeStart(ops, SealedItems.originalForRepair(edited))
                        .getOrThrow()
                        .equals(snapshot),
                "metadata protection preserves exact original");
        var equipment = new ItemStack(Items.DIAMOND_CHESTPLATE);
        equipment.setDamageValue(equipment.getMaxDamage() - 1);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, equipment);
        ((EquipmentRefresh) player).kms$refreshEquipment();
        double armorBefore =
                player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        check(armorBefore > 0, "equipped armor contributes native attributes before break");
        equipment.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.CHEST);
        check(
                SealedItems.isSealed(equipment)
                        && player.getAttributeValue(
                                        net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
                                == 0,
                "equipped break removes cached armor attributes immediately");
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
        for (int schema : new int[] {0, 999}) {
            var broken = invalid(sword, schema);
            var before = ItemStack.CODEC.encodeStart(ops, broken).getOrThrow();
            check(
                    SealedItems.isSealed(broken) && SealedItems.originalForRepair(broken).isEmpty(),
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
            grind.getSlot(0).set(broken.copy());
            grind.getSlot(1).set(new ItemStack(Items.DIAMOND_SWORD));
            check(
                    grind.getSlot(2).getItem().isEmpty(),
                    "invalid snapshot cannot be consumed with a healthy grindstone input");
        }
        var corrupt =
                SnapshotScope.call(
                        () -> {
                            var st = sword.copy();
                            var data = st.get(DataComponents.CUSTOM_DATA).copyTag();
                            var payload = ((CompoundTag) data.get(CarrierSnapshot.KEY));
                            var chunks = new ListTag();
                            chunks.add(StringTag.valueOf("not a compressed snapshot"));
                            payload.put("chunks", chunks);
                            data.put(CarrierSnapshot.KEY, payload);
                            st.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                            return st;
                        });
        check(
                SealedItems.originalForRepair(corrupt).isEmpty()
                        && SealedItems.visualCopy(corrupt) == corrupt,
                "corrupted payload is retained and renders carrier fallback");
        var legacy =
                TagParser.parseTag(
                        "{id:\"minecraft:diamond_sword\",Count:1b,tag:{Damage:1561,other_mod_owner:\"legacy\"}}");
        var bytes = new java.io.ByteArrayOutputStream();
        NbtIo.writeCompressed(legacy, bytes);
        var payload = new CompoundTag();
        payload.putInt("schema", 1);
        payload.putInt("data_version", 3700);
        var chunks = new ListTag();
        chunks.add(StringTag.valueOf(Base64.getEncoder().encodeToString(bytes.toByteArray())));
        payload.put("chunks", chunks);
        var migrated = CarrierSnapshot.decode(payload, s.registryAccess()).getOrThrow();
        check(
                migrated.is(Items.DIAMOND_SWORD)
                        && migrated.getDamageValue() == 1561
                        && migrated.get(DataComponents.CUSTOM_DATA)
                                .copyTag()
                                .getString("other_mod_owner")
                                .equals("legacy"),
                "old native item data upgrades through Minecraft data fixer");
        var failing =
                SnapshotScope.call(
                        () -> {
                            var st = new ItemStack(Items.DIAMOND_SWORD);
                            var data = new CompoundTag();
                            data.putString("too_long_for_native_nbt", "x".repeat(70000));
                            st.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                            st.setDamageValue(st.getMaxDamage() - 1);
                            return st;
                        });
        failing.hurtAndBreak(1, s.overworld(), null, item -> {});
        check(
                failing.getCount() == 1
                        && failing.is(Items.DIAMOND_SWORD)
                        && failing.get(DataComponents.CUSTOM_DATA)
                                        .copyTag()
                                        .getString("too_long_for_native_nbt")
                                        .length()
                                == 70000,
                "failed snapshot encoding cancels destruction and retains original");
    }
}
