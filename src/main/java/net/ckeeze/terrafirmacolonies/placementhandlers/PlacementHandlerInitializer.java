package net.ckeeze.terrafirmacolonies.placementhandlers;

import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;

public final class PlacementHandlerInitializer {
    private PlacementHandlerInitializer() {
    }

    public static void initHandlers() {
        PlacementHandlers.add(new TFCPlacementHandlers.ThatchBedPlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.ForgePlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.FirePitPlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.StainedWattlePlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.UnstainedWattlePlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.UndaubedWattlePlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.StoneAnvilPlacementHandler());
        PlacementHandlers.add(new TFCPlacementHandlers.TFCTorchPlacementHandler());
    }
}
