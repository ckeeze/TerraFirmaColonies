package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBaker;
import net.ckeeze.terrafirmacolonies.api.MiscellaniousUtil;
import net.ckeeze.terrafirmacolonies.api.mixininterfaces.BakerNewVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = BuildingBaker.class)
public abstract class BuildingBakerMixin extends AbstractBuilding implements BakerNewVariables {

    @Unique
    private List<BlockPos> terrafirmacolonies$ovenPosList = new ArrayList<>();

    protected BuildingBakerMixin(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    @Override
    public void registerBlockPosition(@NotNull Block block, @NotNull BlockPos pos, @NotNull Level world) {
        if (MiscellaniousUtil.isOvenTop(block) && MiscellaniousUtil.isOvenBottom(world.getBlockState(pos.below()).getBlock()) && !this.terrafirmacolonies$ovenPosList.contains(pos)) {
            this.terrafirmacolonies$ovenPosList.add(pos);
        }
        super.registerBlockPosition(block, pos, world);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compound = super.serializeNBT();
        ListTag ovenTagList = new ListTag();
        for (BlockPos pos : this.terrafirmacolonies$ovenPosList) {
            ovenTagList.add(NbtUtils.writeBlockPos(pos));
        }
        compound.put("Ovens", ovenTagList);
        return compound;
    }

    @Override
    public void deserializeNBT(CompoundTag compound) {
        super.deserializeNBT(compound);
        ListTag ovenTagList = compound.getList("Ovens", 10);
        for (int i = 0; i < ovenTagList.size(); ++i) {
            CompoundTag ovenCompound = ovenTagList.getCompound(i);
            this.terrafirmacolonies$ovenPosList.add(NbtUtils.readBlockPos(ovenCompound));
        }
    }

    @Override
    public List<BlockPos> getOvenList() {
        return this.terrafirmacolonies$ovenPosList;
    }


}
