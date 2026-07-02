package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.minecolonies.core.entity.ai.workers.AbstractAISkeleton;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.util.WorkerUtil;
import net.ckeeze.terrafirmacolonies.api.ExtendedWorkerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public abstract class AbstractEntityAIBasicMixin<J extends AbstractJob<?, J>, B extends AbstractBuilding> extends AbstractAISkeleton<J> {

    protected AbstractEntityAIBasicMixin(@NotNull J job) {
        super(job);
    }

    @Final
    @Shadow
    public B building;

    @Shadow
    protected static final int NO_TOOL = -10;

    @Shadow
    private static final int TOOL_NOT_FOUND = -1;

    /**
     * @author hohserg
     * @reason need to completely change to add multiple tool type feature
     */
    @Overwrite
    protected int getMostEfficientTool(@NotNull final BlockState target, final BlockPos pos) {
        final Set<EquipmentTypeEntry> toolTypes = ExtendedWorkerUtil.getBestToolForBlock(target, building, world, pos);
        final int required = WorkerUtil.getCorrectHarvestLevelForBlock(target);

        if (toolTypes.isEmpty()) {
            return NO_TOOL;
        }

        int bestSlot = TOOL_NOT_FOUND;
        int bestLevel = Integer.MAX_VALUE;
        @NotNull final InventoryCitizen inventory = worker.getInventoryCitizen();
        final int maxToolLevel = worker.getCitizenColonyHandler().getWorkBuilding().getMaxEquipmentLevel();

        for (int i = 0; i < worker.getInventoryCitizen().getSlots(); i++) {
            final ItemStack item = inventory.getStackInSlot(i);
            for (EquipmentTypeEntry toolType : toolTypes) {
                final int level = toolType.getMiningLevel(item);

                if (level > -1 && level >= required && level < bestLevel && ItemStackUtils.verifyEquipmentLevel(item, level, required, maxToolLevel)) {
                    bestSlot = i;
                    bestLevel = level;
                }
            }
        }

        return bestSlot;
    }
}
