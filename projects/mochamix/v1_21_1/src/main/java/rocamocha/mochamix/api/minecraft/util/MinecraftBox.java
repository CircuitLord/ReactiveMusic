package rocamocha.mochamix.api.minecraft.util;

import rocamocha.mochamix.impl.NativeAccess;

public interface MinecraftBox extends NativeAccess {
    MinecraftVector3 min();
    MinecraftVector3 max();
    MinecraftVector3 center();
    MinecraftVector3 size();

    int width(); // x dimension
    int height(); // y dimension
    int depth(); // z dimension
}
