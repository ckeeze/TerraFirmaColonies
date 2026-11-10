package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.core.colony.buildings.modules.FurnaceUserModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIUsesFurnace;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkCook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ready-food service must remain available in TFC restaurants without vanilla furnaces. */
@Mixin(value = EntityAIWorkCook.class, remap = false)
public abstract class EntityAIWorkCookMixin extends AbstractEntityAIUsesFurnace<JobCook, BuildingCook> {
    protected EntityAIWorkCookMixin(JobCook job) {
        super(job);
    }

    @Inject(method = "startWorking", at = @At("HEAD"), cancellable = true)
    private void terrafirmacolonies$serveWithoutVanillaFurnace(CallbackInfoReturnable<IAIState> cir) {
        if (!building.getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces().isEmpty()) {
            return;
        }

        // Preserve arrival and the cook's existing menu, food and recipient checks.
        if (!walkToBuilding()) {
            cir.setReturnValue(getState());
            return;
        }
        worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        cir.setReturnValue(checkForImportantJobs());
    }
}
