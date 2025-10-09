package rocamocha.mochamix.zones;

import rocamocha.mochamix.api.minecraft.util.MinecraftBox;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;

import java.util.List;
import java.util.Optional;

/**
 * Utility class providing convenient methods for zone management operations.
 * Acts as a simplified interface to the ZoneDataManager.
 */
public class ZoneUtils {
    private static final ZoneDataManager manager = ZoneDataManager.getInstance();
    
    private ZoneUtils() {} // Utility class
    
    /**
     * Create a zone from center point and radii.
     */
    public static String createZoneFromCenter(String name, MinecraftVector3 center, double xRadius, double yRadius, double zRadius) {
        MinecraftBox box = ZoneFactory.createBoxFromCenter(center, xRadius, yRadius, zRadius);
        return manager.createZone(name, box);
    }
    
    /**
     * Create a zone from two corner points.
     */
    public static String createZoneFromCorners(String name, MinecraftVector3 corner1, MinecraftVector3 corner2) {
        MinecraftBox box = ZoneFactory.createBox(corner1, corner2);
        return manager.createZone(name, box);
    }
    
    /**
     * Check if a point is inside any zone with the given name.
     */
    public static boolean isPointInZone(String zoneName, MinecraftVector3 point) {
        List<ZoneData> zones = manager.getZonesByName(zoneName);
        
        for (ZoneData zone : zones) {
            MinecraftBox box = zone.toMinecraftBox();
            if (box != null && isPointInBox(point, box)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a point is inside a specific zone by unique ID.
     */
    public static boolean isPointInZoneById(String uniqueId, MinecraftVector3 point) {
        Optional<ZoneData> zone = manager.getZone(uniqueId);
        
        if (zone.isPresent()) {
            MinecraftBox box = zone.get().toMinecraftBox();
            return box != null && isPointInBox(point, box);
        }
        
        return false;
    }
    
    /**
     * Get all zones that contain the given point.
     */
    public static List<ZoneData> getZonesContainingPoint(MinecraftVector3 point) {
        return manager.getAllZones().stream()
                .filter(zone -> {
                    MinecraftBox box = zone.toMinecraftBox();
                    return box != null && isPointInBox(point, box);
                })
                .toList();
    }
    
    /**
     * Get the first zone (by name) that contains the given point.
     */
    public static Optional<ZoneData> getFirstZoneContainingPoint(String zoneName, MinecraftVector3 point) {
        return manager.getZonesByName(zoneName).stream()
                .filter(zone -> {
                    MinecraftBox box = zone.toMinecraftBox();
                    return box != null && isPointInBox(point, box);
                })
                .findFirst();
    }
    
    /**
     * Expand a zone by the given amounts in each direction.
     */
    public static boolean expandZone(String uniqueId, double xExpansion, double yExpansion, double zExpansion) {
        Optional<ZoneData> zoneOpt = manager.getZone(uniqueId);
        
        if (zoneOpt.isPresent()) {
            ZoneData zone = zoneOpt.get();
            MinecraftBox currentBox = zone.toMinecraftBox();
            
            if (currentBox != null) {
                double minX = currentBox.min().asNativeVec3d().x - xExpansion;
                double minY = currentBox.min().asNativeVec3d().y - yExpansion;
                double minZ = currentBox.min().asNativeVec3d().z - zExpansion;
                double maxX = currentBox.max().asNativeVec3d().x + xExpansion;
                double maxY = currentBox.max().asNativeVec3d().y + yExpansion;
                double maxZ = currentBox.max().asNativeVec3d().z + zExpansion;
                
                MinecraftBox expandedBox = ZoneFactory.createBox(minX, minY, minZ, maxX, maxY, maxZ);
                
                return manager.updateZoneBox(uniqueId, expandedBox);
            }
        }
        
        return false;
    }
    
    /**
     * Move a zone by the given offset.
     */
    public static boolean moveZone(String uniqueId, double xOffset, double yOffset, double zOffset) {
        Optional<ZoneData> zoneOpt = manager.getZone(uniqueId);
        
        if (zoneOpt.isPresent()) {
            ZoneData zone = zoneOpt.get();
            MinecraftBox currentBox = zone.toMinecraftBox();
            
            if (currentBox != null) {
                double minX = currentBox.min().asNativeVec3d().x + xOffset;
                double minY = currentBox.min().asNativeVec3d().y + yOffset;
                double minZ = currentBox.min().asNativeVec3d().z + zOffset;
                double maxX = currentBox.max().asNativeVec3d().x + xOffset;
                double maxY = currentBox.max().asNativeVec3d().y + yOffset;
                double maxZ = currentBox.max().asNativeVec3d().z + zOffset;
                
                MinecraftBox movedBox = ZoneFactory.createBox(minX, minY, minZ, maxX, maxY, maxZ);
                
                return manager.updateZoneBox(uniqueId, movedBox);
            }
        }
        
        return false;
    }
    
    /**
     * Get the center point of a zone.
     */
    public static Optional<MinecraftVector3> getZoneCenter(String uniqueId) {
        Optional<ZoneData> zone = manager.getZone(uniqueId);
        
        if (zone.isPresent()) {
            MinecraftBox box = zone.get().toMinecraftBox();
            if (box != null) {
                double centerX = (box.min().asNativeVec3d().x + box.max().asNativeVec3d().x) / 2.0;
                double centerY = (box.min().asNativeVec3d().y + box.max().asNativeVec3d().y) / 2.0;
                double centerZ = (box.min().asNativeVec3d().z + box.max().asNativeVec3d().z) / 2.0;
                return Optional.of(ZoneFactory.createVector(centerX, centerY, centerZ));
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Get the volume of a zone.
     */
    public static Optional<Double> getZoneVolume(String uniqueId) {
        Optional<ZoneData> zone = manager.getZone(uniqueId);
        
        if (zone.isPresent()) {
            MinecraftBox box = zone.get().toMinecraftBox();
            if (box != null) {
                double width = box.max().asNativeVec3d().x - box.min().asNativeVec3d().x;
                double height = box.max().asNativeVec3d().y - box.min().asNativeVec3d().y;
                double depth = box.max().asNativeVec3d().z - box.min().asNativeVec3d().z;
                return Optional.of(width * height * depth);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Check if two zones overlap.
     */
    public static boolean doZonesOverlap(String uniqueId1, String uniqueId2) {
        Optional<ZoneData> zone1Opt = manager.getZone(uniqueId1);
        Optional<ZoneData> zone2Opt = manager.getZone(uniqueId2);
        
        if (zone1Opt.isPresent() && zone2Opt.isPresent()) {
            MinecraftBox box1 = zone1Opt.get().toMinecraftBox();
            MinecraftBox box2 = zone2Opt.get().toMinecraftBox();
            
            if (box1 != null && box2 != null) {
                return doBoxesOverlap(box1, box2);
            }
        }
        
        return false;
    }
    
    /**
     * Find all zones that overlap with the given zone.
     */
    public static List<ZoneData> getOverlappingZones(String uniqueId) {
        Optional<ZoneData> targetZone = manager.getZone(uniqueId);
        
        if (targetZone.isEmpty()) {
            return List.of();
        }
        
        MinecraftBox targetBox = targetZone.get().toMinecraftBox();
        if (targetBox == null) {
            return List.of();
        }
        
        return manager.getAllZones().stream()
                .filter(zone -> !zone.getUniqueId().equals(uniqueId)) // Exclude the target zone itself
                .filter(zone -> {
                    MinecraftBox box = zone.toMinecraftBox();
                    return box != null && doBoxesOverlap(targetBox, box);
                })
                .toList();
    }
    
    // Helper method to check if a point is inside a box
    private static boolean isPointInBox(MinecraftVector3 point, MinecraftBox box) {
        double px = point.asNativeVec3d().x;
        double py = point.asNativeVec3d().y;
        double pz = point.asNativeVec3d().z;
        double minX = box.min().asNativeVec3d().x;
        double minY = box.min().asNativeVec3d().y;
        double minZ = box.min().asNativeVec3d().z;
        double maxX = box.max().asNativeVec3d().x;
        double maxY = box.max().asNativeVec3d().y;
        double maxZ = box.max().asNativeVec3d().z;
        
        return px >= minX && px <= maxX &&
               py >= minY && py <= maxY &&
               pz >= minZ && pz <= maxZ;
    }
    
    // Helper method to check if two boxes overlap
    private static boolean doBoxesOverlap(MinecraftBox box1, MinecraftBox box2) {
        double b1MinX = box1.min().asNativeVec3d().x;
        double b1MinY = box1.min().asNativeVec3d().y;
        double b1MinZ = box1.min().asNativeVec3d().z;
        double b1MaxX = box1.max().asNativeVec3d().x;
        double b1MaxY = box1.max().asNativeVec3d().y;
        double b1MaxZ = box1.max().asNativeVec3d().z;
        
        double b2MinX = box2.min().asNativeVec3d().x;
        double b2MinY = box2.min().asNativeVec3d().y;
        double b2MinZ = box2.min().asNativeVec3d().z;
        double b2MaxX = box2.max().asNativeVec3d().x;
        double b2MaxY = box2.max().asNativeVec3d().y;
        double b2MaxZ = box2.max().asNativeVec3d().z;
        
        return b1MinX <= b2MaxX && b1MaxX >= b2MinX &&
               b1MinY <= b2MaxY && b1MaxY >= b2MinY &&
               b1MinZ <= b2MaxZ && b1MaxZ >= b2MinZ;
    }
    
    // Convenience methods for direct access to manager functionality
    public static String createZone(String name, MinecraftBox box) {
        return manager.createZone(name, box);
    }
    
    public static Optional<ZoneData> getZone(String uniqueId) {
        return manager.getZone(uniqueId);
    }
    
    public static List<ZoneData> getZonesByName(String name) {
        return manager.getZonesByName(name);
    }
    
    public static List<ZoneData> getAllZones() {
        return manager.getAllZones();
    }
    
    public static boolean deleteZone(String uniqueId) {
        return manager.deleteZone(uniqueId);
    }
    
    public static boolean updateZoneName(String uniqueId, String newName) {
        return manager.updateZoneName(uniqueId, newName);
    }
    
    public static boolean updateZoneBox(String uniqueId, MinecraftBox newBox) {
        return manager.updateZoneBox(uniqueId, newBox);
    }
    
    public static int getZoneCount() {
        return manager.getZoneCount();
    }
}