package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFisherman;
import com.minecolonies.core.colony.jobs.JobFisherman;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAISkill;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFisherman;
import net.dries007.tfc.common.TFCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = EntityAIWorkFisherman.class, remap = false)
public abstract class EntityAIWorkFishermanMixin extends AbstractEntityAISkill<JobFisherman, BuildingFisherman> {

    @Shadow
    protected abstract int getRodSlot();

    @Shadow
    protected abstract void equipRod();

    protected EntityAIWorkFishermanMixin(@NotNull JobFisherman job) {
        super(job);
    }

    /**
     * @author Ckeeze
     * @reason inserting terrafirmacolonies$isWaterinRange()
     */
    @Overwrite(remap = false)
    private IAIState isReadyToFish() {
        int rodSlot = this.getRodSlot();
        if (rodSlot == -1) {
            this.worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStackUtils.EMPTY);
            return AIWorkerState.PREPARING;
        } else if (!terrafirmacolonies$isWaterinRange(this.world, (int) this.worker.getX(), (int) this.worker.getY(), (int) this.worker.getZ())) {
            return AIWorkerState.FISHERMAN_WALKING_TO_WATER;
        } else if (this.worker.getMainHandItem() != null && ItemStackUtils.compareItemStacksIgnoreStackSize(this.worker.getMainHandItem(), this.worker.getItemHandlerCitizen().getStackInSlot(rodSlot), false, true)) {
            return null;
        } else {
            this.equipRod();
            return this.getState();
        }
    }

    @Unique
    private static boolean terrafirmacolonies$isWaterinRange(@NotNull Level world, int posX, int posY, int posZ) {
        for (int x = posX - 3; x < posX + 3; ++x) {
            for (int z = posZ - 3; z < posZ + 3; ++z) {
                for (int y = posY - 3; y < posY + 3; ++y) {
                    if (world.getBlockState(new BlockPos(x, y, z)).getFluidState().is(TFCTags.Fluids.ANY_WATER)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
