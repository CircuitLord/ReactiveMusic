package rocamocha.mochamix.zones;

import com.google.gson.annotations.SerializedName;
import rocamocha.mochamix.api.minecraft.util.MinecraftBox;

import java.util.UUID;

/**
 * Represents zone data that can be serialized to/from JSON.
 * Contains a unique identifier, human-readable name, and bounding box data.
 */
public class ZoneData {
    @SerializedName("id")
    private final String uniqueId;
    
    @SerializedName("name")
    private String zoneName;
    
    @SerializedName("box")
    private BoxData boxData;
    
    @SerializedName("created_at")
    private long createdTimestamp;
    
    @SerializedName("modified_at")
    private long modifiedTimestamp;
    
    // Default constructor for GSON
    public ZoneData() {
        this.uniqueId = UUID.randomUUID().toString();
        this.createdTimestamp = System.currentTimeMillis();
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    public ZoneData(String zoneName, MinecraftBox box) {
        this();
        this.zoneName = zoneName;
        this.boxData = BoxData.fromMinecraftBox(box);
    }
    
    // Getters
    public String getUniqueId() { return uniqueId; }
    public String getZoneName() { return zoneName; }
    public BoxData getBoxData() { return boxData; }
    public long getCreatedTimestamp() { return createdTimestamp; }
    public long getModifiedTimestamp() { return modifiedTimestamp; }
    
    // Setters
    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    public void setBoxData(MinecraftBox box) {
        this.boxData = BoxData.fromMinecraftBox(box);
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    public void setBoxData(BoxData boxData) {
        this.boxData = boxData;
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Convert the stored box data back to a MinecraftBox for use in game.
     */
    public MinecraftBox toMinecraftBox() {
        if (boxData == null) return null;
        return boxData.toMinecraftBox();
    }
    
    /**
     * Get coordinate access methods for convenience
     */
    public double minX() { return boxData != null ? boxData.minX : 0; }
    public double minY() { return boxData != null ? boxData.minY : 0; }
    public double minZ() { return boxData != null ? boxData.minZ : 0; }
    public double maxX() { return boxData != null ? boxData.maxX : 0; }
    public double maxY() { return boxData != null ? boxData.maxY : 0; }
    public double maxZ() { return boxData != null ? boxData.maxZ : 0; }
    
    @Override
    public String toString() {
        return String.format("ZoneData{id='%s', name='%s', box=%s}", 
            uniqueId, zoneName, boxData);
    }
    
    /**
     * Nested class to store box coordinates in JSON-friendly format.
     */
    public static class BoxData {
        @SerializedName("min_x")
        public double minX;
        
        @SerializedName("min_y") 
        public double minY;
        
        @SerializedName("min_z")
        public double minZ;
        
        @SerializedName("max_x")
        public double maxX;
        
        @SerializedName("max_y")
        public double maxY;
        
        @SerializedName("max_z")
        public double maxZ;
        
        // Default constructor for GSON
        public BoxData() {}
        
        public BoxData(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
        
        public static BoxData fromMinecraftBox(MinecraftBox box) {
            return new BoxData(
                box.min().asNativeVec3d().x, box.min().asNativeVec3d().y, box.min().asNativeVec3d().z,
                box.max().asNativeVec3d().x, box.max().asNativeVec3d().y, box.max().asNativeVec3d().z
            );
        }
        
        public MinecraftBox toMinecraftBox() {
            return new rocamocha.mochamix.impl.box.BoxSocket(
                new rocamocha.mochamix.impl.vector3.Vector3Socket(minX, minY, minZ),
                new rocamocha.mochamix.impl.vector3.Vector3Socket(maxX, maxY, maxZ)
            );
        }
        
        @Override
        public String toString() {
            return String.format("Box{min=(%.1f,%.1f,%.1f), max=(%.1f,%.1f,%.1f)}", 
                minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}