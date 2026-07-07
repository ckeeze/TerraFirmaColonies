package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingChickenHerder;
import com.minecolonies.core.colony.jobs.JobChickenHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.EntityAIWorkChickenHerder;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.dries007.tfc.common.blockentities.NestBoxBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EntityAIWorkChickenHerder.class)
public abstract class EntityAIWorkChickenWorkerMixin extends AbstractEntityAIHerder<JobChickenHerder, BuildingChickenHerder> {
    @Unique
    int terrafirmacolonies$maxNestsPerLevel = 2;
    @Unique
    List<BlockPos> terrafirmacolonies$nestPosList = new ArrayList<>(List.of());

    public EntityAIWorkChickenWorkerMixin(@NotNull JobChickenHerder job) {
        super(job);
    }

    @Override
    public IAIState decideWhatToDo() {
        //Breeder IAIState is repurposed for egg collection
        IAIState result = super.decideWhatToDo();
        if (((terrafirmacolonies$getNestWithEggs() != null) || terrafirmacolonies$AreNestsBroken()) && result.equals(AIWorkerState.START_WORKING)) {
            return AIWorkerState.HERDER_BREED;
        }
        return result;
    }

    /**
     * @author Ckeeze
     * @reason Gathering eggs from nests
     */
    @Override
    @Overwrite(remap = false)
    public IAIState breedAnimals() {
        //Entirely new code
        if (terrafirmacolonies$AreNestsBroken()) {
            terrafirmacolonies$getNestPosList();
        }
        @Nullable
        BlockPos eggpos = terrafirmacolonies$getNestWithEggs();
        if (eggpos != null) {
            //Tested behavior walkToPos needs to be negated
            if (!EntityNavigationUtils.walkToPos(this.worker, eggpos, 2, true)) {
            } else {
                terrafirmacolonies$ejectEggsFromNest(eggpos);
            }
        }
        return AIWorkerState.DECIDE;
    }

    @Override
    public double getButcheringAttackDamage() {
        return 5.0F + ((double) this.getPrimarySkillLevel() / (double) 10.0F);
    }

    @Unique
    private int terrafirmacolonies$getMaxNests() {
        return this.building.getBuildingLevel() * terrafirmacolonies$maxNestsPerLevel;
    }

    @Unique
    private void terrafirmacolonies$getNestPosList() {
        terrafirmacolonies$nestPosList.clear();
        BlockPos startpos = this.building.getPosition();
        for (int x = startpos.getX() - 10; x < startpos.getX() + 10; ++x) {
            for (int z = startpos.getZ() - 10; z < startpos.getZ() + 10; ++z) {
                for (int y = startpos.getY() - 3; y < startpos.getY() + 3; ++y) {
                    if (world.getBlockState(new BlockPos(x, y, z)).is(TFCBlocks.NEST_BOX.get()) && terrafirmacolonies$nestPosList.size() < this.terrafirmacolonies$getMaxNests()) {
                        terrafirmacolonies$nestPosList.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
    }

    @Unique
    private boolean terrafirmacolonies$AreNestsBroken() {
        for (BlockPos pos : terrafirmacolonies$nestPosList) {
            if (!(this.world.getBlockEntity(pos) instanceof NestBoxBlockEntity)) {
                return true;
            }
        }
        return terrafirmacolonies$nestPosList.isEmpty();
    }

    @Unique
    private @Nullable BlockPos terrafirmacolonies$getNestWithEggs() {
        for (BlockPos pos : terrafirmacolonies$nestPosList) {
            BlockEntity Nest = this.world.getBlockEntity(pos);
            assert Nest != null;
            if (Nest instanceof NestBoxBlockEntity container) {
                String NBT = container.serializeNBT().toString();
                if (!NBT.contains("entity") && NBT.contains("egg")) {
                    return pos;
                }
            }
        }
        return null;
    }

    @Unique
    private void terrafirmacolonies$ejectEggsFromNest(BlockPos pos) {
        BlockEntity Nest = this.world.getBlockEntity(pos);
        assert Nest != null;
        if (Nest instanceof NestBoxBlockEntity container) {
            container.ejectInventory();
            container.clearContent();
        }
    }

}
