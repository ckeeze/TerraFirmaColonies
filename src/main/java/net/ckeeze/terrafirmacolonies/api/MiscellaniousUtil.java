package net.ckeeze.terrafirmacolonies.api;

import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.eerussianguy.firmalife.common.blocks.OvenType;
import net.minecraft.world.level.block.Block;

public class MiscellaniousUtil {

    public static boolean isOvenTop(Block block) {
        return block == FLBlocks.CURED_OVEN_TOP.get(OvenType.BRICK).get()
                || block == FLBlocks.CURED_OVEN_TOP.get(OvenType.RUSTIC).get()
                || block == FLBlocks.CURED_OVEN_TOP.get(OvenType.STONE).get()
                || block == FLBlocks.CURED_OVEN_TOP.get(OvenType.TILE).get();
    }

    public static boolean isOvenBottom(Block block) {
        return block == FLBlocks.CURED_OVEN_BOTTOM.get(OvenType.BRICK).get()
                || block == FLBlocks.CURED_OVEN_BOTTOM.get(OvenType.RUSTIC).get()
                || block == FLBlocks.CURED_OVEN_BOTTOM.get(OvenType.STONE).get()
                || block == FLBlocks.CURED_OVEN_BOTTOM.get(OvenType.TILE).get()
                || block == FLBlocks.INSULATED_OVEN_BOTTOM.get(OvenType.BRICK).get()
                || block == FLBlocks.INSULATED_OVEN_BOTTOM.get(OvenType.RUSTIC).get()
                || block == FLBlocks.INSULATED_OVEN_BOTTOM.get(OvenType.STONE).get()
                || block == FLBlocks.INSULATED_OVEN_BOTTOM.get(OvenType.TILE).get();
    }
}
