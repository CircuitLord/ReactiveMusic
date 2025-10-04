package rocamocha.mochamix.render;

import rocamocha.mochamix.api.minecraft.util.MinecraftBox;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Debug renderer for zone visualization that integrates with Minecraft's debug rendering system.
 * Similar to ChunkBorderDebugRenderer but for music zones and other custom areas.
 * Renders persistent wireframe boxes that appear every frame during world rendering.
 */
public class ZoneDebugRenderer {
    
    // Store active zones to render
    private static final Map<String, ZoneData> activeZones = new ConcurrentHashMap<>();
    private static boolean enabled = false;
    
    /**
     * Data class to hold zone rendering information
     */
    public static class ZoneData {
        public final MinecraftBox box;
        public final float red, green, blue, alpha;
        public final String label;
        public final long createdTime;
        
        public ZoneData(MinecraftBox box, float red, float green, float blue, float alpha, String label) {
            this.box = box;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            this.label = label;
            this.createdTime = System.currentTimeMillis();
        }
        
        public ZoneData(MinecraftBox box, String label) {
            // Default to cyan color like chunk boundaries
            this(box, 0.0f, 1.0f, 1.0f, 0.8f, label);
        }
    }
    
    /**
     * Add or update a zone for rendering
     */
    public static void addZone(String id, MinecraftBox box, String label) {
        activeZones.put(id, new ZoneData(box, label));
    }
    
    /**
     * Add or update a zone with custom color
     */
    public static void addZone(String id, MinecraftBox box, float red, float green, float blue, float alpha, String label) {
        activeZones.put(id, new ZoneData(box, red, green, blue, alpha, label));
    }
    
    /**
     * Remove a zone from rendering
     */
    public static void removeZone(String id) {
        activeZones.remove(id);
    }
    
    /**
     * Clear all zones
     */
    public static void clearZones() {
        activeZones.clear();
    }
    
    /**
     * Get list of active zone IDs
     */
    public static List<String> getActiveZones() {
        return new ArrayList<>(activeZones.keySet());
    }
    
    /**
     * Enable/disable zone rendering
     */
    public static void setEnabled(boolean enabled) {
        ZoneDebugRenderer.enabled = enabled;
    }
    
