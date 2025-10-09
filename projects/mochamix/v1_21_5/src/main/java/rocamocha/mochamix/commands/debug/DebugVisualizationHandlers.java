package rocamocha.mochamix.commands.debug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import rocamocha.mochamix.api.io.MinecraftView;
import rocamocha.mochamix.api.minecraft.util.MinecraftBox;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.render.ZoneDebugRenderer;
import rocamocha.mochamix.render.DebugRenderManager;

import java.util.Random;

/**
 * Command handlers for debug visualization of zones and boxes
 */
public class DebugVisualizationHandlers {
    
    private static final Random colorRandom = new Random();
    
    /**
     * Generate a random color for zone visualization.
     * Uses vibrant, distinct colors that are easy to see in-game.
     * @return array of [red, green, blue, alpha] values from 0.0 to 1.0
     */
    private static float[] generateRandomColor() {
        // Predefined set of vibrant, distinct colors that work well in Minecraft
        float[][] colors = {
            {1.0f, 0.2f, 0.2f, 0.8f}, // Bright Red
            {0.2f, 1.0f, 0.2f, 0.8f}, // Bright Green  
            {0.2f, 0.2f, 1.0f, 0.8f}, // Bright Blue
            {1.0f, 1.0f, 0.2f, 0.8f}, // Bright Yellow
            {1.0f, 0.2f, 1.0f, 0.8f}, // Bright Magenta
            {0.2f, 1.0f, 1.0f, 0.8f}, // Bright Cyan
            {1.0f, 0.5f, 0.0f, 0.8f}, // Bright Orange
            {0.5f, 0.0f, 1.0f, 0.8f}, // Purple
            {0.0f, 1.0f, 0.5f, 0.8f}, // Spring Green
            {1.0f, 0.0f, 0.5f, 0.8f}, // Hot Pink
            {0.5f, 1.0f, 0.0f, 0.8f}, // Lime
            {0.0f, 0.5f, 1.0f, 0.8f}  // Sky Blue
        };
        
        return colors[colorRandom.nextInt(colors.length)];
    }
    
    /**
     * Helper method to get Vec3d from client command context
     * Since we're on the client side, we'll parse the argument manually
     */
    private static Vec3d getVec3FromClient(CommandContext<FabricClientCommandSource> ctx, String argName) throws CommandSyntaxException {
        // In MC 1.21.5, we need to use Vec3ArgumentType directly instead of PosArgument
        // Get the Vec3d argument directly
        return ctx.getArgument(argName, Vec3d.class);
    }
    
