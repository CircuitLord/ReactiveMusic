package rocamocha.mochamix.zones;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import rocamocha.mochamix.api.minecraft.util.MinecraftBox;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages zone data persistence in JSON format within the world directory.
 * Provides thread-safe operations for creating, reading, updating, and deleting zones.
 */
public class ZoneDataManager {
    private static final String ZONES_FILE_NAME = "mochamix_zones.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    // In-memory cache of zone data for performance
    private final Map<String, ZoneData> zoneCache = new ConcurrentHashMap<>();
    private boolean cacheLoaded = false;
    
    private static ZoneDataManager instance;
    
    private ZoneDataManager() {}
    
    public static ZoneDataManager getInstance() {
        if (instance == null) {
            instance = new ZoneDataManager();
        }
        return instance;
    }
    
    /**
     * Get the path to the zones JSON file in the current world directory.
     * Uses multiple fallback strategies to handle various game states.
     */
    private Path getZonesFilePath() {
        MinecraftClient client = MinecraftClient.getInstance();
        Path gameDir = client.runDirectory.toPath();
        
        // Strategy 1: Try integrated server save path (singleplayer)
        if (client.getServer() != null) {
            try {
                Path savePath = client.getServer().getSavePath(null);
                if (savePath != null) {
                    return savePath.resolve(ZONES_FILE_NAME);
                }
            } catch (Exception e) {
                // getSavePath can throw exceptions, continue to fallbacks
            }
            
            // Strategy 2: Try to get world name from server properties
            try {
                String worldName = client.getServer().getSaveProperties().getLevelName();
                if (worldName != null && !worldName.isEmpty()) {
                    Path worldDir = gameDir.resolve("saves").resolve(worldName);
                    Files.createDirectories(worldDir);
                    return worldDir.resolve(ZONES_FILE_NAME);
                }
            } catch (Exception e) {
                // Continue to next strategy
            }
        }
        
        // Strategy 3: Multiplayer server
        if (client.getCurrentServerEntry() != null) {
            try {
                String serverAddress = client.getCurrentServerEntry().address.replaceAll("[^a-zA-Z0-9.-]", "_");
                Path serverDir = gameDir.resolve("servers").resolve(serverAddress);
                Files.createDirectories(serverDir);
                return serverDir.resolve(ZONES_FILE_NAME);
            } catch (Exception e) {
                // Continue to next strategy
            }
        }
        
        // Strategy 4: Use world registry key (works for both SP and MP)
        if (client.world != null) {
            try {
                String worldKey = client.world.getRegistryKey().getValue().toString().replaceAll("[^a-zA-Z0-9.-]", "_");
                Path worldDir = gameDir.resolve("zones").resolve(worldKey);
                Files.createDirectories(worldDir);
                return worldDir.resolve(ZONES_FILE_NAME);
            } catch (Exception e) {
                // Continue to next strategy
            }
        }
        
        // Strategy 5: Try to infer world name from current session
        try {
            // Check if we can get session info
            if (client.getSession() != null && client.world != null) {
                // Create a unique identifier based on session and world dimension
                String sessionId = client.getSession().getUuidOrNull() != null ? 
                    client.getSession().getUuidOrNull().toString().substring(0, 8) : "unknown";
                String dimension = client.world.getRegistryKey().getValue().getPath();
                String identifier = sessionId + "_" + dimension;
                
                Path sessionDir = gameDir.resolve("zones").resolve("session_" + identifier);
                Files.createDirectories(sessionDir);
                return sessionDir.resolve(ZONES_FILE_NAME);
            }
        } catch (Exception e) {
            // Continue to final fallback
        }
        
        // Final fallback: Use a general zones directory in game folder
        try {
            Path zonesDir = gameDir.resolve("zones").resolve("default");
            Files.createDirectories(zonesDir);
            return zonesDir.resolve(ZONES_FILE_NAME);
        } catch (IOException e) {
            // Absolute final fallback: root game directory
            return gameDir.resolve(ZONES_FILE_NAME);
        }
    }
    
