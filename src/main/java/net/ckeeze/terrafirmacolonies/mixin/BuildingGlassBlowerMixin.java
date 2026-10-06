package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingGlassblower;
import net.ckeeze.terrafirmacolonies.api.mixininterfaces.GlassBlowerNewVaraibles;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.util.Metal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BuildingGlassblower.class)
public abstract class BuildingGlassBlowerMixin extends AbstractBuilding implements GlassBlowerNewVaraibles {

    protected BuildingGlassBlowerMixin(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    @Unique
    private BlockPos terrafirmacolonies$charcoalForge;

    @Override
    public void registerBlockPosition(@NotNull Block block, @NotNull BlockPos pos, @NotNull Level world) {
        super.registerBlockPosition(block, pos, world);
        if (block.defaultBlockState().is(TFCBlocks.CHARCOAL_FORGE.get())) {
            terrafirmacolonies$charcoalForge = pos;
        }
        if (block == TFCBlocks.METALS.get(Metal.Default.BRASS).get(Metal.BlockType.BLOCK).get()) {
            if (world.getBlockState(pos.below()).equals(TFCBlocks.METALS.get(Metal.Default.BRASS).get(Metal.BlockType.BLOCK).get().defaultBlockState())) {

            }
        }

    }

    @Override
    public BlockPos getCharcoalPos() {
        return this.terrafirmacolonies$charcoalForge;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compound = super.serializeNBT();
        compound.put("forge", NbtUtils.writeBlockPos(terrafirmacolonies$charcoalForge));
        return compound;
    }

    @Override
    public void deserializeNBT(CompoundTag compound) {
        super.deserializeNBT(compound);
        this.terrafirmacolonies$charcoalForge = NbtUtils.readBlockPos(compound.getCompound("forge"));
    }
}