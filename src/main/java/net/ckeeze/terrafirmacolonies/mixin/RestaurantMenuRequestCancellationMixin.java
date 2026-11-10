package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.requestsystem.requestable.MinimumStack;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.core.colony.buildings.modules.RestaurantMenuModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Menu replenishment creates MinimumStack requests, indexed by that exact type. */
@Mixin(value = RestaurantMenuModule.class, remap = false)
public abstract class RestaurantMenuRequestCancellationMixin {
    @ModifyArg(method = "removeMenuItem", at = @At(value = "INVOKE",
            target = "Lcom/google/common/reflect/TypeToken;of(Ljava/lang/Class;)Lcom/google/common/reflect/TypeToken;"),
            index = 0, require = 1)
    private Class<?> terrafirmacolonies$findMenuStockRequests(Class<?> requestType) {
        return requestType == Stack.class ? MinimumStack.class : requestType;
    }
}
