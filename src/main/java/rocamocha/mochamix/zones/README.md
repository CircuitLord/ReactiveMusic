# Zone Data Management System

This system provides comprehensive zone management with JSON persistence in the world directory. It allows you to create, modify, query, and delete zones with unique identifiers separate from their names.

## Core Components

### 1. **ZoneData** - Data Structure
- Stores zone information with unique UUID, name, bounding box, and timestamps
- Serializes to/from JSON format for persistence
- Contains nested `BoxData` class for coordinate storage

### 2. **ZoneDataManager** - Persistence Layer
- Manages JSON file storage in world/server directory
- Provides thread-safe CRUD operations
- Handles in-memory caching for performance
- Automatically creates directories and handles file I/O

### 3. **ZoneUtils** - High-Level Operations
- Convenient methods for common zone operations
- Spatial queries (point-in-zone, overlapping zones)
- Zone manipulation (expand, move, analyze)
- Factory-style creation methods

### 4. **ZoneFactory** - Object Creation
- Static factory methods for creating MinecraftBox and MinecraftVector3 instances
- Handles the complexity of working with API interfaces
- Provides convenient creation patterns

## Key Features

### ✅ **Unique Identifiers**
- Each zone has a UUID separate from its display name
- Multiple zones can have the same name but different IDs
- Delete by unique ID to avoid ambiguity

### ✅ **JSON Persistence**
- Automatic saving to `{world}/mochamix_zones.json`
- Multiplayer support with per-server directories
- Pretty-printed JSON for readability

### ✅ **Spatial Operations**
- Point-in-zone testing
- Zone overlap detection
- Center point calculation
- Volume calculation
- Expansion and movement

### ✅ **Thread Safety**
- Synchronized operations for concurrent access
- Safe for use in game tick contexts

## Usage Examples

### Basic Zone Creation
```java
// Create zone from coordinates
MinecraftBox area = ZoneFactory.createBox(100, 64, 200, 150, 80, 250);
String zoneId = ZoneUtils.createZone("spawn", area);

// Create zone from center and radius
MinecraftVector3 center = ZoneFactory.createVector(0, 64, 0);
String centeredZoneId = ZoneUtils.createZoneFromCenter("hub", center, 25, 10, 25);
```

### Point Testing
```java
MinecraftVector3 playerLocation = // ... get from player
boolean inSpawnZone = ZoneUtils.isPointInZone("spawn", playerLocation);
List<ZoneData> allZonesHere = ZoneUtils.getZonesContainingPoint(playerLocation);
```

### Zone Modification
```java
// Rename zone
ZoneUtils.updateZoneName(zoneId, "new_name");

// Expand zone by 10 blocks in all directions  
ZoneUtils.expandZone(zoneId, 10, 5, 10);

// Move zone 50 blocks east
ZoneUtils.moveZone(zoneId, 50, 0, 0);
```

### Zone Analysis
```java
Optional<MinecraftVector3> center = ZoneUtils.getZoneCenter(zoneId);
Optional<Double> volume = ZoneUtils.getZoneVolume(zoneId);
List<ZoneData> overlapping = ZoneUtils.getOverlappingZones(zoneId);
```

### Cleanup Operations
```java
// Delete specific zone by ID
ZoneUtils.deleteZone(zoneId);

// Delete all zones with a name
ZoneDataManager manager = ZoneDataManager.getInstance();
int deletedCount = manager.deleteZonesByName("temporary");
```

## Integration with ReactiveMusicPlugin

The system integrates seamlessly with your ZoneAreaPlugin:

```java
@Override
public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
    if (player == null) return;
    
    // Check if player is in any zone or area
    boolean inZone = ZoneUtils.isPointInZone("ZONE", player.location().pos());
    boolean inArea = ZoneUtils.isPointInZone("AREA", player.location().pos());
    
    // Set event states for songpack system
    eventMap.put(ZONE, inZone);
    eventMap.put(AREA, inArea);
}
```

## File Storage

### Location
- **Singleplayer**: `{world_folder}/mochamix_zones.json`
- **Multiplayer**: `{minecraft_folder}/servers/{server_address}/mochamix_zones.json`

### Format
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "spawn",
    "box": {
      "min_x": 95.0,
      "min_y": 59.0,
      "min_z": 195.0,
      "max_x": 155.0,
      "max_y": 85.0,
      "max_z": 255.0
    },
    "created_at": 1704067200000,
    "modified_at": 1704067200000
  }
]
```

## Performance Considerations

- **In-Memory Caching**: Zones are cached in memory for fast access
- **Lazy Loading**: JSON is only loaded when first accessed
- **Batch Operations**: File I/O is minimized through caching
- **Thread Safety**: Safe for concurrent access from game threads

## Error Handling

- **Missing Files**: Automatically creates directories and files as needed
- **Corrupted JSON**: Graceful fallback with error logging
- **Invalid Operations**: Returns false/empty rather than throwing exceptions
- **Null Safety**: Null-safe throughout the API

This system provides everything you need for persistent zone management with unique identifiers, spatial queries, and seamless integration with your plugin system!