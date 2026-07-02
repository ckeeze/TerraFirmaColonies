package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.SettingsModule;
import com.minecolonies.core.colony.buildings.modules.settings.BlockSetting;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingMiner;
import net.ckeeze.terrafirmacolonies.api.WorkbenchUtils;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = BuildingMiner.class, remap = false)
public abstract class BuildingMinerMixin extends AbstractBuildingStructureBuilder {
    public BuildingMinerMixin(IColony c, BlockPos l) {
        super(c, l);
    }

    @Override
    public void registerModule(@NotNull final IBuildingModule module) {
        if (module.getProducer() == BuildingModules.MINER_SETTINGS) {
            Level world = colony.getWorld();
            BlockPos pos = getLocation().getInDimensionLocation();
            ChunkData chunkData = ChunkData.get(world.getChunkAt(pos));
            Block cobble = chunkData.getRockData().getRock(pos).cobble();
            ItemStack cobbleStack = new ItemStack(cobble);
            WorkbenchUtils.getCraftingResult(
                    world,
                    List.of(
                        cobbleStack, cobbleStack, cobbleStack,
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
                    )
                )
                .filter(slab -> slab.getItem() instanceof BlockItem)
                .ifPresent(slab -> ((SettingsModule) module).with(BuildingMiner.FILL_BLOCK, new BlockSetting((BlockItem) slab.getItem())));
        }
        super.registerModule(module);
    }
}
