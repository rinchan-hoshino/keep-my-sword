package dev.rinchan.keepmysword.item;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The original is live only inside an open work menu; all exit paths seal broken items. */
public final class RepairView {
    public static boolean enabled(AbstractContainerMenu menu, Player player) {
        if (menu == null || menu == player.inventoryMenu
                || menu instanceof InventoryMenu || menu instanceof HorseInventoryMenu) return false;
        if (menu instanceof ItemCombinerMenu) return true;
        boolean hasWorkSlot = false;
        boolean hasResultContainer = false;
        boolean hasGeneralStorageSlot = false;
        for (var slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;
            hasWorkSlot = true;
            if (isStorageContainer(slot.container)) return false;
            if (slot.container instanceof ResultContainer) hasResultContainer = true;
            else if (isGeneralStorageSlot(slot)) hasGeneralStorageSlot = true;
        }
        return hasWorkSlot && (hasResultContainer || !hasGeneralStorageSlot);
    }

    private static boolean isStorageContainer(Container container) {
        return container instanceof BlockEntity
                || container instanceof CompoundContainer
                || container instanceof PlayerEnderChestContainer
                || container instanceof WorldlyContainer;
    }

    private static boolean isGeneralStorageSlot(Slot slot) {
        return slot.getMaxStackSize() > 1 && slot.mayPlace(new ItemStack(Items.COBBLESTONE));
    }

    public static void expose(AbstractContainerMenu menu, Player player) {
        if (!enabled(menu, player)) return;
        for (var slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;
            var stack = slot.getItem();
            var original = original(stack, player);
            if (original != stack) slot.set(original);
        }
    }

    private static ItemStack original(ItemStack stack, Player player) {
        if (!SealedItems.isSealed(stack)) return stack;
        var original = SealedItems.originalForRepair(stack, player.level());
        // An unreadable snapshot must never turn into an empty slot.
        return original.isEmpty() ? stack : original;
    }

    public static void beforeClick(AbstractContainerMenu menu, int index, int button, String type, Player player) {
        expose(menu, player);
        if (!enabled(menu, player)) return;
        if (type.equals("QUICK_CRAFT")
                || index >= 0 && index < menu.slots.size() && !(menu.getSlot(index).container instanceof Inventory)) {
            menu.setCarried(original(menu.getCarried(), player));
            if (type.equals("SWAP") && (button >= 0 && button < 9 || button == 40)) {
                var inventory = player.getInventory();
                inventory.setItem(button, original(inventory.getItem(button), player));
            }
        }
        if (type.equals("QUICK_MOVE") && index >= 0 && index < menu.slots.size()) {
            var slot = menu.getSlot(index);
            if (slot.container instanceof Inventory) slot.set(original(slot.getItem(), player));
        }
    }

    public static void afterClick(AbstractContainerMenu menu, Player player) {
        sealInventory(player);
        sealCarried(menu, player);
        expose(menu, player);
    }

    public static void close(AbstractContainerMenu menu, Player player) {
        for (var slot : menu.slots) {
            if (!(slot.container instanceof Inventory)
                    && SealedItems.sealInPlace(slot.getItem(), player.level().registryAccess())) slot.setChanged();
        }
        sealInventory(player);
        sealCarried(menu, player);
    }

    public static void tick(Player player) {
        expose(player.containerMenu, player);
        sealCarried(player.containerMenu, player);
        if (player.tickCount % 20 == 0) sealInventory(player);
    }

    private static void sealCarried(AbstractContainerMenu menu, Player player) {
        SealedItems.sealInPlace(menu.getCarried(), player.level().registryAccess());
    }

    private static void sealInventory(Player player) {
        var inventory = player.getInventory();
        boolean changed = false;
        for (int i = 0; i < inventory.getContainerSize(); i++)
            changed |= SealedItems.sealInPlace(inventory.getItem(i), player.level().registryAccess());
        if (changed) {
            inventory.setChanged();
            ((EquipmentRefresh) player).kms$refreshEquipment();
        }
    }

    private RepairView() {}
}
