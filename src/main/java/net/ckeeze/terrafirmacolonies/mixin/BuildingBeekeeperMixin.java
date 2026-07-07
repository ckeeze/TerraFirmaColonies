package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBeekeeper;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = BuildingBeekeeper.class)
public abstract class BuildingBeekeeperMixin extends AbstractBuilding {

    protected BuildingBeekeeperMixin(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    /**
     * @author Ckeeze
     * @reason Increase number of hives / apiary
     */
    @Overwrite(remap = false)
    public int getMaximumHives() {
        return this.getBuildingLevel() * 4;
    }
}
