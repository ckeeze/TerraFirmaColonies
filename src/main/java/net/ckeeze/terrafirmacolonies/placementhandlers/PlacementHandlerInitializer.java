package net.ckeeze.terrafirmacolonies.placementhandlers;

import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;

public final class PlacementHandlerInitializer {

    /**
     * Private constructor to hide implicit one.
     */
    private PlacementHandlerInitializer() {
    }

    public static void initHandlers() {
        PlacementHandlers.add(new TFCPlacementHandlers.ThatchBedPlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.ForgePlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.FirePitPlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.WattlePlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.StoneAnvilPlacementHandler());
    }
}
