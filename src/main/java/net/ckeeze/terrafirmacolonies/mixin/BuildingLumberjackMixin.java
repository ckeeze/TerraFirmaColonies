package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.modules.IBuildingModule;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.SettingsModule;
import com.minecolonies.core.colony.buildings.modules.settings.BoolSetting;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLumberjack;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import static net.ckeeze.terrafirmacolonies.api.CustomSettings.USE_SCYTHE;

@Mixin(BuildingLumberjack.class)
public abstract class BuildingLumberjackMixin extends AbstractBuilding {

    public BuildingLumberjackMixin(@NotNull IColony colony, BlockPos pos) {
        super(colony, pos);
    }

    @Override
    public void registerModule(@NotNull final IBuildingModule module) {
        if (module.getProducer() == BuildingModules.FORESTER_SETTINGS) {
            ((SettingsModule) module).with(USE_SCYTHE, new BoolSetting(false));
        }
        super.registerModule(module);
    }
}
