package rocamocha.mochamix.zones;

import rocamocha.mochamix.api.minecraft.util.MinecraftBox;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.impl.box.BoxSocket;
import rocamocha.mochamix.impl.vector3.Vector3Socket;

/**
 * Factory class for creating MinecraftBox and MinecraftVector3 instances.
 * Provides convenient static methods to avoid dealing with implementation classes directly.
 */
public class ZoneFactory {
    
    /**
     * Create a MinecraftVector3 from coordinates.
     */
    public static MinecraftVector3 createVector(double x, double y, double z) {
        return new Vector3Socket(x, y, z);
    }
    
    /**
     * Create a MinecraftBox from min and max coordinates.
     */
    public static MinecraftBox createBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        MinecraftVector3 min = new Vector3Socket(minX, minY, minZ);
        MinecraftVector3 max = new Vector3Socket(maxX, maxY, maxZ);
        return new BoxSocket(min, max);
    }
    
    /**
     * Create a MinecraftBox from two corner vectors.
     */
    public static MinecraftBox createBox(MinecraftVector3 corner1, MinecraftVector3 corner2) {
        double minX = Math.min(corner1.asNativeVec3d().x, corner2.asNativeVec3d().x);
        double minY = Math.min(corner1.asNativeVec3d().y, corner2.asNativeVec3d().y);
        double minZ = Math.min(corner1.asNativeVec3d().z, corner2.asNativeVec3d().z);
        double maxX = Math.max(corner1.asNativeVec3d().x, corner2.asNativeVec3d().x);
        double maxY = Math.max(corner1.asNativeVec3d().y, corner2.asNativeVec3d().y);
        double maxZ = Math.max(corner1.asNativeVec3d().z, corner2.asNativeVec3d().z);
        
        return createBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * Create a MinecraftBox from center point and radii.
     */
    public static MinecraftBox createBoxFromCenter(MinecraftVector3 center, double xRadius, double yRadius, double zRadius) {
        double cx = center.asNativeVec3d().x;
        double cy = center.asNativeVec3d().y;
        double cz = center.asNativeVec3d().z;
        
        return createBox(
            cx - xRadius, cy - yRadius, cz - zRadius,
            cx + xRadius, cy + yRadius, cz + zRadius
        );
    }
    
    /**
     * Create a cubic MinecraftBox from center point and radius.
     */
    public static MinecraftBox createCubeFromCenter(MinecraftVector3 center, double radius) {
        return createBoxFromCenter(center, radius, radius, radius);
    }
}