package rocamocha.mochamix.plugins;

import circuitlord.reactivemusic.api.ReactiveMusicPlugin;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;
import rocamocha.mochamix.zones.ZoneData;
import rocamocha.mochamix.zones.ZoneUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ZoneAreaPlugin extends ReactiveMusicPlugin {
    public ZoneAreaPlugin() {
        super("mochamix", "zones_and_areas");
    }

    static EventRecord ZONE, AREA;
    
    // Plugin-owned zone tracking
    private static Set<String> currentZoneNames = new HashSet<>();
    
    @Override public void init() {
        registerSongpackEvents("ZONE", "AREA");

        ZONE = SongpackEvent.get("ZONE");
        AREA = SongpackEvent.get("AREA");
    }

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        if (player == null) return;
        
        // Get all zones containing the player
        List<ZoneData> zonesContainingPlayer = ZoneUtils.getZonesContainingPoint(player.location().pos());
        boolean inZone = !zonesContainingPlayer.isEmpty();
        
        // Update the current zone names for specific zone validation
        currentZoneNames.clear();
        for (ZoneData zone : zonesContainingPlayer) {
            currentZoneNames.add(zone.getZoneName());
        }
        
        // Set generic zone/area events for OR logic with other conditions
        boolean inArea = inZone;
        eventMap.put(ZONE, inZone);
        eventMap.put(AREA, inArea);
    }

    /**
     * Example usage methods for zone management:
     */
    
    /**
     * Create a new zone at the player's current location.
     */
    public static String createZoneAtPlayer(MinecraftPlayer player, String zoneName, double radius) {
        return ZoneUtils.createZoneFromCenter(
            zoneName, 
            player.location().pos(), 
            radius, radius, radius
        );
    }
    
    /**
     * Get all zones that contain the player's current position.
     */
    public static List<ZoneData> getZonesContainingPlayer(MinecraftPlayer player) {
        return ZoneUtils.getZonesContainingPoint(player.location().pos());
    }
    
    /**
     * List all zones with their basic info.
     */
    public static List<String> listAllZones() {
        return ZoneUtils.getAllZones().stream()
            .map(zone -> String.format("%s: '%s' (ID: %s)", 
                zone.getZoneName(), 
                zone.getBoxData().toString(), 
                zone.getUniqueId().substring(0, 8) + "..."))
            .toList();
    }
    
    /**
     * Remove a zone by its name (removes first match).
     */
    public static boolean removeZoneByName(String zoneName) {
        List<ZoneData> zones = ZoneUtils.getZonesByName(zoneName);
        if (!zones.isEmpty()) {
            return ZoneUtils.deleteZone(zones.get(0).getUniqueId());
        }
        return false;
    }
    
    /**
     * Get the current zone names that the player is in.
     * Used by SongPicker for zone-specific validation.
     */
    public static Set<String> getCurrentZoneNames() {
        return currentZoneNames;
    }
}
