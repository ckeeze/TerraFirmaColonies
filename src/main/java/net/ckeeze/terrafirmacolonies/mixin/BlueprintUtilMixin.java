package net.ckeeze.terrafirmacolonies.mixin;

import com.ldtteam.structurize.api.util.BlockPosUtil;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.blueprints.v1.BlueprintUtil;
import net.ckeeze.terrafirmacolonies.Config;
import net.ckeeze.terrafirmacolonies.Config.Replacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(BlueprintUtil.class)
public class BlueprintUtilMixin {

    @Inject(
        method = "readBlueprintFromNBT",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private static void readBlueprintFromNBT(CompoundTag nbtTag, CallbackInfoReturnable<Blueprint> ci) {
        Blueprint blueprint = ci.getReturnValue();

        Map<Block, Replacer> replacements = Config.getStylepackBlockReplacements();
        if (replacements.size() > 0) {
            List<Pair<BlockPos, BlockState>> blocksToReplace = new ArrayList<>();
            MutableBlockPos pos = new MutableBlockPos();
            for (int x = blueprint.getMinX(); x <= blueprint.getMaxX(); x++) {
                for (int y = blueprint.getMinBuildHeight(); y <= blueprint.getMaxBuildHeight(); y++) {
                    for (int z = blueprint.getMinZ(); z <= blueprint.getMaxZ(); z++) {
                        pos.set(x, y, z);
                        BlockState prev = blueprint.getBlockState(pos);
                        Replacer replacer = replacements.get(prev.getBlock());
                        if (replacer != null) {
                            BlockState next = replacer.apply(prev);
                            blocksToReplace.add(Pair.of(pos.immutable(), next));
                        }
                    }
                }
            }
            for (Pair<BlockPos, BlockState> e : blocksToReplace) {
                blueprint.addBlockState(e.getLeft(), e.getRight());
            }

            if (nbtTag.getAllKeys().contains("optional_data")) {
                CompoundTag optionalTag = nbtTag.getCompound("optional_data");
                if (optionalTag.getAllKeys().contains("structurize")) {
                    CompoundTag structurizeTag = optionalTag.getCompound("structurize");
                    BlockPos offsetPos = BlockPosUtil.readFromNBT(structurizeTag, "primary_offset");
                    blueprint.setCachePrimaryOffset(offsetPos);
                }
            }
        }

        ci.setReturnValue(blueprint);
    }

}
