package net.ckeeze.terrafirmacolonies.api;

import com.minecolonies.api.colony.buildings.modules.settings.ISettingKey;
import com.minecolonies.core.colony.buildings.modules.settings.BoolSetting;
import com.minecolonies.core.colony.buildings.modules.settings.SettingKey;
import net.ckeeze.terrafirmacolonies.Terrafirmacolonies;
import net.minecraft.resources.ResourceLocation;

public class CustomSettings {

    public static final ISettingKey<BoolSetting> USE_SCYTHE = new SettingKey<>(BoolSetting.class, new ResourceLocation(Terrafirmacolonies.MODID, "use_scythe"));
}
