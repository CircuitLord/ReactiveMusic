package rocamocha.reactivemusic.compat.modmenu;


import rocamocha.reactivemusic.compat.CompatUtils;
import rocamocha.reactivemusic.config.ModConfig;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> CompatUtils.isYACLLoaded() ? ModConfig.createScreen(parent) : parent;
    }
}