package circuitlord.reactivemusic.compat.modmenu;


import circuitlord.reactivemusic.compat.CompatUtils;
import circuitlord.reactivemusic.config.ModConfig;

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