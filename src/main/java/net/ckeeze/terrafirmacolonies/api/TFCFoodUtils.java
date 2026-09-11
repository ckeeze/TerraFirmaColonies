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
        return ((data.dairy() * 1.5) + (data.fruit() * 1.1) + data.grain() + (data.protein() * 0.8) + data.vegetables());
    }
}
