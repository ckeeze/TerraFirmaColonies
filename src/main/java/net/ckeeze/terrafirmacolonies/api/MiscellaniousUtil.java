package net.ckeeze.terrafirmacolonies.api;

import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.eerussianguy.firmalife.common.blocks.OvenType;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
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

    public static boolean isTFCSand(Block block) {
        return block == TFCBlocks.SAND.get(SandBlockType.YELLOW).get()
                || block == TFCBlocks.SAND.get(SandBlockType.RED).get()
                || block == TFCBlocks.SAND.get(SandBlockType.GREEN).get()
                || block == TFCBlocks.SAND.get(SandBlockType.BROWN).get()
                || block == TFCBlocks.SAND.get(SandBlockType.BLACK).get()
                || block == TFCBlocks.SAND.get(SandBlockType.PINK).get();
    }
}
