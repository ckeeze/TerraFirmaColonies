package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.core.items.ItemCrop;
import net.ckeeze.terrafirmacolonies.api.TFCFoodUtils;
import net.dries007.tfc.common.TFCTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;

@Mixin(value = FoodUtils.class)
public class FoodUtilsMixin {

    /**
     * @author Ckeeze
     * @reason using TFC values
     */
    @Overwrite(remap = false)
    public static boolean canEat(ItemStack stack, IBuilding homeBuilding, IBuilding workBuilding) {
        if (!TFCFoodUtils.isTFCEdibleFood(stack)) {
            return false;
        } else {
            int homeBuildingLevel = homeBuilding == null ? 0 : homeBuilding.getBuildingLevelEquivalent();
            return canEatLevel(stack, homeBuildingLevel) && (workBuilding == null || workBuilding.canEat(stack));
        }
    }

    /**
     * @author Ckeeze
     * @reason using TFC values
     */
    @Overwrite(remap = false)
    public static boolean canEatLevel(ItemStack stack, int buildingLevel) {
        if (stack.getItem() instanceof ItemCrop) {
            return false;
        } else if (buildingLevel < 3) {
            return stack.getItem().getFoodProperties(stack, null) != null;
        } else {
            FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, null);
            return foodProperties != null && TFCFoodUtils.getTFCEffectiveNutrition(stack) >= buildingLevel + 1;
        }
    }

    /**
     * @author Ckeeze
     * @reason Using TFC values
     */
    @Overwrite(remap = false)
    public static int getBuildingLevelForFood(ItemStack resource) {
        return (int) Math.max(2, Math.min(TFCFoodUtils.getTFCEffectiveNutrition(resource) - 1, 5));
    }

    /**
     * @author Ckeeze
     * @reason getting better values for TFC foods depending on nutrient values
     */
    @Overwrite(remap = false)
    public static double getFoodValue(ItemStack foodStack, @Nullable FoodProperties itemFood, double researchBonus) {
        double value = TFCFoodUtils.getTFCEffectiveNutrition(foodStack);
        if (value != 0) {
            return value;
        } else {
            if (!foodStack.is(TFCTags.Items.FOODS)) {
                if (itemFood == null) {
                    return 0.0F;
                }
                double saturationNerf = foodStack.getItem() instanceof IMinecoloniesFoodItem ? (double) 1.0F : (double) 0.25F;
                return (double) itemFood.getNutrition() * saturationNerf / 1.2 * ((double) 1.0F + researchBonus);
            }
        }
        return value;
    }
}
