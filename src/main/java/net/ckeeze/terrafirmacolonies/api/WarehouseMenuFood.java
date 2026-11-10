package net.ckeeze.terrafirmacolonies.api;

import com.minecolonies.api.colony.buildings.ICommonBuilding;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.core.tileentities.TileEntityRack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Read actual stacks: the rack cache can combine fresh and rotten meal variants. */
public final class WarehouseMenuFood {
    private WarehouseMenuFood() {}

    public static boolean applies(IDeliverable request) {
        return request instanceof Stack stack && RestaurantFoodRequests.isMenuRequest(stack.getStack());
    }

    public static int count(ICommonBuilding building, Predicate<ItemStack> matches, int target) {
        int count = 0;
        var world = building.getColony().getWorld();
        if (world == null || target <= 0) return 0;
        for (var pos : building.getContainers()) {
            if (!world.hasChunkAt(pos) || !(world.getBlockEntity(pos) instanceof TileEntityRack rack)) continue;
            var inventory = rack.getInventory();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                var actual = inventory.getStackInSlot(slot);
                if (!actual.isEmpty() && matches.test(actual)) {
                    count += Math.min(actual.getCount(), target - count);
                    if (count >= target) return target;
                }
            }
        }
        return count;
    }

    public static List<Tuple<ItemStack, BlockPos>> select(ICommonBuilding building, Predicate<ItemStack> matches) {
        List<Tuple<ItemStack, BlockPos>> result = new ArrayList<>();
        if (building == null) return result;
        var world = building.getColony().getWorld();
        if (world == null) return result;
        for (var pos : building.getContainers()) {
            if (!world.hasChunkAt(pos) || !(world.getBlockEntity(pos) instanceof TileEntityRack rack)) continue;
            var inventory = rack.getInventory();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                var actual = inventory.getStackInSlot(slot);
                if (!actual.isEmpty() && matches.test(actual)) result.add(new Tuple<>(actual, pos));
            }
        }
        return result;
    }
}
