package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingRabbitHutch;
import com.minecolonies.core.colony.jobs.JobRabbitHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.EntityAIWorkRabbitHerder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityAIWorkRabbitHerder.class, remap = false)
public abstract class EntityAIWorkRabbitHerderMixin extends AbstractEntityAIHerder<JobRabbitHerder, BuildingRabbitHutch> {

    public EntityAIWorkRabbitHerderMixin(@NotNull JobRabbitHerder job) {
        super(job);
    }

    @Override
    public double getButcheringAttackDamage() {
        return 5.0F + ((double) this.getPrimarySkillLevel() / (double) 10.0F);
    }

}
