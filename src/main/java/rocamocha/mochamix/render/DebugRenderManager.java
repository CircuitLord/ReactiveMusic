package rocamocha.mochamix.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Manager class for integrating zone debug rendering into Minecraft's render pipeline.
 * Handles the lifecycle and registration of debug renderers.
 */
public class DebugRenderManager {
    
    private static boolean initialized = false;
    
    /**
     * Initialize the debug render system
     * Should be called during mod initialization
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        
        // Debug renderer is ready to be hooked into the render pipeline
        // The actual integration will happen via mixin or render event
    }
    
    /**
     * Main render call that should be invoked during world rendering
     * This will be called from our WorldRenderer mixin
     */
    public static void renderDebugElements(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        
        // Always render zone debug overlays when enabled (user controls via commands)
        // We don't tie this to F3 debug mode since zone rendering is independent
        ZoneDebugRenderer.render(matrices, vertexConsumers, cameraX, cameraY, cameraZ);
    }
    
    /**
     * Toggle zone debug rendering on/off
     */
    public static void toggleZoneRendering() {
        ZoneDebugRenderer.setEnabled(!ZoneDebugRenderer.isEnabled());
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            String status = ZoneDebugRenderer.isEnabled() ? "enabled" : "disabled";
            mc.player.sendMessage(net.minecraft.text.Text.literal("Zone debug rendering " + status), false);
        }
    }
    
    /**
     * Enable zone debug rendering
     */
    public static void enableZoneRendering() {
        ZoneDebugRenderer.setEnabled(true);
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("Zone debug rendering enabled"), false);
        }
    }
    
    /**
     * Disable zone debug rendering
     */
    public static void disableZoneRendering() {
        ZoneDebugRenderer.setEnabled(false);
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("Zone debug rendering disabled"), false);
        }
    }
    
    /**
     * Check if zone rendering is enabled
     */
    public static boolean isZoneRenderingEnabled() {
        return ZoneDebugRenderer.isEnabled();
    }
    
    /**
     * Clear all debug zones
     */
    public static void clearAllZones() {
        ZoneDebugRenderer.clearZones();
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("All debug zones cleared"), false);
        }
    }
}