package net.ckeeze.terrafirmacolonies.api;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.core.tileentities.TileEntityRack;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.IdentityHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraftforge.items.wrapper.InvWrapper;

/** Food-specific comparisons; resource requests and recipe matching keep their original semantics. */
public final class FoodStackSafety {
    private FoodStackSafety() {}

    /** The fulfil packet carries one representative stack, so count only that food variant. */
    public static int countSelectedRequestFood(IItemHandler inventory, Predicate<ItemStack> matchesRequest) {
        if (inventory instanceof InvWrapper) {
            // Match the request window's selection range: main inventory, excluding armor/offhand.
            int selectableSlots = Math.max(0, inventory.getSlots() - 5);
            for (int slot = 0; slot < selectableSlots; slot++) {
                ItemStack selected = inventory.getStackInSlot(slot);
                if (selected.isEmpty() || !matchesRequest.test(selected)) continue;
                if (FoodCapability.get(selected) == null) break;
                int count = 0;
                for (int other = 0; other < selectableSlots; other++) {
                    ItemStack actual = inventory.getStackInSlot(other);
                    if (!actual.isEmpty() && matchesRequest.test(actual)
                            && ItemStackUtils.compareItemStacksIgnoreStackSize(selected, actual)
                            && sameFood(selected, actual)) count += actual.getCount();
                }
                return count;
            }
        }
        return InventoryUtils.getItemCountInItemHandler(inventory, matchesRequest);
    }

    /** A restaurant menu selects a TFC dish type, not its bowl, filling or preparation date. */
    public static boolean isOnMenu(Collection<?> menu, Object candidate) {
        if (candidate instanceof ItemStorage wanted && FoodCapability.get(wanted.getItemStack()) != null) {
            for (Object entry : menu) {
                if (entry instanceof ItemStorage listed && listed.getItem() == wanted.getItem()) {
                    return true;
                }
            }
            return false;
        }
        return menu.contains(candidate);
    }

    /** Include composition, ingredients, preservation traits and age. Never rejuvenate food by merging. */
    public static boolean sameFood(ItemStack first, ItemStack second) {
        var firstFood = FoodCapability.get(first);
        var secondFood = FoodCapability.get(second);
        if (firstFood == null && secondFood == null) {
            return true;
        }
        if (firstFood == null || secondFood == null) {
            return false;
        }
        if (firstFood instanceof FoodHandler firstHandler && secondFood instanceof FoodHandler secondHandler) {
            // Addons may expose the TFC capability under their own provider key.
            return firstHandler.serializeNBT().equals(secondHandler.serializeNBT());
        }
        // Unknown food providers: retain their complete capability state rather than assuming
        // a provider key or permitting a merge that could discard composition or freshness.
        return Objects.equals(first.serializeNBT().getCompound("ForgeCaps"),
                second.serializeNBT().getCompound("ForgeCaps"));
    }

    /**
     * A temporary view used only by the restaurant food search. Identity keys retain each real
     * stack even when MineColonies' ItemStorage equality groups different food capabilities.
     * Ordinary ItemStorage keys are intentional: the menu still selects an item type, not a filling.
     */
    public static Map<ItemStorage, Integer> foodSearchContents(TileEntityRack rack) {
        Map<ItemStorage, Integer> contents = new IdentityHashMap<>();
        var inventory = rack.getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                contents.put(new ItemStorage(stack.copy()), stack.getCount());
            }
        }
        return contents;
    }

    /** Transfer the food actually selected, rather than the first stack with the same item ID. */
    public static boolean transferSelectedFood(IBuilding building, ItemStorage selected, int count, IItemHandler target) {
        if (FoodCapability.get(selected.getItemStack()) == null) {
            return InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(building, selected, count, target);
        }
        var world = building.getColony().getWorld();
        for (var pos : building.getContainers()) {
            if (!world.hasChunkAt(pos) || !(world.getBlockEntity(pos) instanceof TileEntityRack rack)) {
                continue;
            }
            var inventory = rack.getInventory();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack actual = inventory.getStackInSlot(slot);
                if (!selected.equals(new ItemStorage(actual)) || !sameFood(selected.getItemStack(), actual)) {
                    continue;
                }
                ItemStack extracted = inventory.extractItem(slot, count, true);
                if (!extracted.isEmpty() && InventoryUtils.addItemStackToItemHandler(target, extracted)) {
                    inventory.extractItem(slot, count, false);
                    return true;
                }
            }
        }
        return false;
    }
}
