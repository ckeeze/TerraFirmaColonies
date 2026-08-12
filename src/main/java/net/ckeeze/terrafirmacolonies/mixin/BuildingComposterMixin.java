package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingComposter;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = BuildingComposter.class)
public abstract class BuildingComposterMixin extends AbstractBuilding {

    @Shadow
    @Final
    private List<BlockPos> barrels;

    protected BuildingComposterMixin(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    /**
     * @author Ckeeze
     * @reason registering TFC composter
     */
    @Override
    @Overwrite(remap = false)
    public void registerBlockPosition(@NotNull Block block, @NotNull BlockPos pos, @NotNull Level world) {
        super.registerBlockPosition(block, pos, world);
        if (block == TFCBlocks.COMPOSTER.get() && !this.barrels.contains(pos)) {
            this.barrels.add(pos);
        }
    }
}
