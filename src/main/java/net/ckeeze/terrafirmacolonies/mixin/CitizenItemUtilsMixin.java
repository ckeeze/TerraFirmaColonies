package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.ckeeze.terrafirmacolonies.api.ExtendedWorkerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CitizenItemUtils.class, remap = false)
public class CitizenItemUtilsMixin {

    @Redirect(
        method = "hitBlockWithToolInHand(Lcom/minecolonies/api/entity/citizen/AbstractEntityCitizen;Lnet/minecraft/core/BlockPos;Z)V",
        at = @At(value = "INVOKE", target = "Lcom/minecolonies/api/util/WorldUtil;removeBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Z)Z")
    )
    private static boolean preventCollapse(LevelAccessor world, BlockPos pos, boolean isMoving) {
        return ExtendedWorkerUtil.setBlockWithoutCollapse(world, pos, Blocks.AIR.defaultBlockState());
    }
}
