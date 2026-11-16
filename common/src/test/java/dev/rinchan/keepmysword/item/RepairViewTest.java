package dev.rinchan.keepmysword.item;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import net.minecraft.core.BlockPos;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

class RepairViewTest {
    private static Player mockPlayerWithInventory() {
        var player = mock(Player.class);
        var level = mock(net.minecraft.world.level.Level.class);
        when(level.getRecipeManager()).thenReturn(mock(net.minecraft.world.item.crafting.RecipeManager.class));
        when(player.level()).thenReturn(level);
        var inventory = new Inventory(player);
        when(player.getInventory()).thenReturn(inventory);
        return player;
    }

    private static final class DummyMenu extends AbstractContainerMenu {
        DummyMenu(Container container, int slotCount, int maxStack, boolean allowGeneric) {
            super(null, 0);
            for (int i = 0; i < slotCount; i++) {
                addSlot(new Slot(container, i, 0, 0) {
                    @Override public int getMaxStackSize() { return maxStack; }
                    @Override public boolean mayPlace(ItemStack stack) { return allowGeneric || stack.isDamageableItem(); }
                });
            }
        }

        void addCustomSlot(Slot slot) {
            addSlot(slot);
        }

        @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(Player player) { return true; }
    }

    @Test void playerInventoryIsDisabled() {
        var player = mockPlayerWithInventory();
        var invMenu = new InventoryMenu(player.getInventory(), true, player);
        assertFalse(RepairView.enabled(invMenu, player));
    }

    @Test void standardWorkAndRepairMenusAreEnabled() {
        var player = mockPlayerWithInventory();
        var inv = player.getInventory();
        assertTrue(RepairView.enabled(new AnvilMenu(1, inv), player));
        assertTrue(RepairView.enabled(new SmithingMenu(2, inv), player));
        assertTrue(RepairView.enabled(new GrindstoneMenu(3, inv), player));
        assertTrue(RepairView.enabled(new CraftingMenu(4, inv), player));
        assertTrue(RepairView.enabled(new EnchantmentMenu(5, inv), player));
    }

    @Test void moddedSingleItemRepairOrEnchantingMenuIsEnabled() {
        var player = mockPlayerWithInventory();
        var infuserLike = new DummyMenu(new SimpleContainer(1), 1, 1, false);
        assertTrue(RepairView.enabled(infuserLike, player));
    }

    @Test void vanillaAndBlockEntityStorageMenusAreDisabled() {
        var player = mockPlayerWithInventory();
        var inv = player.getInventory();
        assertFalse(RepairView.enabled(ChestMenu.threeRows(1, inv), player));
        assertFalse(RepairView.enabled(new ShulkerBoxMenu(2, inv), player));
        assertFalse(RepairView.enabled(new HopperMenu(3, inv), player));
        assertFalse(RepairView.enabled(new DispenserMenu(4, inv), player));

        var blockEntityContainer = mock(ChestBlockEntity.class, withSettings().extraInterfaces(Container.class));
        assertFalse(RepairView.enabled(new DummyMenu(blockEntityContainer, 1, 1, false), player));
        assertFalse(RepairView.enabled(new DummyMenu(new CompoundContainer(new SimpleContainer(27), new SimpleContainer(27)), 54, 64, true), player));
        assertFalse(RepairView.enabled(new DummyMenu(new PlayerEnderChestContainer(), 27, 64, true), player));
        assertFalse(RepairView.enabled(new DummyMenu(mock(WorldlyContainer.class), 5, 64, true), player));
    }

    @Test void moddedBackpackWithCustomSlotsAndUpgradeSlotsIsDisabled() {
        var player = mockPlayerWithInventory();
        var backpackContainer = new SimpleContainer(29);
        var backpackMenu = new DummyMenu(backpackContainer, 27, 64, true);
        // Upgrade slots (restricted, stack size 1) alongside normal backpack slots must not enable RepairView.
        backpackMenu.addCustomSlot(new Slot(backpackContainer, 27, 0, 0) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        assertFalse(RepairView.enabled(backpackMenu, player));
    }
}
