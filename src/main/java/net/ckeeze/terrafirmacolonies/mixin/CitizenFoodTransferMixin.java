package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.core.entity.ai.minimal.EntityAIEatTask;
import net.ckeeze.terrafirmacolonies.api.FoodStackSafety;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityAIEatTask.class, remap = false)
public abstract class CitizenFoodTransferMixin {
    @Redirect(method = "getFoodYourself", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/InventoryUtils;transferItemStackIntoNextBestSlotInItemHandler(Lcom/minecolonies/api/colony/buildings/IBuilding;Lcom/minecolonies/api/crafting/ItemStorage;ILnet/minecraftforge/items/IItemHandler;)Z"), require = 1)
    private boolean terrafirmacolonies$takeSelectedFood(IBuilding building, ItemStorage selected, int count, IItemHandler target) {
        return FoodStackSafety.transferSelectedFood(building, selected, count, target);
    }
}
