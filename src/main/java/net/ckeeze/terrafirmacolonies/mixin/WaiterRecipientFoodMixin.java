package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkCook;
import net.ckeeze.terrafirmacolonies.api.TFCFoodUtils;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraftforge.items.IItemHandler;
import java.util.Queue;
import java.util.function.Predicate;

@Mixin(value = EntityAIWorkCook.class, remap = false)
public abstract class WaiterRecipientFoodMixin {
    @Shadow @Final private Queue<AbstractEntityCitizen> citizenToServe;
    @Unique private ICitizenData terrafirmacolonies$recipient;

    @Inject(method = "serveFoodToCitizen", at = @At("HEAD"), require = 1)
    private void terrafirmacolonies$rememberRecipient(CallbackInfoReturnable<IAIState> cir) {
        var citizen = citizenToServe.peek();
        terrafirmacolonies$recipient = citizen == null ? null : citizen.getCitizenData();
    }

    // The serving method repeats the inventory check after walking to the guest. Its original
    // menu-only predicate must not undo the suitability check used to build the serving queue.
    @Redirect(method = "serveFoodToCitizen", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/InventoryUtils;hasItemInItemHandler(Lnet/minecraftforge/items/IItemHandler;Ljava/util/function/Predicate;)Z"), require = 1)
    private boolean terrafirmacolonies$hasUsableFood(IItemHandler inventory, Predicate<ItemStack> inMenu) {
        var data = terrafirmacolonies$recipient;
        return InventoryUtils.hasItemInItemHandler(inventory, food -> inMenu.test(food)
                && (data == null ? TFCFoodUtils.isTFCEdibleFood(food)
                : FoodUtils.canEat(food, data.getHomeBuilding(), data.getWorkBuilding())));
    }

    @Inject(method = "canEat", at = @At("RETURN"), cancellable = true, require = 1)
    private void terrafirmacolonies$checkRecipientFood(ItemStack food, AbstractEntityCitizen recipient,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            var data = recipient == null ? null : recipient.getCitizenData();
            cir.setReturnValue(data == null ? TFCFoodUtils.isTFCEdibleFood(food)
                    : FoodUtils.canEat(food, data.getHomeBuilding(), data.getWorkBuilding()));
        }
    }
}
