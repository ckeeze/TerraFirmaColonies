package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkCook;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A successful partial transfer must count only the portions actually handed over. */
@Mixin(value = EntityAIWorkCook.class, remap = false)
public abstract class WaiterServingStatisticsMixin {
    @Unique private int terrafirmacolonies$servedCount = -1;

    @Inject(method = "serveFoodToCitizen", at = @At("HEAD"), require = 1)
    private void terrafirmacolonies$resetServedCount(CallbackInfoReturnable<IAIState> cir) {
        terrafirmacolonies$servedCount = -1;
    }

    @Redirect(method = "serveFoodToCitizen", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/InventoryUtils;transferXOfItemStackIntoNextFreeSlotInItemHandler(Lnet/minecraftforge/items/IItemHandler;IILnet/minecraftforge/items/IItemHandler;)Z"), require = 1)
    private boolean terrafirmacolonies$countTransferredPortions(IItemHandler source, int slot, int requested, IItemHandler target) {
        int before = source.getStackInSlot(slot).getCount();
        boolean transferred = InventoryUtils.transferXOfItemStackIntoNextFreeSlotInItemHandler(source, slot, requested, target);
        // This synchronous transfer extracts from the selected source slot, including partial success.
        terrafirmacolonies$servedCount = Math.max(0, before - source.getStackInSlot(slot).getCount());
        return transferred;
    }

    @ModifyArg(method = "serveFoodToCitizen", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/colony/managers/interfaces/IStatisticsManager;incrementBy(Ljava/lang/String;II)V"), index = 1, require = 1)
    private int terrafirmacolonies$countActualColonyServings(int requested) {
        return terrafirmacolonies$servedCount < 0 ? requested : terrafirmacolonies$servedCount;
    }

    @ModifyArg(method = "serveFoodToCitizen", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/StatsUtil;trackStatByName(Lcom/minecolonies/api/colony/buildings/IBuilding;Ljava/lang/String;Ljava/lang/String;I)V"), index = 3, require = 1)
    private int terrafirmacolonies$countActualRestaurantServings(int requested) {
        return terrafirmacolonies$servedCount < 0 ? requested : terrafirmacolonies$servedCount;
    }
}
