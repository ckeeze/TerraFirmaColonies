package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.equipment.ModEquipmentTypes;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ModEquipmentTypes.class, remap = false)
public class ModEquipmentTypesMixin {

    /**
     * @author Ckeeze
     * @reason Fix some TFC equipment recieving the wrong level. (Fishing rods, shields, shears)
     */
    @Overwrite(remap = false)
    public static int durabilityBasedLevel(ItemStack itemStack, int vanillaItemDurability) {
        String itemID = itemStack.getDescriptionId();
        if (itemID.contains("bronze")) {
            return 2; //Bronze tools
        }
        if (itemID.contains("iron")) {
            return 3; //Iron tools
        }
        if (itemID.contains("steel")) {
            if (itemID.contains("_steel")) {
                return 5; //High tier steel alloys (black, red, blue)
            }
            return 4; //Normal steel
        }
        return 1; //Stone, copper, everything else
    }
}
