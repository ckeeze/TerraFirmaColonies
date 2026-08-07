package net.ckeeze.terrafirmacolonies.mixin;

import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFlorist;
import com.minecolonies.core.util.WorkerUtil;
import net.ckeeze.terrafirmacolonies.api.TFCEquipmentTypes;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.util.Metal.Default;
import net.dries007.tfc.util.Metal.ItemType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = WorkerUtil.class, remap = false)
public class WorkerUtilMixin {

    @Inject(method = "getOrInitTestTools", at = @At("RETURN"))
    private static void getOrInitTestTools(CallbackInfoReturnable<List<Tuple<EquipmentTypeEntry, ItemStack>>> ci) {
        WorkerUtil.tools.add(new Tuple<>(TFCEquipmentTypes.tfcscythe.get(), TFCItems.METAL_ITEMS.get(Default.COPPER).get(ItemType.SCYTHE).get().getDefaultInstance()));
        WorkerUtil.tools.add(new Tuple<>(TFCEquipmentTypes.tfchammer.get(), TFCItems.METAL_ITEMS.get(Default.COPPER).get(ItemType.HAMMER).get().getDefaultInstance()));
    }


    /**
     * @author Ckeeze
     * @reason Florist checking
     */
    @Overwrite(remap = false)
    public static boolean isThereCompostedLand(BuildingFlorist buildingFlorist, Level world) {
        for (BlockPos pos : buildingFlorist.getPlantGround()) {
            if (WorldUtil.isBlockLoaded(world, pos)) {
                BlockState butterflyblock = world.getBlockState(pos.above());
                return butterflyblock.is(FLBlocks.BUTTERFLY_GRASS.get());
            }
        }
        return false;
    }
}
