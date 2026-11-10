package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.core.tileentities.TileEntityRack;
import net.ckeeze.terrafirmacolonies.api.FoodStackSafety;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = FoodUtils.class, remap = false)
public abstract class RestaurantFoodSearchMixin {
    @Redirect(method = "checkForFoodInBuilding", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/core/tileentities/TileEntityRack;getAllContent()Ljava/util/Map;"), require = 1)
    private static Map<ItemStorage, Integer> terrafirmacolonies$inspectActualFood(TileEntityRack rack) {
        return FoodStackSafety.foodSearchContents(rack);
    }
}
