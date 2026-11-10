package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.core.event.ClientEventHandler;
import net.ckeeze.terrafirmacolonies.api.FoodStackSafety;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Client-only: the menu warning must use the same matching rule as server food selection. */
@Mixin(value = ClientEventHandler.class, remap = false)
public abstract class FoodMenuTooltipMixin {
    @Redirect(method = "onItemTooltipEvent",
            at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"), require = 1)
    private static boolean terrafirmacolonies$matchDishType(List<?> menu, Object candidate) {
        return FoodStackSafety.isOnMenu(menu, candidate);
    }
}
