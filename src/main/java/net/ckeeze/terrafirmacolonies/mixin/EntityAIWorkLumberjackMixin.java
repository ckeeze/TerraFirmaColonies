package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkLumberjack;
import net.ckeeze.terrafirmacolonies.api.TFCEquipmentTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityAIWorkLumberjack.class, remap = false)
public abstract class EntityAIWorkLumberjackMixin {

    @Redirect(
        method = "prepareForWoodcutting",
        at = @At(
            value = "INVOKE",
            target = "Lcom/minecolonies/core/entity/ai/workers/production/EntityAIWorkLumberjack;checkForToolOrWeapon(Lcom/minecolonies/api/equipment/registry/EquipmentTypeEntry;)Z",
            ordinal = 1
        )
    )
    public boolean useScythe(EntityAIWorkLumberjack self, EquipmentTypeEntry equipmentType) {
        if (equipmentType == ModEquipmentTypes.hoe.get()) {
            boolean fail = self.checkForToolOrWeapon(TFCEquipmentTypes.tfcscythe.get());
            if (fail) {
                return self.checkForToolOrWeapon(ModEquipmentTypes.hoe.get());
            } else {
                return false;
            }
        }
        return self.checkForToolOrWeapon(equipmentType);
    }

}
