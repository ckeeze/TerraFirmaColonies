package lab;

import com.google.common.collect.ImmutableMap;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.research.util.ResearchState;
import com.minecolonies.core.MineColonies;
import com.minecolonies.core.colony.IColonyManagerCapability;
import com.minecolonies.core.research.LocalResearch;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Test-only integration fixture. Never install on a real server. */
@Mod("persistence_probe")
public class PersistenceProbe {
    private static final Path OUT = Path.of("../evidence");
    public PersistenceProbe() { MinecraftForge.EVENT_BUS.addListener(this::started); }
    private static void log(String s) { System.out.println("PERSISTENCE_PROBE " + s); }
    private static void check(boolean b, String s) {
        log((b ? "PASS " : "FAIL ") + s);
        if (!b) throw new AssertionError(s);
    }
    private static void field(Object o, String name, Object v) throws Exception {
        Class<?> c = o.getClass();
        while (c != null) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); f.set(o, v); return; }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    private static IBuilding building(IColony colony, String type, int x) {
        var entry = IMinecoloniesAPI.getInstance().getBuildingRegistry().getValue(new ResourceLocation("minecolonies", type));
        if (entry == null) throw new IllegalArgumentException("Unknown building " + type);
        return entry.produceBuilding(new BlockPos(x, 120, 0), colony);
    }
    private static void buildings(IColony c, Map<BlockPos, IBuilding> b) throws Exception {
        // Bypass placement only to isolate persistence, not to claim gameplay construction coverage.
        field(c.getServerBuildingManager(), "buildings", ImmutableMap.copyOf(b));
        c.markDirty();
    }
    private static void snapshot(String name, CompoundTag tag) throws Exception {
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve(name + ".snbt"), String.valueOf(tag));
    }
    private static void roundTrip(IColony colony, String type, String listGetter, String[] fields, String[] keys, String[] getters) throws Exception {
        IBuilding b = building(colony, type, 64);
        CompoundTag empty = b.serializeNBT();
        for (String key : keys) check(!empty.m_128441_(key), type + " omits absent " + key);
        b.deserializeNBT(empty);
        for (String getter : getters) check(b.getClass().getMethod(getter).invoke(b) == null, type + " keeps absent " + getter + " null");
        for (int i=0; i<fields.length; i++) field(b, fields[i], new BlockPos(70+i,121,-11));
        if (listGetter != null) {
            @SuppressWarnings("unchecked")
            var list = (java.util.List<BlockPos>)b.getClass().getMethod(listGetter).invoke(b);
            list.add(new BlockPos(72,121,-11));
        }
        CompoundTag saved = b.serializeNBT();
        IBuilding restored = building(colony,type,64);
        restored.deserializeNBT(saved);
        restored.deserializeNBT(saved);
        for (int i=0; i<getters.length; i++) check(new BlockPos(70+i,121,-11).equals(restored.getClass().getMethod(getters[i]).invoke(restored)), type + " restores distinct " + keys[i]);
        if (listGetter != null) check(((java.util.List<?>)restored.getClass().getMethod(listGetter).invoke(restored)).equals(java.util.List.of(new BlockPos(72,121,-11))), type + " repeated deserialize preserves exactly one list entry");
        for (String key : keys) saved.m_128365_(key, new CompoundTag());
        restored.deserializeNBT(saved);
        for (String getter : getters) check(restored.getClass().getMethod(getter).invoke(restored)==null, type + " rejects incomplete coordinates");
        for (String key : keys) saved.m_128359_(key,"invalid");
        restored.deserializeNBT(saved);
        for (String getter : getters) check(restored.getClass().getMethod(getter).invoke(restored)==null, type + " rejects wrong coordinate tag type");
        for (int i=0;i<keys.length;i++) saved.m_128365_(keys[i],NbtUtils.m_129224_(new BlockPos(0,0,0)));
        restored.deserializeNBT(saved);
        for (String getter : getters) check(new BlockPos(0,0,0).equals(restored.getClass().getMethod(getter).invoke(restored)), type + " preserves explicit origin coordinates");
        restored.deserializeNBT(empty);
        if (listGetter != null) check(((java.util.List<?>)restored.getClass().getMethod(listGetter).invoke(restored)).isEmpty(),type + " clears stale list on empty load");
        for (String getter : getters) check(restored.getClass().getMethod(getter).invoke(restored)==null,type + " clears stale optional coordinate on empty load");
    }
    private void started(ServerStartedEvent event) {
        try {
            if (!Boolean.getBoolean("persistence.lab")) throw new IllegalStateException("Lab property required");
            Level world = event.getServer().m_129783_();
            var cap = ((net.minecraftforge.common.capabilities.ICapabilityProvider)world).getCapability(MineColonies.COLONY_MANAGER_CAP).resolve().orElseThrow();
            String mode = System.getProperty("persistence.mode", "seed");
            boolean fixed = System.getProperty("persistence.expected", "broken").equals("fixed");
            log("MODE " + mode);
            if (mode.equals("inspect") || mode.equals("inspect-control")) {
                check(cap.getColonies().size() == 1, "one persisted colony");
                IColony c = cap.getColonies().get(0);
                snapshot("restart-" + System.getProperty("persistence.label", "unknown"), c.getColonyTag());
                log("RESTART name=" + c.getName() + " buildings=" + c.getServerBuildingManager().getBuildings().size()
                    + " research=" + c.getResearchManager().getResearchTree().getCompletedList());
                boolean control = mode.equals("inspect-control") || fixed;
                check(c.getName().equals(control ? "UNSAVED_PROGRESS" : "BASELINE"), "expected name after restart");
                check(c.getServerBuildingManager().getBuildings().size() == (fixed ? 4 : control ? 2 : 1), "expected building count after restart");
                check(c.getResearchManager().getResearchTree().isComplete(new ResourceLocation("minecolonies:technology/woodwork")) == control, "expected research after restart");
                if (fixed) {
                    for (var entry : c.getServerBuildingManager().getBuildings().entrySet()) {
                        IBuilding b=entry.getValue();
                        int x=entry.getKey().m_123341_();
                        String[] getters = x==16 ? new String[]{"getPotPos","getVatPos"} : x==32 ? new String[]{"getCharcoalPos"} : x==48 ? new String[]{"getCharcoalPos","getBellowPos"} : new String[]{};
                        for (String getter : getters) check(b.getClass().getMethod(getter).invoke(b)==null,"restart preserves absent " + x + " " + getter);
                    }
                }
            } else if (mode.equals("seed")) {
                check(cap.getColonies().isEmpty(), "fresh lab world");
                IColony c = cap.createColony(world, new BlockPos(0, 120, 0));
                c.setName("BASELINE"); c.setCanBeAutoDeleted(false);
                IBuilding th = building(c, "townhall", 0);
                Map<BlockPos, IBuilding> map = new LinkedHashMap<>(); map.put(new BlockPos(0,120,0), th);
                buildings(c, map);
                snapshot("baseline", c.getColonyTag());
                check(c.getColonyTag() != null, "baseline serializes");
            } else if (fixed) {
                IColony c = cap.getColonies().get(0);
                if (mode.equals("mutate")) {
                    roundTrip(c,"kitchen","getOvenList",new String[]{"terrafirmacolonies$pot","terrafirmacolonies$VAT"},new String[]{"Pot","VAT"},new String[]{"getPotPos","getVatPos"});
                    roundTrip(c,"glassblower",null,new String[]{"terrafirmacolonies$charcoalForge"},new String[]{"forge"},new String[]{"getCharcoalPos"});
                    roundTrip(c,"stonesmeltery","getDryingBlockList",new String[]{"terrafirmacolonies$charcoalForge","terrafirmacolonies$bellow"},new String[]{"forge","bellow"},new String[]{"getCharcoalPos","getBellowPos"});
                    roundTrip(c,"baker","getOvenList",new String[]{},new String[]{},new String[]{});
                    CompoundTag old = c.getColonyTag();
                    Map<BlockPos,IBuilding> map = new LinkedHashMap<>(c.getServerBuildingManager().getBuildings());
                    for (int i=0;i<3;i++) map.put(new BlockPos(16*(i+1),120,0),building(c,new String[]{"kitchen","glassblower","stonesmeltery"}[i],16*(i+1)));
                    buildings(c,map);
                    c.setName("UNSAVED_PROGRESS");
                    var id=new ResourceLocation("minecolonies:technology/woodwork");
                    var branch=new ResourceLocation("minecolonies:technology");
                    LocalResearch r=new LocalResearch(id,branch,1); r.setState(ResearchState.FINISHED);
                    c.getResearchManager().getResearchTree().addResearch(branch,r);
                    c.getResearchManager().getResearchTree().finishResearch(id);
                    c.markDirty();
                    check(c.getColonyTag()!=old,"unfinished buildings allow a fresh whole-colony snapshot");
                }
                check(c.getName().equals("UNSAVED_PROGRESS"),"new name exists in memory");
                check(c.getServerBuildingManager().getBuildings().size()==4,"all three unfinished buildings remain in memory");
                check(c.getResearchManager().getResearchTree().isComplete(new ResourceLocation("minecolonies:technology/woodwork")),"completed research remains in memory");
                snapshot("fixed-"+System.getProperty("persistence.label",mode),c.getColonyTag());
            } else {
                IColony c = cap.getColonies().get(0);
                CompoundTag old = c.getColonyTag();
                snapshot(System.getProperty("persistence.label", mode) + "-before-mutation", old);
                for (String type : new String[]{"kitchen", "glassblower", "stonesmeltery"}) {
                    IBuilding b = building(c, type, 16);
                    boolean failed = false;
                    try { b.serializeNBT(); }
                    catch (NullPointerException e) { failed = true; log("EXPECTED_NULL " + type); e.printStackTrace(System.out); }
                    check(failed, "fresh " + type + " serialization fails");
                }
                IBuilding kitchen = building(c, "kitchen", 16);
                // Independent null cases and positive control use the actual transformed serializer.
                BlockPos p = new BlockPos(16,120,0);
                field(kitchen, "terrafirmacolonies$pot", p);
                boolean vatFailed = false;
                try { kitchen.serializeNBT(); } catch (NullPointerException e) { vatFailed = true; }
                check(vatFailed, "kitchen with pot but no VAT fails");
                field(kitchen, "terrafirmacolonies$VAT", p);
                check(kitchen.serializeNBT() != null, "kitchen with both coordinates serializes");
                var ovenAccess = kitchen.getClass().getMethod("getOvenList");
                @SuppressWarnings("unchecked")
                var ovens = (java.util.List<BlockPos>) ovenAccess.invoke(kitchen);
                ovens.add(p);
                IBuilding loadedKitchen = building(c, "kitchen", 32);
                loadedKitchen.deserializeNBT(kitchen.serializeNBT());
                check(((java.util.List<?>)ovenAccess.invoke(loadedKitchen)).isEmpty(), "kitchen loses saved oven list on deserialize (wrong NBT type)");
                IBuilding glass = building(c, "glassblower", 48);
                field(glass, "terrafirmacolonies$charcoalForge", p);
                check(glass.serializeNBT() != null, "glassblower with forge serializes");
                IBuilding smelter = building(c, "stonesmeltery", 64);
                field(smelter, "terrafirmacolonies$charcoalForge", p);
                boolean bellowFailed = false;
                try { smelter.serializeNBT(); } catch (NullPointerException e) { bellowFailed = true; }
                check(bellowFailed, "stonesmeltery with forge but no bellow fails");
                field(smelter, "terrafirmacolonies$bellow", p);
                check(smelter.serializeNBT() != null, "stonesmeltery with forge and bellow serializes");
                if (!mode.equals("control")) field(kitchen, "terrafirmacolonies$pot", null);
                Map<BlockPos, IBuilding> map = new LinkedHashMap<>(c.getServerBuildingManager().getBuildings());
                map.put(p, kitchen); buildings(c, map);
                c.setName("UNSAVED_PROGRESS");
                var id = new ResourceLocation("minecolonies:technology/woodwork");
                var branch = new ResourceLocation("minecolonies:technology");
                LocalResearch r = new LocalResearch(id, branch, 1); r.setState(ResearchState.FINISHED);
                c.getResearchManager().getResearchTree().addResearch(branch, r);
                c.getResearchManager().getResearchTree().finishResearch(id);
                c.markDirty();
                check(c.getResearchManager().getResearchTree().isComplete(id), "research completed in memory");
                check(c.getServerBuildingManager().getBuildings().size() == 2, "new building exists in memory");
                CompoundTag stale = c.getColonyTag();
                snapshot(System.getProperty("persistence.label", mode) + "-cache", stale);
                if (mode.equals("control")) {
                    check(stale != old, "valid kitchen allows new colony snapshot");
                } else {
                    check(stale == old, "failed serialization returns SAME previous cached colony tag");
                    check(stale.toString().equals(old.toString()), "cached data remains unchanged");
                }
                snapshot("capability-" + System.getProperty("persistence.label", mode), (CompoundTag) IColonyManagerCapability.Storage.writeNBT(MineColonies.COLONY_MANAGER_CAP, cap, true));
            }
            log("READY");
        } catch (Throwable e) { log("FATAL"); e.printStackTrace(System.out); }
    }
}
