package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.INonExhaustiveDeliverable;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingWareHouse;
import com.minecolonies.core.colony.requestsystem.resolvers.WarehouseConcreteRequestResolver;
import net.ckeeze.terrafirmacolonies.api.WarehouseMenuFood;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WarehouseConcreteRequestResolver.class, remap = false)
public abstract class WarehouseMenuFoodCountMixin {
    @Inject(method = "getWarehouseInternalCount", at = @At("HEAD"), cancellable = true)
    private void terrafirmacolonies$countMenuFood(BuildingWareHouse warehouse,
            IRequest<? extends IDeliverable> request, CallbackInfoReturnable<Integer> cir) {
        var food = request.getRequest();
        if (!WarehouseMenuFood.applies(food)) return;
        int leave = food instanceof INonExhaustiveDeliverable nonExhaustive ? nonExhaustive.getLeftOver() : 0;
        int target = (int) Math.min(Integer.MAX_VALUE, (long) food.getCount() + leave);
        int count = WarehouseMenuFood.count(warehouse, food::matches, target);
        cir.setReturnValue(Math.min(food.getCount(), Math.max(0, count - leave)));
    }
}
