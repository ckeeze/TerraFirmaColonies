package net.ckeeze.terrafirmacolonies.api.mixininterfaces;

import net.minecraft.core.BlockPos;

import java.util.List;

public interface StoneSmelterNewVariables {

    BlockPos getCharcoalPos();

    BlockPos getBellowPos();

    List<BlockPos> getDryingBlockList();
}
