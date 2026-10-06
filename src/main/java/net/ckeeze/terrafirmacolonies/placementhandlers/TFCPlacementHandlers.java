package net.ckeeze.terrafirmacolonies.placementhandlers;

import com.ldtteam.structurize.placement.IPlacementContext;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
import net.dries007.tfc.common.blocks.StainedWattleBlock;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.ThatchBedBlock;
import net.dries007.tfc.common.blocks.WattleBlock;
import net.dries007.tfc.common.blocks.devices.CharcoalForgeBlock;
import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.dries007.tfc.common.blocks.devices.GrillBlock;
import net.dries007.tfc.common.blocks.devices.PotBlock;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.common.blocks.rock.RockAnvilBlock;
import net.dries007.tfc.common.items.HideItemType;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ldtteam.structurize.api.util.constant.Constants.UPDATE_FLAG;

public class TFCPlacementHandlers {

    //Thatch bed placement, needs 2 thatch block and 1 large hide
    static class ThatchBedPlacementHandler implements IPlacementHandler {
        @Override
        public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState) {
            return blockState.getBlock() instanceof ThatchBedBlock;
        }

        @Override
        public IPlacementHandler.ActionProcessingResult handle(
                @NotNull Level world,
                @NotNull BlockPos pos,
                @NotNull BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            if (blockState.getValue(BedBlock.PART) == BedPart.HEAD) {
                Direction facing = blockState.getValue(BedBlock.FACING);
                world.setBlock(pos.relative(facing.getOpposite()), blockState.setValue(BedBlock.PART, BedPart.FOOT), 3);
                world.setBlock(pos, blockState.setValue(BedBlock.PART, BedPart.HEAD), 3);
                if (tileEntityData != null) {
                    PlacementHandlers.handleTileEntityPlacement(tileEntityData, world, pos, placementContext.getRotationMirror());
                    PlacementHandlers.handleTileEntityPlacement(tileEntityData, world, pos.relative(facing.getOpposite()), placementContext.getRotationMirror());
                }

                return ActionProcessingResult.SUCCESS;
            } else {
                return ActionProcessingResult.PASS;
            }
        }

        @Override
        public List<ItemStack> getRequiredItems(
                Level world,
                BlockPos pos,
                BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            List<ItemStack> itemList = new ArrayList<>();
            if (blockState.getValue(BedBlock.PART) == BedPart.HEAD) {
                itemList.add(new ItemStack(TFCItems.HIDES.get(HideItemType.RAW).get(HideItemType.Size.LARGE).get()));
                itemList.add(new ItemStack(TFCBlocks.THATCH.get().asItem(), 2));
            }
            return itemList;
        }

