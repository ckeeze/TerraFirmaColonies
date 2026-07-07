package net.ckeeze.terrafirmacolonies.api;

import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.items.HammerItem;
import net.dries007.tfc.common.items.ScytheItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Objects;
import java.util.function.Consumer;

public class TFCEquipmentTypes {
    public static final DeferredRegister<EquipmentTypeEntry> DEFERRED_REGISTER = DeferredRegister.create(new ResourceLocation("minecolonies", "equipmenttypes"), "minecolonies");

    public static final RegistryObject<EquipmentTypeEntry> tfcscythe = register("tfcscythe", (builder) -> builder.setDisplayName(Component.translatable("com.minecolonies.coremod.tooltype.shield")).setIsEquipment((itemStack, equipmentType) -> itemStack.getItem() instanceof ScytheItem).setEquipmentLevel((itemStack, equipmentType) -> getLevel(itemStack)).build());
    public static final RegistryObject<EquipmentTypeEntry> tfcknife = register("tfcknife", (builder) -> builder.setDisplayName(Component.translatable("com.minecolonies.coremod.tooltype.shield")).setIsEquipment((itemStack, equipmentType) -> itemStack.is(TFCTags.Items.KNIVES)).setEquipmentLevel((itemStack, equipmentType) -> getLevel(itemStack)).build());
    public static final RegistryObject<EquipmentTypeEntry> tfchammer = register("tfchammer", (builder) -> builder.setDisplayName(Component.translatable("com.minecolonies.coremod.tooltype.shield")).setIsEquipment((itemStack, equipmentType) -> itemStack.getItem() instanceof HammerItem).setEquipmentLevel((itemStack, equipmentType) -> getLevel(itemStack)).build());

    @SuppressWarnings("rawtypes")
    private static RegistryObject<EquipmentTypeEntry> register(String id, Consumer<EquipmentTypeEntry.Builder> consumer) {
        EquipmentTypeEntry.Builder equipmentType = (new EquipmentTypeEntry.Builder()).setRegistryName(new ResourceLocation("minecolonies", id));
        consumer.accept(equipmentType);
        Objects.requireNonNull(equipmentType);
        return ((DeferredRegister) DEFERRED_REGISTER).register(id, equipmentType::build);
    }


    public static int getLevel(ItemStack itemStack) {
        String itemID = itemStack.getDescriptionId();
        if (itemID.contains("bronze")) {
            return 2;
        }
        if (itemID.contains("iron")) {
            return 3;
        }
        if (itemID.contains("steel")) {
            if (itemID.contains("_steel")) {
                return 5;
            }
            return 4;
        }
        return 1;
    }
}
