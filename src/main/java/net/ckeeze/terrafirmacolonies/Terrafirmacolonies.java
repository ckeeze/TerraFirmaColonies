package net.ckeeze.terrafirmacolonies;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Terrafirmacolonies.MODID)
public class Terrafirmacolonies {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "terrafirmacolonies";
    // Directly reference a slf4j logger

    // Create a Deferred Register to hold Blocks which will all be registered under the "terrafirmacolonies" namespace
    public Terrafirmacolonies(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        ModChanges.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }
}
