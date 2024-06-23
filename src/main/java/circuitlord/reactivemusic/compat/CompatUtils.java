package circuitlord.reactivemusic.compat;

import net.fabricmc.loader.api.FabricLoader;

public class CompatUtils {
    public static boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static boolean isClothConfigLoaded() {
        return isModLoaded("cloth-config2");
    }


}