    /**
     * Debug method to get information about the current zones file path and context.
     * Useful for troubleshooting path resolution issues.
     */
    public String getZonesFileDebugInfo() {
        MinecraftClient client = MinecraftClient.getInstance();
        StringBuilder info = new StringBuilder();
        
        info.append("=== Zone File Debug Info ===\n");
        info.append("Game Directory: ").append(client.runDirectory.getAbsolutePath()).append("\n\n");
        
        // Test each strategy
        info.append("=== Strategy Testing ===\n");
        
        // Strategy 1: Integrated server save path
        info.append("1. Integrated Server:\n");
        info.append("   Has Server: ").append(client.getServer() != null).append("\n");
        if (client.getServer() != null) {
            try {
                Path savePath = client.getServer().getSavePath(null);
                info.append("   Save Path: ").append(savePath != null ? savePath.toString() : "NULL").append("\n");
            } catch (Exception e) {
                info.append("   Save Path Error: ").append(e.getMessage()).append("\n");
            }
            
            try {
                String worldName = client.getServer().getSaveProperties().getLevelName();
                info.append("   World Name: ").append(worldName != null ? worldName : "NULL").append("\n");
            } catch (Exception e) {
                info.append("   World Name Error: ").append(e.getMessage()).append("\n");
            }
        }
        
        // Strategy 2: Server entry (multiplayer)
        info.append("2. Server Entry:\n");
        info.append("   Has Server Entry: ").append(client.getCurrentServerEntry() != null).append("\n");
        if (client.getCurrentServerEntry() != null) {
            info.append("   Server Address: ").append(client.getCurrentServerEntry().address).append("\n");
        }
        
        // Strategy 3: World registry key
        info.append("3. World Registry:\n");
        info.append("   Has World: ").append(client.world != null).append("\n");
        if (client.world != null) {
            info.append("   Registry Key: ").append(client.world.getRegistryKey().getValue().toString()).append("\n");
        }
        
        // Strategy 4: Session info
        info.append("4. Session Info:\n");
        info.append("   Has Session: ").append(client.getSession() != null).append("\n");
        if (client.getSession() != null) {
            info.append("   Session UUID: ").append(client.getSession().getUuidOrNull() != null ? 
                client.getSession().getUuidOrNull().toString() : "NULL").append("\n");
        }
        
        info.append("\n=== Final Result ===\n");
        try {
            Path zonesFile = getZonesFilePath();
            info.append("Resolved Path: ").append(zonesFile.toString()).append("\n");
            info.append("Parent Exists: ").append(Files.exists(zonesFile.getParent())).append("\n");
            info.append("File Exists: ").append(Files.exists(zonesFile)).append("\n");
            info.append("Directory Writable: ").append(Files.isWritable(zonesFile.getParent())).append("\n");
        } catch (Exception e) {
            info.append("Path Resolution Error: ").append(e.getMessage()).append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Load zones from JSON file into memory cache.
     */
    private synchronized void loadZones() {
        if (cacheLoaded) return;
        
        Path zonesFile = getZonesFilePath();
        zoneCache.clear();
        
        if (!Files.exists(zonesFile)) {
            cacheLoaded = true;
            return;
        }
        
        try {
            String json = Files.readString(zonesFile);
            Type listType = new TypeToken<List<ZoneData>>(){}.getType();
            List<ZoneData> zones = GSON.fromJson(json, listType);
            
            if (zones != null) {
                for (ZoneData zone : zones) {
                    zoneCache.put(zone.getUniqueId(), zone);
                }
            }
            
            cacheLoaded = true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load zones from " + zonesFile, e);
        }
    }
    
    /**
     * Save all zones from memory cache to JSON file.
     */
    private synchronized void saveZones() {
        if (!cacheLoaded) {
            loadZones();
        }
        
        Path zonesFile;
        try {
            zonesFile = getZonesFilePath();
            if (zonesFile == null) {
                throw new RuntimeException("Unable to determine zones file path - no valid save location found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve zones file path: " + e.getMessage(), e);
        }
        
        try {
            // Ensure parent directory exists
            Path parentDir = zonesFile.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            
            List<ZoneData> zoneList = new ArrayList<>(zoneCache.values());
            String json = GSON.toJson(zoneList);
            
            Files.writeString(zonesFile, json, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.WRITE, 
                StandardOpenOption.TRUNCATE_EXISTING);
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to save zones to " + zonesFile + ". Debug info: " + getZonesFileDebugInfo(), e);
        }
    }
    
    /**
     * Create a new zone with the given name and bounding box.
     * Returns the unique ID of the created zone.
     */
    public String createZone(String zoneName, MinecraftBox box) {
        if (!cacheLoaded) {
            loadZones();
        }
        
        ZoneData zone = new ZoneData(zoneName, box);
        zoneCache.put(zone.getUniqueId(), zone);
        saveZones();
        
        return zone.getUniqueId();
    }
    
    /**
     * Get a zone by its unique ID.
     */
    public Optional<ZoneData> getZone(String uniqueId) {
        if (!cacheLoaded) {
            loadZones();
        }
        
        return Optional.ofNullable(zoneCache.get(uniqueId));
    }
    
    /**
     * Get all zones with the specified name.
     * Note: Multiple zones can have the same name but different unique IDs.
     */
    public List<ZoneData> getZonesByName(String zoneName) {
        if (!cacheLoaded) {
            loadZones();
        }
        
        return zoneCache.values().stream()
                .filter(zone -> zoneName.equals(zone.getZoneName()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get all zones.
     */
    public List<ZoneData> getAllZones() {
        if (!cacheLoaded) {
            loadZones();
        }
        
        return new ArrayList<>(zoneCache.values());
    }
    
    /**
     * Update an existing zone's name.
     * Returns true if the zone was found and updated.
     */
    public boolean updateZoneName(String uniqueId, String newZoneName) {
        if (!cacheLoaded) {
            loadZones();
        }
        
        ZoneData zone = zoneCache.get(uniqueId);
        if (zone != null) {
            zone.setZoneName(newZoneName);
            saveZones();
            return true;
        }
        
        return false;
    }
    
    /**
     * Update an existing zone's bounding box.
     * Returns true if the zone was found and updated.
     */
    public boolean updateZoneBox(String uniqueId, MinecraftBox newBox) {
        if (!cacheLoaded) {
            loadZones();
        }
        
        ZoneData zone = zoneCache.get(uniqueId);
        if (zone != null) {
            zone.setBoxData(newBox);
            saveZones();
            return true;
        }
        
        return false;
    }
    
    /**
     * Update both name and box of an existing zone.
     * Returns true if the zone was found and updated.
     */
    public boolean updateZone(String uniqueId, String newZoneName, MinecraftBox newBox) {
        if (!cacheLoaded) {
            loadZones();
        }
        
        ZoneData zone = zoneCache.get(uniqueId);
        if (zone != null) {
            zone.setZoneName(newZoneName);
            zone.setBoxData(newBox);
            saveZones();
            return true;
        }
        
        return false;
    }
    
    /**
     * Delete a zone by its unique ID.
     * Returns true if the zone was found and deleted.
     */
    public boolean deleteZone(String uniqueId) {
        if (!cacheLoaded) {
            loadZones();
        }
        
        ZoneData removed = zoneCache.remove(uniqueId);
        if (removed != null) {
            saveZones();
            return true;
        }
        
        return false;
    }
    
    /**
     * Delete all zones with the specified name.
     * Returns the number of zones deleted.
     */
    public int deleteZonesByName(String zoneName) {
        if (!cacheLoaded) {
            loadZones();
        }
        
        List<String> toRemove = zoneCache.values().stream()
                .filter(zone -> zoneName.equals(zone.getZoneName()))
                .map(ZoneData::getUniqueId)
                .collect(Collectors.toList());
        
        int deletedCount = 0;
        for (String id : toRemove) {
            if (zoneCache.remove(id) != null) {
                deletedCount++;
            }
        }
        
        if (deletedCount > 0) {
            saveZones();
        }
        
        return deletedCount;
    }
    
    /**
     * Check if a zone with the given unique ID exists.
     */
    public boolean zoneExists(String uniqueId) {
        if (!cacheLoaded) {
            loadZones();
        }
        
        return zoneCache.containsKey(uniqueId);
    }
    
    /**
     * Get the number of zones currently stored.
     */
    public int getZoneCount() {
        if (!cacheLoaded) {
            loadZones();
        }
        
        return zoneCache.size();
    }
    
    /**
     * Clear all zones from memory and storage.
     * USE WITH CAUTION - this will permanently delete all zone data!
     */
    public void clearAllZones() {
        zoneCache.clear();
        saveZones();
    }
    
    /**
     * Force reload zones from disk, discarding any cached changes.
     */
    public void reloadZones() {
        cacheLoaded = false;
        loadZones();
    }
    
    /**
     * Force save current cache to disk.
     */
    public void forceSave() {
        if (cacheLoaded) {
            saveZones();
        }
    }
}