package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkCook;
import net.ckeeze.terrafirmacolonies.api.FoodContainerReturns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Queue;

@Mixin(value = EntityAIWorkCook.class, remap = false)
public abstract class WaiterFoodContainerMixin {
    @Shadow @Final private Queue<AbstractEntityCitizen> citizenToServe;
    @Unique private AbstractEntityCitizen terrafirmacolonies$containerRecipient;

    @Inject(method = "serveFoodToCitizen", at = @At("HEAD"), require = 1)
    private void terrafirmacolonies$rememberContainerRecipient(CallbackInfoReturnable<IAIState> cir) {
        // The native method polls the queue before it consumes the portion.
        terrafirmacolonies$containerRecipient = citizenToServe.peek();
    }

    @Inject(method = "serveFoodToCitizen", at = @At("RETURN"), require = 1)
    private void terrafirmacolonies$clearContainerRecipient(CallbackInfoReturnable<IAIState> cir) {
        terrafirmacolonies$containerRecipient = null;
    }

    @Redirect(method = "serveFoodToCitizen", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/FoodUtils;getFoodValue(Lnet/minecraft/world/item/ItemStack;Lcom/minecolonies/api/entity/citizen/AbstractEntityCitizen;)D"), require = 1)
    private double terrafirmacolonies$returnDirectMealContainer(ItemStack portion, AbstractEntityCitizen worker) {
        double value = FoodUtils.getFoodValue(portion, worker);
        // One portion has already been extracted. This fallback feeds directly and
        // never calls ItemStackUtils.consumeFood.
        AbstractEntityCitizen recipient = terrafirmacolonies$containerRecipient;
        if (recipient != null) {
            FoodContainerReturns.returnServedContainer(FoodContainerReturns.getContainer(portion), recipient, worker);
        }
        return value;
    }
}
