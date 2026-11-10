package net.ckeeze.terrafirmacolonies.api;

import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Pickup;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkDeliveryman;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;

/** Return surplus empty bowls only while the waiter is already at the restaurant hut. */
public final class WaiterBowlDropoff {
    private WaiterBowlDropoff() {}

    public static int deposit(AbstractEntityCitizen waiter, BuildingCook restaurant, JobEntry job) {
        if (waiter.level().isClientSide || waiter.blockPosition().distSqr(restaurant.getPosition()) > 9
                || restaurant.getTileEntity() == null) {
            return 0;
        }
        var target = restaurant.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (target == null || target == waiter.getInventoryCitizen()) {
            return 0;
        }
        var source = waiter.getInventoryCitizen();
        var kept = new ArrayList<ItemStorage>();
        int transferred = 0;
        boolean blockedSurplus = false;
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            // Do not classify filled dishes or unrelated modded vessels by their names.
            if (!isEmptyBowl(stack)) {
                continue;
            }
            int surplus = restaurant.buildingRequiresCertainAmountOfItem(stack, kept, true, job);
            if (surplus <= 0) {
                continue;
            }
            ItemStack offered = stack.copy();
            offered.setCount(Math.min(surplus, stack.getCount()));
            // Both inventories are server-owned. Insert only a copy, then remove exactly
            // the accepted amount from the original slot; a full target leaves it untouched.
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, offered, false);
            int accepted = Math.min(surplus, stack.getCount()) - remainder.getCount();
            blockedSurplus |= !remainder.isEmpty();
            if (accepted > 0) {
                source.extractItem(slot, accepted, false);
                transferred += accepted;
            }
        }
        if (transferred > 0) {
            restaurant.markDirty();
        }
        // A blocked deposit also needs clearance, but only if a courier can actually
        // remove surplus. Never call one just because all slots contain required food.
        int priority = restaurant.getPickUpPriority();
        if (priority > 0
                && restaurant.getOpenRequestsByRequestableType()
                    .getOrDefault(TypeConstants.PICKUP, java.util.Collections.emptyList()).isEmpty()
                && hasPickupSurplus(restaurant, target, transferred == 0 && !blockedSurplus)) {
            restaurant.createRequest(new Pickup(priority), false);
        }
        return transferred;
    }

    private static boolean isEmptyBowl(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Items.BOWL) || stack.is(TFCBlocks.CERAMIC_BOWL.get().asItem()));
    }

    private static boolean hasPickupSurplus(BuildingCook restaurant, IItemHandler inventory, boolean bowlsOnly) {
        var kept = new ArrayList<ItemStorage>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            // Also resume pickup of bowls deposited while export was disabled (or
            // before a restart). Without an active deposit, unrelated surplus alone
            // must not turn this into a general restaurant-cleaning loop.
            if (!stack.isEmpty() && EntityAIWorkDeliveryman.workerRequiresItem(restaurant, stack, kept) > 0
                    && (!bowlsOnly || isEmptyBowl(stack))) {
                return true;
            }
        }
        return false;
    }
}
