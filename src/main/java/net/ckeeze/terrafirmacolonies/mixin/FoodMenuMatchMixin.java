package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.util.FoodUtils;
import net.ckeeze.terrafirmacolonies.api.FoodStackSafety;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(value = FoodUtils.class, remap = false)
public abstract class FoodMenuMatchMixin {
    @Redirect(method = {"getBestFoodForCitizen", "checkForFoodInBuilding", "hasBestOptionInInv"},
            at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"), require = 3)
    private static boolean terrafirmacolonies$matchDishType(Set<?> menu, Object candidate) {
        return FoodStackSafety.isOnMenu(menu, candidate);
    }
}
