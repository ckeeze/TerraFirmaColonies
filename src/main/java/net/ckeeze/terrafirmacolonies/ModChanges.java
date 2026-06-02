package net.ckeeze.terrafirmacolonies;

import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class ModChanges {

    public static final DeferredRegister<BuildingEntry> DEFERRED_REGISTER = DeferredRegister.create(new ResourceLocation("minecolonies", "buildings"), "minecolonies");

    public static void register(IEventBus eventBus) {
        DEFERRED_REGISTER.register(eventBus);
    }
}
