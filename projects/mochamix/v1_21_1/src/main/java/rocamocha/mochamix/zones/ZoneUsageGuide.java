package rocamocha.mochamix.zones;

import rocamocha.mochamix.api.minecraft.util.MinecraftBox;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;

import java.util.List;
import java.util.Optional;

/**
 * Simple usage guide for the Zone Data Management system.
 * This shows common patterns for zone operations.
 */
public class ZoneUsageGuide {
    
    /**
     * Basic zone creation and management
     */
    public static void quickStart() {
        // Create a zone using coordinates
        MinecraftBox area = ZoneFactory.createBox(100, 64, 200, 150, 80, 250);
        String zoneId = ZoneUtils.createZone("spawn", area);
        
        // Create a zone from center and radius  
        MinecraftVector3 center = ZoneFactory.createVector(0, 64, 0);
        String centeredZoneId = ZoneUtils.createZoneFromCenter("hub", center, 25, 10, 25);
        
        System.out.println("Created zones: " + zoneId + " and " + centeredZoneId);
    }
    
    /**
     * Point testing - check if a location is inside zones
     */
    public static void checkPlayerInZone(MinecraftVector3 playerLocation) {
        // Simple check for any zone with a specific name
        boolean inSpawnZone = ZoneUtils.isPointInZone("spawn", playerLocation);
        
        // Get all zones that contain this point
        List<ZoneData> allZonesHere = ZoneUtils.getZonesContainingPoint(playerLocation);
        
        // Find specific zone by name containing the point
        Optional<ZoneData> spawnZone = ZoneUtils.getFirstZoneContainingPoint("spawn", playerLocation);
        
        if (inSpawnZone) {
            System.out.println("Player is in spawn zone!");
        }
        
        System.out.println("Player is in " + allZonesHere.size() + " zones");
        
        if (spawnZone.isPresent()) {
            System.out.println("Found spawn zone: " + spawnZone.get().getUniqueId());
        }
    }
    
    /**
     * Zone modification operations
     */
    public static void modifyZones(String zoneId) {
        // Update zone name
        boolean renamed = ZoneUtils.updateZoneName(zoneId, "new_name");
        
        // Expand zone by 10 blocks in all directions
        boolean expanded = ZoneUtils.expandZone(zoneId, 10, 5, 10);
        
        // Move zone 50 blocks east
        boolean moved = ZoneUtils.moveZone(zoneId, 50, 0, 0);
        
        System.out.println("Zone operations - renamed: " + renamed + 
                         ", expanded: " + expanded + ", moved: " + moved);
    }
    
    /**
     * Zone information and analysis
     */
    public static void analyzeZone(String zoneId) {
        Optional<ZoneData> zone = ZoneUtils.getZone(zoneId);
        
        if (zone.isPresent()) {
            ZoneData zoneData = zone.get();
            
            // Basic info
            System.out.println("Zone: " + zoneData.getZoneName());
            System.out.println("ID: " + zoneData.getUniqueId());
            
            // Geometric info
            Optional<MinecraftVector3> center = ZoneUtils.getZoneCenter(zoneId);
            Optional<Double> volume = ZoneUtils.getZoneVolume(zoneId);
            
            if (center.isPresent()) {
                MinecraftVector3 c = center.get();
                System.out.printf("Center: (%.1f, %.1f, %.1f)%n", 
                    c.asNativeVec3d().x, c.asNativeVec3d().y, c.asNativeVec3d().z);
            }
            
            if (volume.isPresent()) {
                System.out.println("Volume: " + volume.get() + " blocks³");
            }
            
            // Find overlapping zones
            List<ZoneData> overlapping = ZoneUtils.getOverlappingZones(zoneId);
            System.out.println("Overlapping with " + overlapping.size() + " other zones");
        }
    }
    
    /**
     * List and search operations
     */
    public static void listOperations() {
        // Get all zones
        List<ZoneData> allZones = ZoneUtils.getAllZones();
        System.out.println("Total zones: " + allZones.size());
        
        // Get zones by name
        List<ZoneData> spawnZones = ZoneUtils.getZonesByName("spawn");
        System.out.println("Spawn zones: " + spawnZones.size());
        
        // Display zone info
        for (ZoneData zone : allZones) {
            System.out.printf("Zone '%s' (ID: %s) at %s%n", 
                zone.getZoneName(),
                zone.getUniqueId().substring(0, 8) + "...",
                zone.getBoxData());
        }
    }
    
    /**
     * Cleanup operations
     */
    public static void cleanup() {
        // Delete specific zone
        String someZoneId = "zone-id-here";
        boolean deleted = ZoneUtils.deleteZone(someZoneId);
        
        // Delete all zones with a name (useful for temporary zones)
        ZoneDataManager manager = ZoneDataManager.getInstance();
        int tempZonesDeleted = manager.deleteZonesByName("temporary");
        
        // Force save to ensure persistence
        manager.forceSave();
        
        System.out.println("Deleted zone: " + deleted);
        System.out.println("Deleted temporary zones: " + tempZonesDeleted);
    }
    
    /**
     * Example usage in a ReactiveMusicPlugin context
     */
    public static void pluginExample(MinecraftVector3 playerLocation) {
        // Check if player is in any PvP zone
        boolean inPvpZone = ZoneUtils.isPointInZone("pvp", playerLocation);
        
        // Check if player is in safe zone
        boolean inSafeZone = ZoneUtils.isPointInZone("safe", playerLocation);
        
        // Create events for songpack system based on location
        if (inPvpZone) {
            System.out.println("Player entered PvP area - trigger combat music");
        } else if (inSafeZone) {
            System.out.println("Player in safe area - trigger calm music");
        }
        
        // Get all zones player is currently in for detailed tracking
        List<ZoneData> currentZones = ZoneUtils.getZonesContainingPoint(playerLocation);
        if (!currentZones.isEmpty()) {
            System.out.println("Player in zones: " + 
                currentZones.stream()
                    .map(ZoneData::getZoneName)
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b));
        }
    }
}