    /**
     * Visualizes a box from center position and xyz radii
     * Usage: /mochamix api debug box_center <center> <x_radius> <y_radius> <z_radius>
     * Supports relative coordinates: ~ ~1 ~ 5 3 5 (works in singleplayer only)
     */
    public static int visualizeBoxFromCenter(CommandContext<FabricClientCommandSource> ctx) {
        try {
            // Get the center position (supports relative coordinates like ~ ~ ~)
            // This works by accessing the integrated server context
            Vec3d center = getVec3FromClient(ctx, "center");
            
            // Get the radii
            double xRadius = ctx.getArgument("x_radius", Double.class);
            double yRadius = ctx.getArgument("y_radius", Double.class);
            double zRadius = ctx.getArgument("z_radius", Double.class);
            
            // Create min/max positions from center and radii
            Vec3d min = new Vec3d(center.x - xRadius, center.y - yRadius, center.z - zRadius);
            Vec3d max = new Vec3d(center.x + xRadius, center.y + yRadius, center.z + zRadius);
            
            // Create MinecraftBox using your API with safety checks
            if (min == null || max == null) {
                ctx.getSource().sendError(Text.literal("Invalid coordinates provided"));
                return 0;
            }
            
            MinecraftVector3 minVec = MinecraftView.of(min);
            MinecraftVector3 maxVec = MinecraftView.of(max);
            
            if (minVec == null || maxVec == null) {
                ctx.getSource().sendError(Text.literal("Failed to create coordinate vectors"));
                return 0;
            }
            
            MinecraftBox box = new SimpleMinecraftBox(minVec, maxVec);
            
            // Generate random color for visual distinction
            float[] color = generateRandomColor();
            
            // Add zone to debug renderer for persistent visualization
            String zoneId = "box_center_" + System.currentTimeMillis();
            ZoneDebugRenderer.addZone(zoneId, box, color[0], color[1], color[2], color[3], "Center Box");
            
            // Enable rendering if not already enabled
            if (!ZoneDebugRenderer.isEnabled()) {
                DebugRenderManager.enableZoneRendering();
            }
            
            // Send feedback
            ctx.getSource().sendFeedback(Text.literal("✓ Visualizing colored box at ")
                .append(Text.literal(String.format("%.1f, %.1f, %.1f", center.x, center.y, center.z)).formatted(Formatting.AQUA))
                .append(Text.literal(" with size "))
                .append(Text.literal(String.format("%.1f×%.1f×%.1f", xRadius*2, yRadius*2, zRadius*2)).formatted(Formatting.GREEN)));
            
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Failed to visualize box: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Visualizes a box from min and max positions
     * Usage: /mochamix api debug box_minmax <min> <max>
     * Supports relative coordinates: ~-5 ~ ~-5 ~5 ~10 ~5 (works in singleplayer only)
     */
    public static int visualizeBoxFromMinMax(CommandContext<FabricClientCommandSource> ctx) {
        try {
            // Get the positions (supports relative coordinates)
            Vec3d min = getVec3FromClient(ctx, "min");
            Vec3d max = getVec3FromClient(ctx, "max");
            
            // Ensure min is actually smaller than max
            Vec3d actualMin = new Vec3d(
                Math.min(min.x, max.x),
                Math.min(min.y, max.y), 
                Math.min(min.z, max.z)
            );
            Vec3d actualMax = new Vec3d(
                Math.max(min.x, max.x),
                Math.max(min.y, max.y),
                Math.max(min.z, max.z)
            );
            
            // Create MinecraftBox using your API
            MinecraftVector3 minVec = MinecraftView.of(actualMin);
            MinecraftVector3 maxVec = MinecraftView.of(actualMax);
            MinecraftBox box = new SimpleMinecraftBox(minVec, maxVec);
            
            // Generate random color for visual distinction
            float[] color = generateRandomColor();
            
            // Add zone to debug renderer for persistent visualization
            String zoneId = "box_minmax_" + System.currentTimeMillis();
            ZoneDebugRenderer.addZone(zoneId, box, color[0], color[1], color[2], color[3], "MinMax Box");
            
            // Enable rendering if not already enabled
            if (!ZoneDebugRenderer.isEnabled()) {
                DebugRenderManager.enableZoneRendering();
            }
            
            // Calculate dimensions
            Vec3d size = actualMax.subtract(actualMin);
            
            // Send feedback
            ctx.getSource().sendFeedback(Text.literal("✓ Visualizing colored box from ")
                .append(Text.literal(String.format("%.1f,%.1f,%.1f", actualMin.x, actualMin.y, actualMin.z)).formatted(Formatting.AQUA))
                .append(Text.literal(" to "))
                .append(Text.literal(String.format("%.1f,%.1f,%.1f", actualMax.x, actualMax.y, actualMax.z)).formatted(Formatting.AQUA))
                .append(Text.literal(" (size: "))
                .append(Text.literal(String.format("%.1f×%.1f×%.1f", size.x, size.y, size.z)).formatted(Formatting.GREEN))
                .append(Text.literal(")")));
            
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Failed to visualize box: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Visualizes a box centered at the player's current position
     * Usage: /mochamix api debug box_here <x_radius> <y_radius> <z_radius>
     * Example: /mochamix api debug box_here 5 3 5
     */
    public static int visualizeBoxAtPlayerPosition(CommandContext<FabricClientCommandSource> ctx) {
        try {
            // Get the player's current position
            Vec3d center = ctx.getSource().getPosition();
            
            // Get the radii
            double xRadius = ctx.getArgument("x_radius", Double.class);
            double yRadius = ctx.getArgument("y_radius", Double.class);
            double zRadius = ctx.getArgument("z_radius", Double.class);
            
            // Create min/max positions from center and radii
            Vec3d min = new Vec3d(center.x - xRadius, center.y - yRadius, center.z - zRadius);
            Vec3d max = new Vec3d(center.x + xRadius, center.y + yRadius, center.z + zRadius);
            
            // Create MinecraftBox using your API
            MinecraftVector3 minVec = MinecraftView.of(min);
            MinecraftVector3 maxVec = MinecraftView.of(max);
            MinecraftBox box = new SimpleMinecraftBox(minVec, maxVec);
            
            // Generate random color for visual distinction
            float[] color = generateRandomColor();
            
            // Add zone to debug renderer for persistent visualization
            String zoneId = "box_here_" + System.currentTimeMillis();
            ZoneDebugRenderer.addZone(zoneId, box, color[0], color[1], color[2], color[3], "Player Box");
            
            // Enable rendering if not already enabled
            if (!ZoneDebugRenderer.isEnabled()) {
                DebugRenderManager.enableZoneRendering();
            }
            
            // Send feedback
            ctx.getSource().sendFeedback(Text.literal("✓ Visualizing colored box at your position ")
                .append(Text.literal(String.format("%.1f, %.1f, %.1f", center.x, center.y, center.z)).formatted(Formatting.AQUA))
                .append(Text.literal(" with size "))
                .append(Text.literal(String.format("%.1f×%.1f×%.1f", xRadius*2, yRadius*2, zRadius*2)).formatted(Formatting.GREEN)));
            
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Failed to visualize box: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Clear all debug zones
     * Usage: /mochamix api debug config clear
     */
    public static int clearAllZones(CommandContext<FabricClientCommandSource> ctx) {
        try {
            DebugRenderManager.clearAllZones();
            
            ctx.getSource().sendFeedback(Text.literal("✓ All debug zones cleared").formatted(Formatting.GREEN));
            
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Failed to clear zones: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Enable zone debug rendering
     * Usage: /mochamix api debug config enable
     */
    public static int enableZoneRendering(CommandContext<FabricClientCommandSource> ctx) {
        try {
            DebugRenderManager.enableZoneRendering();
            
            ctx.getSource().sendFeedback(Text.literal("✓ Zone debug rendering ")
                .append(Text.literal("enabled").formatted(Formatting.GREEN))
                .append(Text.literal(" (persistent wireframes)")));
            
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Failed to enable rendering: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Disable zone debug rendering
     * Usage: /mochamix api debug config disable
     */
    public static int disableZoneRendering(CommandContext<FabricClientCommandSource> ctx) {
        try {
            DebugRenderManager.disableZoneRendering();
            
            ctx.getSource().sendFeedback(Text.literal("✓ Zone debug rendering ")
                .append(Text.literal("disabled").formatted(Formatting.RED))
                .append(Text.literal(" (zones remain but invisible)")));
            
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Failed to disable rendering: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Toggle zone debug rendering on/off
     * Usage: /mochamix api debug config toggle
     */
    public static int toggleZoneRendering(CommandContext<FabricClientCommandSource> ctx) {
        try {
            DebugRenderManager.toggleZoneRendering();
            
            String status = DebugRenderManager.isZoneRenderingEnabled() ? "enabled" : "disabled";
            Formatting color = DebugRenderManager.isZoneRenderingEnabled() ? Formatting.GREEN : Formatting.RED;
            
            ctx.getSource().sendFeedback(Text.literal("✓ Zone debug rendering ")
                .append(Text.literal(status).formatted(color)));
            
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Failed to toggle rendering: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Simple implementation of MinecraftBox for the visualization commands
     */
    private static class SimpleMinecraftBox implements MinecraftBox {
        private final MinecraftVector3 min;
        private final MinecraftVector3 max;
        private final MinecraftVector3 center;
        private final MinecraftVector3 size;
        
        public SimpleMinecraftBox(MinecraftVector3 min, MinecraftVector3 max) {
            this.min = min;
            this.max = max;
            
            // Calculate center
            Vec3d minVec = min.asNativeVec3d();
            Vec3d maxVec = max.asNativeVec3d();
            Vec3d centerVec = new Vec3d(
                (minVec.x + maxVec.x) / 2,
                (minVec.y + maxVec.y) / 2,
                (minVec.z + maxVec.z) / 2
            );
            this.center = MinecraftView.of(centerVec);
            
            // Calculate size
            Vec3d sizeVec = maxVec.subtract(minVec);
            this.size = MinecraftView.of(sizeVec);
        }
        
        @Override public MinecraftVector3 min() { return min; }
        @Override public MinecraftVector3 max() { return max; }
        @Override public MinecraftVector3 center() { return center; }
        @Override public MinecraftVector3 size() { return size; }
        
        @Override public int width() { return (int) Math.ceil(size.asNativeVec3d().x); }
        @Override public int height() { return (int) Math.ceil(size.asNativeVec3d().y); }
        @Override public int depth() { return (int) Math.ceil(size.asNativeVec3d().z); }
        
        @Override public Object asNative() { return null; } // Not needed for visualization
    }
}