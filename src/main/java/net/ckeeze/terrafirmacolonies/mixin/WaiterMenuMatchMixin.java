package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkCook;
import net.ckeeze.terrafirmacolonies.api.FoodStackSafety;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(value = EntityAIWorkCook.class, remap = false)
public abstract class WaiterMenuMatchMixin {
    @Redirect(method = {"canEat", "isSmeltable"},
            at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"), require = 2)
    private boolean terrafirmacolonies$matchDishType(Set<?> menu, Object candidate) {
        return FoodStackSafety.isOnMenu(menu, candidate);
    }

    @Redirect(method = {"lambda$checkForImportantJobs$7", "lambda$serveFoodToCitizen$2", "lambda$serveFoodToPlayer$3"},
            at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"), require = 3)
    private static boolean terrafirmacolonies$matchServingDishType(Set<?> menu, Object candidate) {
        return FoodStackSafety.isOnMenu(menu, candidate);
    }
}
