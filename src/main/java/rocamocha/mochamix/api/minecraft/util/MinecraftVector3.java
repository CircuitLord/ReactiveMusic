package rocamocha.mochamix.api.minecraft.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rocamocha.mochamix.impl.vector3.Vector3Math;

/**
 * A unified interface for different Minecraft position types: Vec3d, Vec3i, BlockPos.
 * Provides conversions between types and math operations, so you don't have to care about the underlying type.
 * 
 * Uses double internally, so may lose precision for very large coordinates.
 * Avoids unnecessary object allocations by reusing Vector3View instances.
 * Math operations are delegated to Vector3Math for clean separation of concerns.
 * Use asVec3i() or asBlockPos() to get integer coordinates when needed.
 * 
 * This API type is returned by other Mochamix methods as part of the stable API.
 */
public interface MinecraftVector3 {
    
    /** 
     * Abstraction layer for position math, for clean separation of concerns
     * @return a Vector3Math instance for this position
     */
    Vector3Math doMath();

    // Conversions between different position types
    Vector3d asVec3d();
    Vector3i asVec3i();
    BlockPosition asBlockPos();


    public interface Vector3d extends MinecraftVector3 {
        double xd();
        double yd();
        double zd();
    }

    public interface Vector3i extends MinecraftVector3 {
        int xi();
        int yi();
        int zi();
    }
    
    interface BlockPosition extends Vector3i {
        
    }

    // Math operations delegated to Vector3Math
    default MinecraftVector3 offset(int xi, int yi, int zi) { return doMath().offset(xi, yi, zi); }
    default MinecraftVector3 add(MinecraftVector3 that) { return doMath().add(that); }
    default MinecraftVector3 subtract(MinecraftVector3 that) { return doMath().subtract(that); }

    default double length() { return doMath().length(); }

    /**
     * Get the native Minecraft position object.
     * This is useful for interoperability with native Minecraft APIs.
     * Leaks the abstraction, so use sparingly.
     * When mojang changes the underlying type, this will break and require API consumers to rebuild.
     */
    BlockPos asNativeBlockPos();
    /** @see asNativeBlockPos */
    Vec3i asNativeVec3i();
    /** @see asNativeBlockPos */
    Vec3d asNativeVec3d();
}
