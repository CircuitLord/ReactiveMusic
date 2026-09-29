package circuitlord.reactivemusic.config;

import net.minecraft.client.gui.screens.Screen;

/**
 * Legacy placeholder kept so old references/imports do not break.
 * The real config screen is built by {@link ModConfig#createScreen(Screen)}.
 */
public class YAConfig {
    public static Screen buildScreen(Screen parent) {
        return ModConfig.createScreen(parent);
    }
}
