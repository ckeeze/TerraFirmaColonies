package net.ckeeze.terrafirmacolonies.api;

import com.eerussianguy.firmalife.common.items.FLItems;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.InventoryUtils;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.capabilities.food.DynamicBowlHandler;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

/** Container handling shared by normal eating and the waiter's direct-feed fallback. */
public final class FoodContainerReturns {
    private FoodContainerReturns() {}

    public static ItemStack getContainer(ItemStack food) {
        if (food.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (FoodCapability.get(food) instanceof DynamicBowlHandler bowlFood) {
            ItemStack bowl = bowlFood.getBowl();
            if (!bowl.isEmpty()) {
                return bowl;
            }
        }
        if (food.is(TFCTags.Items.UNSEALED_JARS)) {
            return new ItemStack(TFCItems.EMPTY_JAR.get());
        }
        // Keep Firmalife's existing pie-pan support without matching unrelated names
        // or text buried in an ingredient's NBT.
        // Firmalife's pumpkin-pie oven recipe deliberately produces the vanilla item.
        if (food.is(Items.PUMPKIN_PIE) || (food.getDescriptionId().startsWith("item.firmalife.")
                && food.getDescriptionId().contains("pie"))) {
            return new ItemStack(FLItems.PIE_PAN.get());
        }
        return ItemStack.EMPTY;
    }

    public static void returnContainer(ItemStack container, AbstractEntityCitizen citizen, Inventory playerInventory) {
        returnContainer(container, citizen, playerInventory, null);
    }

    public static void returnServedContainer(ItemStack container, AbstractEntityCitizen citizen,
                                            AbstractEntityCitizen waiter) {
        returnContainer(container, citizen, null, waiter);
    }

    private static void returnContainer(ItemStack container, AbstractEntityCitizen citizen,
                                        Inventory playerInventory, AbstractEntityCitizen waiter) {
        if (container.isEmpty()) {
            return;
        }
        IItemHandler target = playerInventory == null ? citizen.getInventoryCitizen()
                : new PlayerMainInvWrapper(playerInventory);
        // All slots occupied can still mean room in a bowl stack. Use actual insertion,
        // not the cached free-slot counter, and drop only what could not be inserted.
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, container.copy(), false);
        // Direct feeding already extracted the portion from the waiter. Reuse that
        // freed slot (or an existing container stack) when the guest cannot carry it.
        if (!remainder.isEmpty() && waiter != null && waiter != citizen) {
            remainder = ItemHandlerHelper.insertItemStacked(waiter.getInventoryCitizen(), remainder, false);
        }
        if (!remainder.isEmpty()) {
            InventoryUtils.spawnItemStack(citizen.level(), citizen.getX(), citizen.getY(), citizen.getZ(), remainder);
        }
    }
}