    /**
     * Check if zone rendering is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Main render method called during world rendering
     * This should be called from a mixin or event handler during the world render pass
     */
    public static void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ) {
        if (!enabled || activeZones.isEmpty()) {
            return;
        }
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        
        // Render transparent faces first (behind wireframes)  
        // Use a simpler render layer for the faces
        VertexConsumer facesConsumer = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        for (Map.Entry<String, ZoneData> entry : activeZones.entrySet()) {
            ZoneData zone = entry.getValue();
            renderZoneFaces(facesConsumer, positionMatrix, normalMatrix, zone, cameraX, cameraY, cameraZ);
        }
        
        // Render wireframe edges on top for clear definition
        VertexConsumer wireframeConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
        for (Map.Entry<String, ZoneData> entry : activeZones.entrySet()) {
            ZoneData zone = entry.getValue();
            renderZoneWireframe(wireframeConsumer, positionMatrix, normalMatrix, zone, cameraX, cameraY, cameraZ);
        }
    }
    
    /**
     * Render a single zone as a wireframe box
     */
    private static void renderZoneWireframe(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, ZoneData zone, double cameraX, double cameraY, double cameraZ) {
        MinecraftVector3 min = zone.box.min();
        MinecraftVector3 max = zone.box.max();
        
        Vec3d minVec = min.asNativeVec3d();
        Vec3d maxVec = max.asNativeVec3d();
        
        // Offset by camera position for proper world space rendering
        double minX = minVec.x - cameraX;
        double minY = minVec.y - cameraY;
        double minZ = minVec.z - cameraZ;
        double maxX = maxVec.x - cameraX;
        double maxY = maxVec.y - cameraY;
        double maxZ = maxVec.z - cameraZ;
        
        // Draw the 12 edges of the box
        // Bottom face edges
        drawLine(vertexConsumer, positionMatrix, normalMatrix, minX, minY, minZ, maxX, minY, minZ, zone.red, zone.green, zone.blue, zone.alpha);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, maxX, minY, minZ, maxX, minY, maxZ, zone.red, zone.green, zone.blue, zone.alpha);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, maxX, minY, maxZ, minX, minY, maxZ, zone.red, zone.green, zone.blue, zone.alpha);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, minX, minY, maxZ, minX, minY, minZ, zone.red, zone.green, zone.blue, zone.alpha);
        
        // Top face edges
        drawLine(vertexConsumer, positionMatrix, normalMatrix, minX, maxY, minZ, maxX, maxY, minZ, zone.red, zone.green, zone.blue, zone.alpha);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, maxX, maxY, minZ, maxX, maxY, maxZ, zone.red, zone.green, zone.blue, zone.alpha);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, maxX, maxY, maxZ, minX, maxY, maxZ, zone.red, zone.green, zone.blue, zone.alpha);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, minX, maxY, maxZ, minX, maxY, minZ, zone.red, zone.green, zone.blue, zone.alpha);
        
        // Vertical edges
        drawLine(vertexConsumer, positionMatrix, normalMatrix, minX, minY, minZ, minX, maxY, minZ, zone.red, zone.green, zone.blue, zone.alpha);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, maxX, minY, minZ, maxX, maxY, minZ, zone.red, zone.green, zone.blue, zone.alpha);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, maxX, minY, maxZ, maxX, maxY, maxZ, zone.red, zone.green, zone.blue, zone.alpha);
        drawLine(vertexConsumer, positionMatrix, normalMatrix, minX, minY, maxZ, minX, maxY, maxZ, zone.red, zone.green, zone.blue, zone.alpha);
    }
    
    /**
     * Draw a single line between two points
     */
    private static void drawLine(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix,
                                double x1, double y1, double z1, double x2, double y2, double z2, 
                                float red, float green, float blue, float alpha) {
        // Calculate line direction for normal vector
        float dx = (float)(x2 - x1);
        float dy = (float)(y2 - y1);
        float dz = (float)(z2 - z1);
        
        // Normalize the direction vector
        float length = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (length > 0.0001f) {
            dx /= length;
            dy /= length;
            dz /= length;
        } else {
            // Default normal if line has zero length
            dx = 0f; dy = 1f; dz = 0f;
        }
        
        // Transform normal vector using the normal matrix
        float nx = normalMatrix.m00() * dx + normalMatrix.m01() * dy + normalMatrix.m02() * dz;
        float ny = normalMatrix.m10() * dx + normalMatrix.m11() * dy + normalMatrix.m12() * dz;
        float nz = normalMatrix.m20() * dx + normalMatrix.m21() * dy + normalMatrix.m22() * dz;
        
        // Create vertices with position, color, and transformed normal
        vertexConsumer.vertex(positionMatrix, (float)x1, (float)y1, (float)z1)
                      .color(red, green, blue, alpha)
                      .normal(nx, ny, nz);
        vertexConsumer.vertex(positionMatrix, (float)x2, (float)y2, (float)z2)
                      .color(red, green, blue, alpha)
                      .normal(nx, ny, nz);
    }
    
    /**
     * Render transparent faces for a single zone
     */
    private static void renderZoneFaces(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix, ZoneData zone, double cameraX, double cameraY, double cameraZ) {
        MinecraftVector3 min = zone.box.min();
        MinecraftVector3 max = zone.box.max();
        
        Vec3d minVec = min.asNativeVec3d();
        Vec3d maxVec = max.asNativeVec3d();
        
        // Offset by camera position for proper world space rendering
        double minX = minVec.x - cameraX;
        double minY = minVec.y - cameraY;
        double minZ = minVec.z - cameraZ;
        double maxX = maxVec.x - cameraX;
        double maxY = maxVec.y - cameraY;
        double maxZ = maxVec.z - cameraZ;
        
        // Use lower alpha for faces so they're translucent
        float faceAlpha = Math.min(0.3f, zone.alpha * 0.4f);
        
        // Bottom face (Y = minY) - looking up from below
        drawQuad(vertexConsumer, positionMatrix, normalMatrix,
            minX, minY, minZ,  minX, minY, maxZ,  maxX, minY, maxZ,  maxX, minY, minZ,
            0f, -1f, 0f, zone.red, zone.green, zone.blue, faceAlpha);
        
        // Top face (Y = maxY) - looking down from above
        drawQuad(vertexConsumer, positionMatrix, normalMatrix,
            minX, maxY, minZ,  maxX, maxY, minZ,  maxX, maxY, maxZ,  minX, maxY, maxZ,
            0f, 1f, 0f, zone.red, zone.green, zone.blue, faceAlpha);
        
        // North face (Z = minZ) - looking south 
        drawQuad(vertexConsumer, positionMatrix, normalMatrix,
            minX, minY, minZ,  maxX, minY, minZ,  maxX, maxY, minZ,  minX, maxY, minZ,
            0f, 0f, -1f, zone.red, zone.green, zone.blue, faceAlpha);
        
        // South face (Z = maxZ) - looking north
        drawQuad(vertexConsumer, positionMatrix, normalMatrix,
            minX, minY, maxZ,  minX, maxY, maxZ,  maxX, maxY, maxZ,  maxX, minY, maxZ,
            0f, 0f, 1f, zone.red, zone.green, zone.blue, faceAlpha);
        
        // West face (X = minX) - looking east
        drawQuad(vertexConsumer, positionMatrix, normalMatrix,
            minX, minY, minZ,  minX, maxY, minZ,  minX, maxY, maxZ,  minX, minY, maxZ,
            -1f, 0f, 0f, zone.red, zone.green, zone.blue, faceAlpha);
        
        // East face (X = maxX) - looking west
        drawQuad(vertexConsumer, positionMatrix, normalMatrix,
            maxX, minY, minZ,  maxX, minY, maxZ,  maxX, maxY, maxZ,  maxX, maxY, minZ,
            1f, 0f, 0f, zone.red, zone.green, zone.blue, faceAlpha);
    }
    
    /**
     * Draw a quad (4 vertices) using the debug quads vertex format
     * RenderLayer.getDebugQuads() expects exactly 4 vertices per quad
     */
    private static void drawQuad(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix,
                                double x1, double y1, double z1, double x2, double y2, double z2,
                                double x3, double y3, double z3, double x4, double y4, double z4,
                                float normalX, float normalY, float normalZ,
                                float red, float green, float blue, float alpha) {
        
        // Draw exactly 4 vertices for a proper quad
        vertexConsumer.vertex(positionMatrix, (float)x1, (float)y1, (float)z1).color(red, green, blue, alpha);
        vertexConsumer.vertex(positionMatrix, (float)x2, (float)y2, (float)z2).color(red, green, blue, alpha);
        vertexConsumer.vertex(positionMatrix, (float)x3, (float)y3, (float)z3).color(red, green, blue, alpha);
        vertexConsumer.vertex(positionMatrix, (float)x4, (float)y4, (float)z4).color(red, green, blue, alpha);
    }
    
    /**
     * Convenience method to add a zone from center and size
     */
    public static void addZoneFromCenter(String id, Vec3d center, double xRadius, double yRadius, double zRadius, String label) {
        MinecraftVector3 min = rocamocha.mochamix.api.io.MinecraftView.of(
            new Vec3d(center.x - xRadius, center.y - yRadius, center.z - zRadius));
        MinecraftVector3 max = rocamocha.mochamix.api.io.MinecraftView.of(
            new Vec3d(center.x + xRadius, center.y + yRadius, center.z + zRadius));
        
        MinecraftBox box = new SimpleMinecraftBox(min, max);
        addZone(id, box, label);
    }
    
    /**
     * Simple MinecraftBox implementation for our debug renderer
     */
    private static class SimpleMinecraftBox implements MinecraftBox {
        private final MinecraftVector3 min, max;
        
        public SimpleMinecraftBox(MinecraftVector3 min, MinecraftVector3 max) {
            this.min = min;
            this.max = max;
        }
        
        @Override public MinecraftVector3 min() { return min; }
        @Override public MinecraftVector3 max() { return max; }
        @Override public MinecraftVector3 center() {
            Vec3d minVec = min.asNativeVec3d();
            Vec3d maxVec = max.asNativeVec3d();
            return rocamocha.mochamix.api.io.MinecraftView.of(
                new Vec3d((minVec.x + maxVec.x) / 2.0, (minVec.y + maxVec.y) / 2.0, (minVec.z + maxVec.z) / 2.0)
            );
        }
        @Override public MinecraftVector3 size() {
            Vec3d minVec = min.asNativeVec3d();
            Vec3d maxVec = max.asNativeVec3d();
            return rocamocha.mochamix.api.io.MinecraftView.of(
                new Vec3d(maxVec.x - minVec.x, maxVec.y - minVec.y, maxVec.z - minVec.z)
            );
        }
        @Override public int width() { 
            Vec3d minVec = min.asNativeVec3d();
            Vec3d maxVec = max.asNativeVec3d();
            return (int)(maxVec.x - minVec.x); 
        }
        @Override public int height() { 
            Vec3d minVec = min.asNativeVec3d();
            Vec3d maxVec = max.asNativeVec3d();
            return (int)(maxVec.y - minVec.y); 
        }
        @Override public int depth() { 
            Vec3d minVec = min.asNativeVec3d();
            Vec3d maxVec = max.asNativeVec3d();
            return (int)(maxVec.z - minVec.z); 
        }
        
        @Override public Object asNative() { return this; }
    }
}