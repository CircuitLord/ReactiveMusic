package rocamocha.mochamix.api.minecraft;

import java.util.Map;

import rocamocha.mochamix.api.minecraft.MinecraftComponent.*;
import rocamocha.mochamix.api.minecraft.util.MinecraftIdentity;
import rocamocha.mochamix.impl.NativeAccess;

public interface MinecraftItemStack extends NativeAccess {

    MinecraftIdentity identity();
    Map<String, EnchantmentAccess> enchantments();
    FoodAccess food();


    String name();
    int count();
    int max();
    boolean isEmpty();

    interface Identity {
        String namespace();
        String path();
        String full();
    }
}
