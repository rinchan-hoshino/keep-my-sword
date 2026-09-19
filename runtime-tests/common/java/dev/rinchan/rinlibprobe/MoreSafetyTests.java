package dev.rinchan.rinlibprobe;

import com.mojang.serialization.JsonOps;

import dev.rinchan.keepmysword.item.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

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
    }

    private MoreSafetyTests() {}
}
