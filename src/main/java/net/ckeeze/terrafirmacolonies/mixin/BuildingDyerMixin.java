package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDyer;
import net.ckeeze.terrafirmacolonies.api.mixininterfaces.DyerNewVariables;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BuildingDyer.class)
public abstract class BuildingDyerMixin extends AbstractBuilding implements DyerNewVariables {

    @Unique
    private BlockPos terrafirmacolonies$Quern;

    @Unique
    private BlockPos terrafirmacolonies$Pot;

    protected BuildingDyerMixin(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    @Override
    public void registerBlockPosition(@NotNull Block block, @NotNull BlockPos pos, @NotNull Level world) {
        super.registerBlockPosition(block, pos, world);
        if (block.defaultBlockState().is(TFCBlocks.QUERN.get())) {
            terrafirmacolonies$Quern = pos;
        }
        if (block.defaultBlockState().is(TFCBlocks.POT.get())) {
            terrafirmacolonies$Pot = pos;
        }
    }

    @Override
    public BlockPos getQuernPos() {
        return this.terrafirmacolonies$Quern;
    }

    @Override
    public BlockPos getPotPos() {
        return this.terrafirmacolonies$Pot;
    }
}
