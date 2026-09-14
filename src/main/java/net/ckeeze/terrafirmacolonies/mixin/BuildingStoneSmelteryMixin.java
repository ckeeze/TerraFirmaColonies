package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStoneSmeltery;
import net.ckeeze.terrafirmacolonies.api.MiscellaniousUtil;
import net.ckeeze.terrafirmacolonies.api.mixininterfaces.StoneSmelterNewVariables;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.SoilBlockType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(value = BuildingStoneSmeltery.class)
public abstract class BuildingStoneSmelteryMixin extends AbstractBuilding implements StoneSmelterNewVariables {

    @Unique
    private List<BlockPos> terrafirmacolonies$dryingBlock = new ArrayList<>();

    @Unique
    private BlockPos terrafirmacolonies$charcoalForge;

    @Unique
    private BlockPos terrafirmacolonies$bellow;

    protected BuildingStoneSmelteryMixin(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    @Override
    public void registerBlockPosition(@NotNull Block block, @NotNull BlockPos pos, @NotNull Level world) {
        super.registerBlockPosition(block, pos, world);
        if (MiscellaniousUtil.isTFCSand(block) && world.getBlockState(pos.above()).is(Blocks.AIR) && !getDryingBlockList().contains(pos)) {
            terrafirmacolonies$dryingBlock.add(pos);
        }
        if (terrafirmacolonies$charcoalForge == null && block.defaultBlockState().is(TFCBlocks.CHARCOAL_FORGE.get())) {
            if (world.getBlockState(pos.above().north()).is(TFCBlocks.BELLOWS.get())) {
                terrafirmacolonies$charcoalForge = pos;
                terrafirmacolonies$bellow = pos.above().north();
            }
            if (world.getBlockState(pos.above().east()).is(TFCBlocks.BELLOWS.get())) {
                terrafirmacolonies$charcoalForge = pos;
                terrafirmacolonies$bellow = pos.above().east();
            }
            if (world.getBlockState(pos.above().west()).is(TFCBlocks.BELLOWS.get())) {
                terrafirmacolonies$charcoalForge = pos;
                terrafirmacolonies$bellow = pos.above().west();
            }
            if (world.getBlockState(pos.above().south()).is(TFCBlocks.BELLOWS.get())) {
                terrafirmacolonies$charcoalForge = pos;
                terrafirmacolonies$bellow = pos.above().south();
            }
        }
    }

    @Override
    public BlockPos getCharcoalPos() {
        return this.terrafirmacolonies$charcoalForge;
    }

    @Override
    public BlockPos getBellowPos() {
        return this.terrafirmacolonies$bellow;
    }

    @Override
    public List<BlockPos> getDryingBlockList() {
        return this.terrafirmacolonies$dryingBlock;
    }

    @Override
    public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount() {
        Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> toKeep = new HashMap<>(super.getRequiredItemsAndAmount());

        toKeep.put((stack) -> ItemStack.isSameItem(TFCBlocks.SOIL.get(SoilBlockType.DRYING_BRICKS).get(SoilBlockType.Variant.SILT).get().asItem().getDefaultInstance(), stack), new Tuple<>(32, true));
        toKeep.put((stack) -> ItemStack.isSameItem(TFCBlocks.SOIL.get(SoilBlockType.DRYING_BRICKS).get(SoilBlockType.Variant.SANDY_LOAM).get().asItem().getDefaultInstance(), stack), new Tuple<>(32, true));
        toKeep.put((stack) -> ItemStack.isSameItem(TFCBlocks.SOIL.get(SoilBlockType.DRYING_BRICKS).get(SoilBlockType.Variant.SILTY_LOAM).get().asItem().getDefaultInstance(), stack), new Tuple<>(32, true));
        toKeep.put((stack) -> ItemStack.isSameItem(TFCBlocks.SOIL.get(SoilBlockType.DRYING_BRICKS).get(SoilBlockType.Variant.LOAM).get().asItem().getDefaultInstance(), stack), new Tuple<>(32, true));

        return toKeep;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compound = super.serializeNBT();
        ListTag dryingBlockTagList = new ListTag();
        for (BlockPos pos : this.terrafirmacolonies$dryingBlock) {
            dryingBlockTagList.add(NbtUtils.writeBlockPos(pos));
        }
        compound.put("DryingBlocks", dryingBlockTagList);
        compound.put("forge", NbtUtils.writeBlockPos(terrafirmacolonies$charcoalForge));
        compound.put("bellow", NbtUtils.writeBlockPos(terrafirmacolonies$bellow));
        return compound;
    }

    @Override
    public void deserializeNBT(CompoundTag compound) {
        super.deserializeNBT(compound);
        ListTag dryingBlockTagList = compound.getList("DryingBlocks", 10);
        for (int i = 0; i < dryingBlockTagList.size(); ++i) {
            CompoundTag ovenCompound = dryingBlockTagList.getCompound(i);
            this.terrafirmacolonies$dryingBlock.add(NbtUtils.readBlockPos(ovenCompound));
        }
        this.terrafirmacolonies$charcoalForge = NbtUtils.readBlockPos(compound.getCompound("forge"));
        this.terrafirmacolonies$bellow = NbtUtils.readBlockPos(compound.getCompound("bellow"));
    }

}