        @Override
        public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, @NotNull IPlacementContext structureHandler) {
            return worldState.equals(blueprintState);
        }
    }

    // Forge needs 7 charcoal
    static class ForgePlacementHandler implements IPlacementHandler {
        @Override
        public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState) {
            return blockState.getBlock() instanceof CharcoalForgeBlock;
        }

        @Override
        public ActionProcessingResult handle(
                @NotNull Level world,
                @NotNull BlockPos pos,
                @NotNull BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            world.setBlock(pos, blockState.getBlock().defaultBlockState(), UPDATE_FLAG);
            return ActionProcessingResult.SUCCESS;
        }

        @Override
        public List<ItemStack> getRequiredItems(
                Level world,
                BlockPos pos,
                BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            return List.of(new ItemStack(Items.CHARCOAL, 7));
        }

        @Override
        public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, @NotNull IPlacementContext structureHandler) {
            return worldState.equals(blueprintState);
        }
    }

    //Anvils need raw rock.
    static class StoneAnvilPlacementHandler implements IPlacementHandler {

        private final Map<Block, Block> anvilToRock = Map.ofEntries(
                Map.entry(TFCBlocks.ROCK_ANVILS.get(Rock.DACITE).get(), TFCBlocks.ROCK_BLOCKS.get(Rock.DACITE).get(Rock.BlockType.RAW).get()),
                Map.entry(TFCBlocks.ROCK_ANVILS.get(Rock.ANDESITE).get(), TFCBlocks.ROCK_BLOCKS.get(Rock.ANDESITE).get(Rock.BlockType.RAW).get()),
                Map.entry(TFCBlocks.ROCK_ANVILS.get(Rock.BASALT).get(), TFCBlocks.ROCK_BLOCKS.get(Rock.BASALT).get(Rock.BlockType.RAW).get()),
                Map.entry(TFCBlocks.ROCK_ANVILS.get(Rock.GRANITE).get(), TFCBlocks.ROCK_BLOCKS.get(Rock.GRANITE).get(Rock.BlockType.RAW).get()),
                Map.entry(TFCBlocks.ROCK_ANVILS.get(Rock.GABBRO).get(), TFCBlocks.ROCK_BLOCKS.get(Rock.GABBRO).get(Rock.BlockType.RAW).get()),
                Map.entry(TFCBlocks.ROCK_ANVILS.get(Rock.RHYOLITE).get(), TFCBlocks.ROCK_BLOCKS.get(Rock.RHYOLITE).get(Rock.BlockType.RAW).get()),
                Map.entry(TFCBlocks.ROCK_ANVILS.get(Rock.DIORITE).get(), TFCBlocks.ROCK_BLOCKS.get(Rock.DIORITE).get(Rock.BlockType.RAW).get())
        );

        @Override
        public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState) {
            return blockState.getBlock() instanceof RockAnvilBlock;
        }

        @Override
        public ActionProcessingResult handle(
                @NotNull Level world,
                @NotNull BlockPos pos,
                @NotNull BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            world.setBlock(pos, blockState.getBlock().defaultBlockState(), UPDATE_FLAG);
            return ActionProcessingResult.SUCCESS;
        }

        @Override
        public List<ItemStack> getRequiredItems(
                Level world,
                BlockPos pos,
                BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            return List.of(new ItemStack(anvilToRock.get(blockState.getBlock()).asItem()));
        }

        @Override
        public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, @NotNull IPlacementContext structureHandler) {
            return worldState.equals(blueprintState);
        }
    }

    static class FirePitPlacementHandler implements IPlacementHandler {

        @Override
        public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState) {
            return blockState.getBlock() instanceof FirepitBlock;
        }

        @Override
        public ActionProcessingResult handle(
                @NotNull Level world,
                @NotNull BlockPos pos,
                @NotNull BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            world.setBlock(pos, blockState.getBlock().defaultBlockState(), UPDATE_FLAG);
            return ActionProcessingResult.SUCCESS;
        }

        @Override
        public List<ItemStack> getRequiredItems(
                Level world,
                BlockPos pos,
                BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            List<ItemStack> list = new ArrayList<>(List.of(
                    new ItemStack(Items.STICK, 3),
                    new ItemStack(TFCItems.STRAW.get(), 1)
            ));
            if (blockState.getBlock() instanceof PotBlock) {
                list.add(new ItemStack(TFCItems.POT.get()));
            }
            if (blockState.getBlock() instanceof GrillBlock) {
                list.add(new ItemStack(TFCItems.WROUGHT_IRON_GRILL.get()));
            }
            return list;
        }

        @Override
        public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, @NotNull IPlacementContext structureHandler) {
            return worldState.equals(blueprintState);
        }
    }

    //Stained Wattle
    static class StainedWattlePlacementHandler implements IPlacementHandler {

        private final Map<Block, Item> stainedWattleToDye = Map.ofEntries(
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.WHITE).get(), Items.WHITE_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.LIGHT_GRAY).get(), Items.LIGHT_GRAY_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.GRAY).get(), Items.GRAY_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.BLACK).get(), Items.BLACK_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.YELLOW).get(), Items.YELLOW_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.BLUE).get(), Items.BLUE_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.BROWN).get(), Items.BROWN_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.LIGHT_BLUE).get(), Items.LIGHT_BLUE_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.GREEN).get(), Items.GREEN_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.LIME).get(), Items.LIME_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.MAGENTA).get(), Items.MAGENTA_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.PINK).get(), Items.PINK_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.PURPLE).get(), Items.PURPLE_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.RED).get(), Items.RED_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.CYAN).get(), Items.CYAN_DYE),
                Map.entry(TFCBlocks.STAINED_WATTLE.get(DyeColor.ORANGE).get(), Items.ORANGE_DYE));

        @Override
        public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState) {
            return blockState.getBlock() instanceof StainedWattleBlock;
        }

        @Override
        public ActionProcessingResult handle(
                @NotNull Level world,
                @NotNull BlockPos pos,
                @NotNull BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            world.setBlock(pos, blockState.getBlock().defaultBlockState(), UPDATE_FLAG);
            return ActionProcessingResult.SUCCESS;
        }

        @Override
        public List<ItemStack> getRequiredItems(
                Level world,
                BlockPos pos,
                BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            List<ItemStack> list = new ArrayList<>(List.of(
                    new ItemStack(TFCBlocks.WATTLE.get().asItem(), 1),
                    new ItemStack(TFCItems.DAUB.get(), 1),
                    new ItemStack(Items.STICK, 4),
                    new ItemStack(stainedWattleToDye.get(blockState.getBlock()), 1)
            ));
            if (blockState.getValue(StainedWattleBlock.TOP)) {
                list.add(new ItemStack(Items.STICK));
            }
            if (blockState.getValue(StainedWattleBlock.BOTTOM)) {
                list.add(new ItemStack(Items.STICK));
            }
            if (blockState.getValue(StainedWattleBlock.LEFT)) {
                list.add(new ItemStack(Items.STICK));
            }
            if (blockState.getValue(StainedWattleBlock.RIGHT)) {
                list.add(new ItemStack(Items.STICK));
            }
            return list;
        }

        @Override
        public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, @NotNull IPlacementContext structureHandler) {
            return worldState.equals(blueprintState);
        }
    }


    //Unstained Wattle
    static class UnstainedWattlePlacementHandler implements IPlacementHandler {

        @Override
        public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState) {
            return blockState.is(TFCBlocks.UNSTAINED_WATTLE.get());
        }

        @Override
        public ActionProcessingResult handle(
                @NotNull Level world,
                @NotNull BlockPos pos,
                @NotNull BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            world.setBlock(pos, blockState.getBlock().defaultBlockState(), UPDATE_FLAG);
            return ActionProcessingResult.SUCCESS;
        }

        @Override
        public List<ItemStack> getRequiredItems(
                Level world,
                BlockPos pos,
                BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            List<ItemStack> list = new ArrayList<>(List.of(
                    new ItemStack(TFCBlocks.WATTLE.get().asItem(), 1),
                    new ItemStack(TFCItems.DAUB.get(), 1),
                    new ItemStack(Items.STICK, 4)
            ));
            if (blockState.getValue(StainedWattleBlock.TOP)) {
                list.add(new ItemStack(Items.STICK));
            }
            if (blockState.getValue(StainedWattleBlock.BOTTOM)) {
                list.add(new ItemStack(Items.STICK));
            }
            if (blockState.getValue(StainedWattleBlock.LEFT)) {
                list.add(new ItemStack(Items.STICK));
            }
            if (blockState.getValue(StainedWattleBlock.RIGHT)) {
                list.add(new ItemStack(Items.STICK));
            }
            return list;
        }

        @Override
        public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, @NotNull IPlacementContext structureHandler) {
            return worldState.equals(blueprintState);
        }
    }

    //Undaubed Wattle
    static class UndaubedWattlePlacementHandler implements IPlacementHandler {

        @Override
        public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState) {
            return blockState.is(TFCBlocks.WATTLE.get());
        }

        @Override
        public ActionProcessingResult handle(
                @NotNull Level world,
                @NotNull BlockPos pos,
                @NotNull BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            world.setBlock(pos, blockState.getBlock().defaultBlockState(), UPDATE_FLAG);
            return ActionProcessingResult.SUCCESS;
        }

        @Override
        public List<ItemStack> getRequiredItems(
                Level world,
                BlockPos pos,
                BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            List<ItemStack> list = new ArrayList<>(List.of(
                    new ItemStack(TFCBlocks.WATTLE.get().asItem(), 1)
            ));
            if (blockState.getValue(WattleBlock.WOVEN)) {
                list.add(new ItemStack(Items.STICK, 4));
            }
            if (blockState.getValue(StainedWattleBlock.TOP)) {
                list.add(new ItemStack(Items.STICK));
            }
            if (blockState.getValue(StainedWattleBlock.BOTTOM)) {
                list.add(new ItemStack(Items.STICK));
            }
            if (blockState.getValue(StainedWattleBlock.LEFT)) {
                list.add(new ItemStack(Items.STICK));
            }
            if (blockState.getValue(StainedWattleBlock.RIGHT)) {
                list.add(new ItemStack(Items.STICK));
            }
            return list;
        }

        @Override
        public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, @NotNull IPlacementContext structureHandler) {
            return worldState.equals(blueprintState);
        }
    }

    //TFCTorcj
    static class TFCTorchPlacementHandler implements IPlacementHandler {

        @Override
        public boolean canHandle(final Level world, final BlockPos pos, final BlockState blockState) {
            return blockState.is(TFCBlocks.DEAD_TORCH.get());
        }

        @Override
        public ActionProcessingResult handle(
                @NotNull Level world,
                @NotNull BlockPos pos,
                @NotNull BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            world.setBlock(pos, blockState.getBlock().defaultBlockState(), UPDATE_FLAG);
            return ActionProcessingResult.SUCCESS;
        }

        @Override
        public List<ItemStack> getRequiredItems(
                Level world,
                BlockPos pos,
                BlockState blockState,
                @Nullable CompoundTag tileEntityData,
                @NotNull IPlacementContext placementContext
        ) {
            return new ArrayList<>(List.of(
                    new ItemStack(TFCItems.TORCH.get(), 1)
            ));
        }

        @Override
        public boolean doesWorldStateMatchBlueprintState(BlockState worldState, BlockState blueprintState, Tuple<BlockEntity, CompoundTag> blockEntityData, @NotNull IPlacementContext structureHandler) {
            return worldState.equals(blueprintState);
        }
    }
}