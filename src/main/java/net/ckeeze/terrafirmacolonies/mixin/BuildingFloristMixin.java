package net.ckeeze.terrafirmacolonies.mixin;

import com.eerussianguy.firmalife.common.items.FLItems;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFlorist;
import net.ckeeze.terrafirmacolonies.api.TFCEquipmentTypes;
import net.dries007.tfc.common.TFCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(value = BuildingFlorist.class)
public abstract class BuildingFloristMixin extends AbstractBuilding {

    @Shadow(remap = false)
    private final List<BlockPos> plantGround = new ArrayList<>();

    protected BuildingFloristMixin(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    /**
     * @author Ckeeze
     * @reason register TFC dirt positions
     */
    @Override
    @Overwrite(remap = false)
    public void registerBlockPosition(@NotNull Block block, @NotNull BlockPos pos, @NotNull Level world) {
        super.registerBlockPosition(block, pos, world);
        if (block.defaultBlockState().is(TFCTags.Blocks.GRASS_PLANTABLE_ON) && !this.plantGround.contains(pos)) {
            this.plantGround.add(pos);
        }
    }

    @Override
    public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount() {
        Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> toKeep = new HashMap<>(super.getRequiredItemsAndAmount());

        toKeep.put((itemStack) -> ItemStackUtils.hasEquipmentLevel(itemStack, TFCEquipmentTypes.tfcknife.get(), 0, this.getMaxEquipmentLevel()), new Tuple<>(1, true));
        toKeep.put((stack) -> ItemStack.isSameItem(FLItems.SEED_BALL.get().getDefaultInstance(), stack), new Tuple<>(64, true));

        return toKeep;
    }
}
