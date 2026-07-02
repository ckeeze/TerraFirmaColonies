package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLumberjack;
import com.minecolonies.core.colony.jobs.JobLumberjack;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkLumberjack;
import net.ckeeze.terrafirmacolonies.api.TFCEquipmentTypes;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.ckeeze.terrafirmacolonies.api.CustomSettings.USE_SCYTHE;

@Mixin(value = EntityAIWorkLumberjack.class, remap = false)
public abstract class EntityAIWorkLumberjackMixin extends AbstractEntityAICrafting<JobLumberjack, BuildingLumberjack> {

    public EntityAIWorkLumberjackMixin(@NotNull JobLumberjack job) {
        super(job);
    }

    @Redirect(
        method = "prepareForWoodcutting",
        at = @At(
            value = "FIELD",
            target = "Lcom/minecolonies/api/equipment/ModEquipmentTypes;hoe:Lnet/minecraftforge/registries/RegistryObject;",
            opcode = Opcodes.GETSTATIC
        )
    )
    public RegistryObject<EquipmentTypeEntry> useScythe() {
        if (building.getSetting(USE_SCYTHE).getValue())
            return TFCEquipmentTypes.tfcscythe;
        else
            return ModEquipmentTypes.hoe;
    }

}
