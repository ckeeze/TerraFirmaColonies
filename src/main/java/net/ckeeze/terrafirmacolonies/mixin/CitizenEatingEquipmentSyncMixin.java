package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.core.entity.ai.minimal.EntityAIEatTask;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityAIEatTask.class, remap = false)
public abstract class CitizenEatingEquipmentSyncMixin {
    @Shadow @Final private EntityCitizen citizen;

    @Inject(method = "eat", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/ItemStackUtils;consumeFood(Lnet/minecraft/world/item/ItemStack;Lcom/minecolonies/api/entity/citizen/AbstractEntityCitizen;Lnet/minecraft/world/entity/player/Inventory;)V",
            shift = At.Shift.AFTER), require = 1)
    private void terrafirmacolonies$syncHandAfterEating(CallbackInfoReturnable<IState> cir) {
        // consumeFood shrinks the same stack referenced by the hand. On the last bite,
        // setItemSlot sees empty -> empty and misses the change from the client's last view.
        // Queue the normal MineColonies equipment broadcast after the hand is cleared.
        citizen.markEquipmentDirty();
    }
}
