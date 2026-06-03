package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingSwineHerder;
import com.minecolonies.core.colony.jobs.JobSwineHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.EntityAIWorkSwineHerder;
import net.dries007.tfc.common.items.Food;
import net.dries007.tfc.common.items.TFCItems;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = EntityAIWorkSwineHerder.class, remap = false)
public abstract class EntityAIWorkSwineHerderMixin extends AbstractEntityAIHerder<JobSwineHerder, BuildingSwineHerder> {

    public EntityAIWorkSwineHerderMixin(@NotNull JobSwineHerder job) {
        super(job);
    }


    //Overrides

    /**
     * @author Ckeeze
     * @reason Increase butchering damage to improve worker Reliability
     */
    @Override
    @Overwrite(remap = false)
    public double getButcheringAttackDamage() {
        return 5.0F + ((double) this.getPrimarySkillLevel() / (double) 10.0F);
    }

    /**
     * @author Ckeeze
     * @reason Detect TFC items instead of vanilla
     */
    @Override
    @Overwrite
    protected void updateRenderMetaData() {
        String renderMeta = this.getState() == AIWorkerState.IDLE ? "" : "working";
        if (this.worker.getCitizenInventoryHandler().hasItemInInventory(TFCItems.FOOD.get(Food.CARROT).get())) {
            renderMeta = renderMeta + "carrot";
        }

        this.worker.setRenderMetadata(renderMeta);
    }

    //Uniques
}
