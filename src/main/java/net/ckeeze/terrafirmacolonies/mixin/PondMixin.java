package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.util.Pond;
import net.dries007.tfc.common.TFCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(value = Pond.class)
public class PondMixin {

    /**
     * @author Ckeeze
     * @reason Adding TFC Saltwater
     */
    @Overwrite(remap = false)
    public static Pond.PondState checkWaterForFishing(BlockGetter world, BlockPos pos) {
        Pond.PondState pondState = Pond.PondState.INVALID;
        BlockState state = world.getBlockState(pos);
        if (!state.isAir() && !state.is(Blocks.LILY_PAD)) {
            FluidState fluidstate = state.getFluidState();
            //Changed from Vanilla Water fluid
            if (fluidstate.is(TFCTags.Fluids.ANY_WATER)) {
                if (fluidstate.isSource()) {
                    pondState = Pond.PondState.VALID;
                } else {
                    pondState = Pond.PondState.SUBOPTIMAL;
                }
            }
        }

        return pondState;
    }
}
