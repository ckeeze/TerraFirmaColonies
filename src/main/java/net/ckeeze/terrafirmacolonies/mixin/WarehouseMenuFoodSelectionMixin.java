package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.core.colony.requestsystem.resolvers.core.AbstractWarehouseRequestResolver;
import com.minecolonies.core.tileentities.TileEntityWareHouse;
import net.ckeeze.terrafirmacolonies.api.WarehouseMenuFood;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(value = AbstractWarehouseRequestResolver.class, remap = false)
public abstract class WarehouseMenuFoodSelectionMixin {
    @Redirect(method = {"attemptResolveRequest", "getFollowupRequestForCompletion"},
            at = @At(value = "INVOKE", target = "Lcom/minecolonies/core/tileentities/TileEntityWareHouse;getMatchingItemStacksInWarehouse(Ljava/util/function/Predicate;)Ljava/util/List;"))
    private List<Tuple<ItemStack, BlockPos>> terrafirmacolonies$selectMenuFood(TileEntityWareHouse warehouse,
            Predicate<ItemStack> matches, IRequestManager manager, IRequest<? extends IDeliverable> request) {
        return WarehouseMenuFood.applies(request.getRequest())
                ? WarehouseMenuFood.select(warehouse.getBuilding(), matches)
                : warehouse.getMatchingItemStacksInWarehouse(matches);
    }
}
