package rocamocha.mochamix.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/**
 * Client-side zone renderer that integrates with Fabric's world render events.
 * This provides safe, event-based rendering without complex mixin injections.
 */
@Environment(EnvType.CLIENT)
public class ClientZoneRenderer {

    /**
     * Initialize the client-side zone rendering system.
     * Registers with Fabric's WorldRenderEvents for automatic rendering.
     */
    public static void initialize() {
        try {
            // Register for the AFTER_TRANSLUCENT render phase
            // This ensures zones render after most world geometry but before UI
            WorldRenderEvents.AFTER_TRANSLUCENT.register((wrc) -> {
                try {
                    renderZones(wrc);
                } catch (Exception e) {
                    // Log error but don't crash the game
                    System.err.println("MochaMix: Error in zone rendering: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            System.out.println("MochaMix: ClientZoneRenderer initialized successfully");
        } catch (Exception e) {
            System.err.println("MochaMix: Failed to initialize ClientZoneRenderer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Renders all active zones using the world render context.
     * Called automatically during each frame by Fabric's event system.
     * 
     * @param wrc The world render context providing matrices and vertex consumers
     */
    private static void renderZones(WorldRenderContext wrc) {
        // Only render if zones are enabled
        if (!DebugRenderManager.isZoneRenderingEnabled()) {
            return;
        }

        // Null checks for safety
        if (wrc == null || wrc.camera() == null || wrc.matrixStack() == null || wrc.consumers() == null) {
            return;
        }

        try {
            // Get camera position for world-space rendering
            Vec3d cameraPos = wrc.camera().getPos();
            
            // Get the matrix stack and vertex consumers from the context
            MatrixStack matrixStack = wrc.matrixStack();
            
            // Use the vertex consumers provided by the world render context
            // This ensures proper integration with Minecraft's rendering pipeline
            DebugRenderManager.renderDebugElements(
                matrixStack, 
                wrc.consumers(), 
                cameraPos.x, 
                cameraPos.y, 
                cameraPos.z
            );
        } catch (Exception e) {
            // Don't crash the game, just log the error
            System.err.println("MochaMix: Error rendering zones: " + e.getMessage());
            // Disable rendering to prevent further errors this session
            DebugRenderManager.disableZoneRendering();
        }
    }

    /**
     * Cleanup method for when the renderer is no longer needed.
     * Currently no cleanup is required since Fabric manages event lifecycle.
     */
    public static void cleanup() {
        // Fabric automatically handles event cleanup
        // No manual cleanup required
    }
}