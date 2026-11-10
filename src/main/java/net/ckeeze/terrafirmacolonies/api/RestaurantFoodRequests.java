package net.ckeeze.terrafirmacolonies.api;

import com.minecolonies.api.colony.buildings.ICommonBuilding;
import com.minecolonies.core.tileentities.TileEntityRack;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.minecraft.world.item.ItemStack;

/** The marker belongs only to a menu request's copied prototype, never to delivered food. */
public final class RestaurantFoodRequests {
    private static final String MENU_REQUEST = "terrafirmacolonies:restaurant_food_request";

    private RestaurantFoodRequests() {}

    public static ItemStack markPrototype(ItemStack prototype) {
        ItemStack copy = prototype.copy();
        copy.getOrCreateTag().putBoolean(MENU_REQUEST, true);
        return copy;
    }

    public static boolean isMenuRequest(ItemStack prototype) {
        return prototype.hasTag() && prototype.getTag().getBoolean(MENU_REQUEST)
                && FoodCapability.get(prototype) != null;
    }

    /** Menu identity is the dish; its actual nutritional composition must still be usable. */
    public static boolean matches(ItemStack prototype, ItemStack actual) {
        return actual.getItem() == prototype.getItem() && TFCFoodUtils.isTFCEdibleFood(actual);
    }

    public static int countStock(ICommonBuilding building, ItemStack prototype, int target) {
        int count = 0;
        var world = building.getColony().getWorld();
        for (var pos : building.getContainers()) {
            if (!world.hasChunkAt(pos) || !(world.getBlockEntity(pos) instanceof TileEntityRack rack)) continue;
            // Rack's cached ItemStorage map can group fresh and spoiled food of the same item.
            var inventory = rack.getInventory();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                var actual = inventory.getStackInSlot(slot);
                if (matches(prototype, actual)) count += actual.getCount();
            }
            if (count >= target) return count;
        }
        return count;
    }
}
