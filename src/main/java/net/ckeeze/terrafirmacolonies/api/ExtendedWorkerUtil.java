package net.ckeeze.terrafirmacolonies.api;

import com.google.common.collect.ImmutableSet;
import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.SettingsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IForgeShearable;

import java.util.Set;

import static com.minecolonies.core.colony.buildings.AbstractBuilding.USE_SHEARS;
import static com.minecolonies.core.util.WorkerUtil.getOrInitTestTools;

public class ExtendedWorkerUtil {
    public static Set<EquipmentTypeEntry> getBestToolForBlock(final BlockState state, final AbstractBuilding building, final BlockGetter level, final BlockPos pos) {
        if (state.getBlock() instanceof IForgeShearable && building.hasModule(SettingsModule.class) && building.getFirstModuleOccurance(SettingsModule.class).getSettingValueOrDefault(USE_SHEARS, true)) {
            return ImmutableSet.of(ModEquipmentTypes.shears.get());
        }

        float blockHardness = state.getDestroySpeed(level, pos);
        if (blockHardness > 0f) {
            ImmutableSet.Builder<EquipmentTypeEntry> builder = ImmutableSet.builder();
            for (final Tuple<EquipmentTypeEntry, ItemStack> tool : getOrInitTestTools()) {
                if (tool.getB() != null && tool.getB().getItem() instanceof DiggerItem) {
                    if (state.getBlock() instanceof IMateriallyTexturedBlock materiallyTexturedBlock) {
                        if (materiallyTexturedBlock.isCorrectToolForDrops(state, tool.getB(), level, pos)) {
                            builder.add(tool.getA());
                        }
                    }
                    if (tool.getB().isCorrectToolForDrops(state)) {
                        builder.add(tool.getA());
                    }
                }
            }
            return builder.build();
        }

        return ImmutableSet.of();
    }

    public static boolean setBlockWithoutCollapse(LevelAccessor world, BlockPos pos, BlockState blockState){
        return WorldUtil.setBlockState(world, pos, blockState, (~Block.UPDATE_NEIGHBORS) & Block.UPDATE_CLIENTS);
    }
}
