package net.ckeeze.terrafirmacolonies.mixin;

import com.ldtteam.domumornamentum.block.decorative.PanelBlock;
import com.ldtteam.domumornamentum.block.vanilla.TrapdoorBlock;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import net.dries007.tfc.common.TFCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PathfindingUtils.class, remap = false)
public class PathFindingUtilsMixin {

    @Shadow
    private static Object empty = Fluids.EMPTY.defaultFluidState();

    /**
     * @author Ckeeze
     * @reason Add TFC WATER
     */
    @Overwrite(remap = false)
    public static boolean isWater(@NotNull final BlockGetter world, final BlockPos pos, @Nullable BlockState pState, @Nullable FluidState pFluidState) {
        BlockState state = pState;
        if (state == null) {
            state = world.getBlockState(pos);
        }

        if (state.isSolid()) {
            return false;
        }
        FluidState fluidState = pFluidState;
        if (fluidState == null) {
            fluidState = state.getFluidState();
        }
        if (fluidState.is(TFCTags.Fluids.ANY_INFINITE_WATER)) {
            return true;
        }

        if (fluidState == empty || fluidState.isEmpty()) {
            return false;
        }

        if (state.getBlock() instanceof TrapdoorBlock
                || state.getBlock() instanceof PanelBlock && (!state.getValue(TrapdoorBlock.OPEN) && state.getValue(TrapdoorBlock.HALF) == Half.TOP)) {
            return false;
        }

        final Fluid fluid = fluidState.getType();
        return fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
    }
}
