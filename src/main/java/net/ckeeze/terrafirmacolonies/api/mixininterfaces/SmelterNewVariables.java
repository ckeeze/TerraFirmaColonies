package net.ckeeze.terrafirmacolonies.api.mixininterfaces;

import net.minecraft.core.BlockPos;

public interface SmelterNewVariables {

    BlockPos getCharcoalPos();

    void setCharcoalPos(BlockPos pos);

    BlockPos getForgeBellowPos();

    void setForgeBellowPos(BlockPos pos);

    BlockPos getBlastFurnaceBellowPosUpper();

    void setBlastFurnaceBellowPosUpper(BlockPos pos);

    BlockPos getBlastFurnaceBellowPosLower();

    void setBlastFurnaceBellowPosLower(BlockPos pos);

    BlockPos getAnvilPos();

    void setAnvilPos(BlockPos pos);

    BlockPos getBloomeryPos();

    void setBloomeryPos(BlockPos pos);

    BlockPos getBlastFurnacePos();

    void setBlastFurnacePos(BlockPos pos);
}
