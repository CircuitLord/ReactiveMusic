package rocamocha.mochamix.zones;

import rocamocha.mochamix.api.minecraft.util.MinecraftBox;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;

import java.util.List;
import java.util.Optional;

/**
 * Example usage and testing class for the Zone Data Management system.
 * This shows how to use the various zone management operations.
 */
public class ZoneManagementExamples {
    
    /**
     * Example: Basic zone creation and retrieval
     */
    public static void basicZoneOperations() {
        // Create a simple zone using factory methods
        MinecraftBox shopArea = ZoneFactory.createBox(100, 64, 200, 150, 80, 250);
        String shopZoneId = ZoneUtils.createZone("shop", shopArea);
        
        // Create another zone using center and radius
        MinecraftVector3 spawnPoint = ZoneFactory.createVector(0, 64, 0);
        String spawnZoneId = ZoneUtils.createZoneFromCenter("spawn", spawnPoint, 25, 10, 25);
        
        // Retrieve zones and display info
        Optional<ZoneData> shopZone = ZoneUtils.getZone(shopZoneId);
        List<ZoneData> allShopZones = ZoneUtils.getZonesByName("shop");
        
        System.out.println("Shop zone created with ID: " + shopZoneId);
        System.out.println("Spawn zone created with ID: " + spawnZoneId);
        System.out.println("Total zones: " + ZoneUtils.getZoneCount());
        
        if (shopZone.isPresent()) {
            System.out.println("Shop zone found: " + shopZone.get().getZoneName());
        }
        System.out.println("All shop zones: " + allShopZones.size());
    }
    
    /**
     * Example: Zone modification operations
     */
    public static void modifyZoneOperations(String zoneId) {
        // Update zone name
        ZoneUtils.updateZoneName(zoneId, "updated_shop");
        
        // Expand the zone by 10 blocks in each direction
        ZoneUtils.expandZone(zoneId, 10, 5, 10);
        
        // Move the zone 50 blocks east
        ZoneUtils.moveZone(zoneId, 50, 0, 0);
        
        // Update the entire bounding box
        MinecraftBox newBox = ZoneFactory.createBox(200, 60, 300, 280, 90, 380);
        ZoneUtils.updateZoneBox(zoneId, newBox);
    }
    
    /**
     * Example: Spatial queries and point testing
     */
    public static void spatialQueries() {
        MinecraftVector3 testPoint = ZoneFactory.createVector(125, 70, 225);
        
        // Check if point is in any shop zone
        boolean inShop = ZoneUtils.isPointInZone("shop", testPoint);
        
        // Get all zones containing this point
        List<ZoneData> containingZones = ZoneUtils.getZonesContainingPoint(testPoint);
        
        // Find specific zone containing point
        Optional<ZoneData> firstShopZone = ZoneUtils.getFirstZoneContainingPoint("shop", testPoint);
        
        System.out.println("Point in shop: " + inShop);
        System.out.println("Zones containing point: " + containingZones.size());
        if (firstShopZone.isPresent()) {
            System.out.println("First shop zone found: " + firstShopZone.get().getZoneName());
        }
    }
    
    /**
     * Example: Zone analysis and utilities
     */
    public static void zoneAnalysis(String zoneId1, String zoneId2) {
        // Get zone center
        Optional<MinecraftVector3> center = ZoneUtils.getZoneCenter(zoneId1);
        
        // Get zone volume
        Optional<Double> volume = ZoneUtils.getZoneVolume(zoneId1);
        
        // Check if zones overlap
        boolean overlap = ZoneUtils.doZonesOverlap(zoneId1, zoneId2);
        
        // Find all zones that overlap with a specific zone
        List<ZoneData> overlapping = ZoneUtils.getOverlappingZones(zoneId1);
        
        System.out.println("Zone center: " + center.orElse(null));
        System.out.println("Zone volume: " + volume.orElse(0.0));
        System.out.println("Zones overlap: " + overlap);
        System.out.println("Overlapping zones: " + overlapping.size());
    }
    
    /**
     * Example: Cleanup and deletion
     */
    public static void cleanupOperations() {
        // Delete specific zone by ID
        String zoneId = "some-zone-id-here";
        boolean deleted = ZoneUtils.deleteZone(zoneId);
        
        // Delete all zones with a specific name
        ZoneDataManager manager = ZoneDataManager.getInstance();
        int deletedCount = manager.deleteZonesByName("temporary");
        
        // List all remaining zones
        List<ZoneData> allZones = ZoneUtils.getAllZones();
        
        System.out.println("Zone deleted: " + deleted);
        System.out.println("Temporary zones deleted: " + deletedCount);
        System.out.println("Remaining zones: " + allZones.size());
    }
    
    /**
     * Example: Working with JSON persistence
     */
    public static void persistenceOperations() {
        ZoneDataManager manager = ZoneDataManager.getInstance();
        
        // Force save current state to disk
        manager.forceSave();
        
        // Reload from disk (discarding any unsaved changes)
        manager.reloadZones();
        
        // Check if zone exists
        boolean exists = manager.zoneExists("some-zone-id");
        
        System.out.println("Zone exists: " + exists);
        System.out.println("Total zones loaded: " + manager.getZoneCount());
    }
    
    /**
     * Example: Creating zones from player positions or commands
     */
    public static String createZoneFromTwoCorners(MinecraftVector3 pos1, MinecraftVector3 pos2, String zoneName) {
        // This would typically be called from a command where player selects two points
        return ZoneUtils.createZoneFromCorners(zoneName, pos1, pos2);
    }
    
    /**
     * Example: Zone information display
     */
    public static void displayZoneInfo(String uniqueId) {
        Optional<ZoneData> zoneOpt = ZoneUtils.getZone(uniqueId);
        
        if (zoneOpt.isPresent()) {
            ZoneData zone = zoneOpt.get();
            System.out.println("Zone Info:");
            System.out.println("  ID: " + zone.getUniqueId());
            System.out.println("  Name: " + zone.getZoneName());
            System.out.println("  Box: " + zone.getBoxData());
            System.out.println("  Created: " + new java.util.Date(zone.getCreatedTimestamp()));
            System.out.println("  Modified: " + new java.util.Date(zone.getModifiedTimestamp()));
            
            // Additional calculations
            Optional<MinecraftVector3> center = ZoneUtils.getZoneCenter(uniqueId);
            Optional<Double> volume = ZoneUtils.getZoneVolume(uniqueId);
            
            if (center.isPresent()) {
                System.out.println("  Center: " + center.get());
            }
            if (volume.isPresent()) {
                System.out.println("  Volume: " + String.format("%.2f blocks³", volume.get()));
            }
        } else {
            System.out.println("Zone not found: " + uniqueId);
        }
    }
}