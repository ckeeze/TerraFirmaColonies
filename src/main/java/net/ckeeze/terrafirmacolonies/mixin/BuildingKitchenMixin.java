package net.ckeeze.terrafirmacolonies.mixin;

import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingKitchen;
import net.ckeeze.terrafirmacolonies.api.MiscellaniousUtil;
import net.ckeeze.terrafirmacolonies.api.mixininterfaces.ChefNewVaraibles;
import net.dries007.tfc.common.blocks.TFCBlocks;
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

@Mixin(value = BuildingKitchen.class)
public abstract class BuildingKitchenMixin extends AbstractBuilding implements ChefNewVaraibles {

    @Unique
    private List<BlockPos> terrafirmacolonies$ovenPosList = new ArrayList<>();

    @Unique
    private BlockPos terrafirmacolonies$pot;

    @Unique
    private BlockPos terrafirmacolonies$VAT;

    protected BuildingKitchenMixin(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    @Override
    public void registerBlockPosition(@NotNull Block block, @NotNull BlockPos pos, @NotNull Level world) {
        super.registerBlockPosition(block, pos, world);
        if (MiscellaniousUtil.isOvenTop(block) && MiscellaniousUtil.isOvenBottom(world.getBlockState(pos.below()).getBlock()) && !this.terrafirmacolonies$ovenPosList.contains(pos)) {
            this.terrafirmacolonies$ovenPosList.add(pos);
        }

        if (block.defaultBlockState().is(TFCBlocks.POT.get())) {
            terrafirmacolonies$pot = pos;
        }

        if (block.defaultBlockState().is(FLBlocks.VAT.get())) {
            terrafirmacolonies$VAT = pos;
        }

    }

    @Override
    public List<BlockPos> getOvenList() {
        return this.terrafirmacolonies$ovenPosList;
    }

    @Override
    public BlockPos getPotPos() {
        return this.terrafirmacolonies$pot;
    }

    @Override
    public BlockPos getVatPos() {
        return this.terrafirmacolonies$pot;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compound = super.serializeNBT();
        ListTag ovenTagList = new ListTag();
        for (BlockPos pos : this.terrafirmacolonies$ovenPosList) {
            ovenTagList.add(NbtUtils.writeBlockPos(pos));
        }
        compound.put("Ovens", ovenTagList);
        compound.put("Pot", NbtUtils.writeBlockPos(terrafirmacolonies$pot));
        compound.put("VAT", NbtUtils.writeBlockPos(terrafirmacolonies$VAT));
        return compound;
    }

    @Override
    public void deserializeNBT(CompoundTag compound) {
        super.deserializeNBT(compound);
        ListTag ovenTagList = compound.getList("Ovens", 5);
        for (int i = 0; i < ovenTagList.size(); ++i) {
            CompoundTag ovenCompound = ovenTagList.getCompound(i);
            this.terrafirmacolonies$ovenPosList.add(NbtUtils.readBlockPos(ovenCompound));
        }
        this.terrafirmacolonies$pot = NbtUtils.readBlockPos(compound.getCompound("Pot"));
        this.terrafirmacolonies$VAT = NbtUtils.readBlockPos(compound.getCompound("VAT"));
    }
}
