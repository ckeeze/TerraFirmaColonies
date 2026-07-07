package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.BuildingExtensionsModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFarmer;
import net.dries007.tfc.common.blocks.crop.Crop;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(value = BuildingFarmer.class, remap = false)
public abstract class BuildingFarmerMixin extends AbstractBuilding {
    protected BuildingFarmerMixin(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    /**
     * @author Ckeeze
     * @reason add sticks to needed items when using tomato or green bean
     */
    @Override
    @Overwrite
    public Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> getRequiredItemsAndAmount() {
        Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> toKeep = new HashMap<>(super.getRequiredItemsAndAmount());

        for (BuildingExtensionsModule module : this.getModulesByType(BuildingExtensionsModule.class)) {
            for (IBuildingExtension field : module.getOwnedExtensions()) {
                if (field instanceof FarmField farmField) {
                    if (!farmField.getSeed().isEmpty()) {
                        toKeep.put((Predicate) (stack) -> ItemStack.isSameItem(farmField.getSeed(), (ItemStack) stack), new Tuple<>(64, true));
                        //New if branch
                        if (farmField.getSeed().is(TFCItems.CROP_SEEDS.get(Crop.GREEN_BEAN).get()) || farmField.getSeed().is(TFCItems.CROP_SEEDS.get(Crop.TOMATO).get())) {
                            toKeep.put((Predicate) (stack) -> ItemStack.isSameItem(Items.STICK.getDefaultInstance(), (ItemStack) stack), new Tuple<>(64, true));
                        }
                    }
                }
            }
        }
        return toKeep;
    }
}
