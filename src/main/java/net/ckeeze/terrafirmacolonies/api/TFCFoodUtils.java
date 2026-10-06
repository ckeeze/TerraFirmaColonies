package net.ckeeze.terrafirmacolonies.api;

import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodData;
import net.dries007.tfc.common.capabilities.food.IFood;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class TFCFoodUtils {

    //Checks if food is tfc food
    public static boolean isTFCEdibleFood(final ItemStack stack) {
        if (stack.isEmpty()) return false;
        final @Nullable IFood food = FoodCapability.get(stack);
        if (food == null || food.isRotten()) return false;
        return food.getData().hunger() > 0;
    }

    //Summs up all nutritients into a double value that will be used to judge food quality and tier
    public static double getTFCEffectiveNutrition(final ItemStack stack) {
        final @Nullable IFood food = FoodCapability.get(stack);
        if (food == null) return 0;
        final FoodData data = food.getData();
        double nutritionbonus = 1.0;
        if (data.vegetables() > 0.0) {
            nutritionbonus += 0.1;
        }
        if (data.protein() > 0.0) {
            nutritionbonus += 0.05;
        }
        if (data.grain() > 0.0) {
            nutritionbonus += 0.1;
        }
        if (data.fruit() > 0.0) {
            nutritionbonus += 0.15;
        }
        if (data.dairy() > 0.0) {
            nutritionbonus += 0.25;
        }
        return ((data.dairy() + data.fruit() + data.grain() + data.protein() + data.vegetables()) * nutritionbonus);
    }
}
