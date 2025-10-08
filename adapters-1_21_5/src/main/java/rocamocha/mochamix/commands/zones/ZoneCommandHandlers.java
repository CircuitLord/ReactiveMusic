package rocamocha.mochamix.commands.zones;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import circuitlord.reactivemusic.ReactiveMusicDebug.TextBuilder;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.api.io.MinecraftView;
import rocamocha.mochamix.zones.ZoneData;
import rocamocha.mochamix.zones.ZoneUtils;

import java.util.List;
import java.util.Optional;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * Command handlers for zone management operations.
 * Provides commands to create, list, and delete zones with proper feedback using TextBuilder.
 */
public class ZoneCommandHandlers {
    
    /**
     * Schedule zone rendering to be disabled after 5 seconds (100 ticks)
     */
    private static void scheduleRenderingDisable() {
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                    rocamocha.mochamix.render.ZoneDebugRenderer.setEnabled(false);
                });
            }
        }, 5000); // 5 seconds
    }

    /**
     * Helper method to get Vec3d from client command context by accessing integrated server
     * This allows us to use relative coordinates (~ ~ ~) in singleplayer
     */
    private static Vec3d getVec3FromClient(CommandContext<FabricClientCommandSource> ctx, String argName) throws CommandSyntaxException {
        FabricClientCommandSource clientSource = ctx.getSource();
        
        // Try to get the integrated server source
        var client = clientSource.getClient();
        if (client.getServer() != null && client.player != null) {
            // We have an integrated server (singleplayer)
            // Get the server world that corresponds to the client world
            var serverWorld = client.getServer().getWorld(client.player.getWorld().getRegistryKey());
            if (serverWorld != null) {
                var serverSource = client.getServer().getCommandSource()
                    .withEntity(client.player)
                    .withPosition(client.player.getPos())
                    .withRotation(client.player.getRotationClient())
                    .withWorld(serverWorld);
                
                // Get the position argument and resolve it using server context
                var posArg = ctx.getArgument(argName, net.minecraft.command.argument.PosArgument.class);
                return posArg.getPos(serverSource);
            }
        }
        
        // Fallback for multiplayer or when integrated server isn't available
        // In this case, we'll just use the player's current position
        ctx.getSource().sendError(Text.literal("⚠ Relative coordinates only work in singleplayer. Using your current position instead."));
        return clientSource.getPosition();
    }

    /**
     * Create a zone from center position and radii.
     * Usage: /mochamix zones create_center <name> <center> <x_radius> <y_radius> <z_radius>
     */
    public static int createZoneFromCenter(CommandContext<FabricClientCommandSource> ctx) {
        try {
            String zoneName = StringArgumentType.getString(ctx, "name");
            Vec3d centerVec = getVec3FromClient(ctx, "center");
            double xRadius = DoubleArgumentType.getDouble(ctx, "x_radius");
            double yRadius = DoubleArgumentType.getDouble(ctx, "y_radius");
            double zRadius = DoubleArgumentType.getDouble(ctx, "z_radius");
            
            // Convert to MinecraftVector3
            MinecraftVector3 center = MinecraftView.of(centerVec);
            
            // Create the zone
            String zoneId = ZoneUtils.createZoneFromCenter(zoneName, center, xRadius, yRadius, zRadius);
            
            // Auto-enable rendering if not already enabled
            boolean wasRenderingEnabled = rocamocha.mochamix.render.ZoneDebugRenderer.isEnabled();
            if (!wasRenderingEnabled) {
                rocamocha.mochamix.render.ZoneDebugRenderer.setEnabled(true);
                rocamocha.mochamix.render.ZoneDebugRenderer.syncWithPersistedZones();
            }
            
            // Success feedback
            TextBuilder response = new TextBuilder();
            response.line("Zone Created Successfully!", Formatting.GREEN, Formatting.BOLD);
            response.line("Name: " + zoneName, Formatting.AQUA);
            response.line("ID: " + zoneId.substring(0, 8) + "...", Formatting.GRAY);
            response.line("Center: " + String.format("(%.1f, %.1f, %.1f)", centerVec.x, centerVec.y, centerVec.z), Formatting.YELLOW);
            response.line("Radii: " + String.format("%.1f x %.1f x %.1f", xRadius, yRadius, zRadius), Formatting.YELLOW);
            response.line("Total zones: " + ZoneUtils.getZoneCount(), Formatting.WHITE);
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to create zone: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }

    /**
     * Creates a new zone centered at the player's current position.
     * Usage: /mochamix zones create here <name> <x_radius> <y_radius> <z_radius>
     */
    public static int createZoneFromPlayerCenter(CommandContext<FabricClientCommandSource> ctx) {
        try {
            String zoneName = StringArgumentType.getString(ctx, "name");
            double xRadius = DoubleArgumentType.getDouble(ctx, "x_radius");
            double yRadius = DoubleArgumentType.getDouble(ctx, "y_radius");
            double zRadius = DoubleArgumentType.getDouble(ctx, "z_radius");
            
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) {
                TextBuilder error = new TextBuilder();
                error.line("No player found", Formatting.RED);
                ctx.getSource().sendFeedback(error.build());
                return 0;
            }
            
            Vec3d playerPos = player.getPos();
            
            // Convert to MinecraftVector3
            MinecraftVector3 center = MinecraftView.of(playerPos).asBlockPos();
            
            // Create the zone
            String zoneId = ZoneUtils.createZoneFromCenter(zoneName, center, xRadius, yRadius, zRadius);
            
            // Auto-enable rendering if not already enabled
            boolean wasRenderingEnabled = rocamocha.mochamix.render.ZoneDebugRenderer.isEnabled();
            if (!wasRenderingEnabled) {
                rocamocha.mochamix.render.ZoneDebugRenderer.setEnabled(true);
                rocamocha.mochamix.render.ZoneDebugRenderer.syncWithPersistedZones();
                
                // Schedule to turn rendering back off after 5 seconds
                scheduleRenderingDisable();
            }
            
            // Success feedback
            TextBuilder response = new TextBuilder();
            response.line("Zone Created at Your Position!", Formatting.GREEN, Formatting.BOLD);
            response.line("Name: " + zoneName, Formatting.AQUA);
            response.line("ID: " + zoneId.substring(0, 8) + "...", Formatting.GRAY);
            response.line("Center: " + String.format("(%.1f, %.1f, %.1f)", playerPos.x, playerPos.y, playerPos.z), Formatting.YELLOW);
            response.line("Radii: " + String.format("%.1f x %.1f x %.1f", xRadius, yRadius, zRadius), Formatting.YELLOW);
            response.line("Total zones: " + ZoneUtils.getZoneCount(), Formatting.WHITE);
            
            if (!wasRenderingEnabled) {
                response.line("Zone rendering auto-enabled!", Formatting.GREEN);
            }
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to create zone: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }

    /**
     * Create a zone from two corner positions.
     * Usage: /mochamix zones create_corners <name> <corner1> <corner2>
     */
    public static int createZoneFromCorners(CommandContext<FabricClientCommandSource> ctx) {
        try {
            String zoneName = StringArgumentType.getString(ctx, "name");
            Vec3d corner1Vec = getVec3FromClient(ctx, "corner1");
            Vec3d corner2Vec = getVec3FromClient(ctx, "corner2");
            
            // Convert to MinecraftVector3
            MinecraftVector3 corner1 = MinecraftView.of(corner1Vec);
            MinecraftVector3 corner2 = MinecraftView.of(corner2Vec);
            
            // Create the zone
            String zoneId = ZoneUtils.createZoneFromCorners(zoneName, corner1, corner2);
            
            // Auto-enable rendering if not already enabled
            boolean wasRenderingEnabled = rocamocha.mochamix.render.ZoneDebugRenderer.isEnabled();
            if (!wasRenderingEnabled) {
                rocamocha.mochamix.render.ZoneDebugRenderer.setEnabled(true);
                rocamocha.mochamix.render.ZoneDebugRenderer.syncWithPersistedZones();
                
                // Schedule to turn rendering back off after 5 seconds
                scheduleRenderingDisable();
            }
            
            // Calculate dimensions for feedback
            double width = Math.abs(corner2Vec.x - corner1Vec.x);
            double height = Math.abs(corner2Vec.y - corner1Vec.y);
            double depth = Math.abs(corner2Vec.z - corner1Vec.z);
            
            // Success feedback
            TextBuilder response = new TextBuilder();
            response.line("Zone Created Successfully!", Formatting.GREEN, Formatting.BOLD);
            response.line("Name: " + zoneName, Formatting.AQUA);
            response.line("ID: " + zoneId.substring(0, 8) + "...", Formatting.GRAY);
            response.line("Corner 1: " + String.format("(%.1f, %.1f, %.1f)", corner1Vec.x, corner1Vec.y, corner1Vec.z), Formatting.YELLOW);
            response.line("Corner 2: " + String.format("(%.1f, %.1f, %.1f)", corner2Vec.x, corner2Vec.y, corner2Vec.z), Formatting.YELLOW);
            response.line("Dimensions: " + String.format("%.1f x %.1f x %.1f", width, height, depth), Formatting.YELLOW);
            response.line("Total zones: " + ZoneUtils.getZoneCount(), Formatting.WHITE);
            
            if (!wasRenderingEnabled) {
                response.line("Zone rendering auto-enabled!", Formatting.GREEN);
            }
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to create zone: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }

    /**
     * Create a zone at the player's current position with specified radii.
     * Usage: /mochamix zones create_here <name> <x_radius> <y_radius> <z_radius>
     */
    public static int createZoneAtPlayer(CommandContext<FabricClientCommandSource> ctx) {
        try {
            MinecraftClient client = ctx.getSource().getClient();
            if (client.player == null) {
                ctx.getSource().sendFeedback(Text.literal("No player found").formatted(Formatting.RED));
                return 0;
            }
            
            String zoneName = StringArgumentType.getString(ctx, "name");
            double xRadius = DoubleArgumentType.getDouble(ctx, "x_radius");
            double yRadius = DoubleArgumentType.getDouble(ctx, "y_radius");
            double zRadius = DoubleArgumentType.getDouble(ctx, "z_radius");
            
            // Get player position
            Vec3d playerPos = client.player.getPos();
            MinecraftVector3 center = MinecraftView.of(playerPos).asBlockPos();
            
            // Create the zone
            String zoneId = ZoneUtils.createZoneFromCenter(zoneName, center, xRadius, yRadius, zRadius);
            
            // Auto-enable rendering if not already enabled
            boolean wasRenderingEnabled = rocamocha.mochamix.render.ZoneDebugRenderer.isEnabled();
            if (!wasRenderingEnabled) {
                rocamocha.mochamix.render.ZoneDebugRenderer.setEnabled(true);
                rocamocha.mochamix.render.ZoneDebugRenderer.syncWithPersistedZones();
                
                // Schedule to turn rendering back off after 5 seconds
                scheduleRenderingDisable();
            }
            
            // Success feedback
            TextBuilder response = new TextBuilder();
            response.line("Zone Created at Your Location!", Formatting.GREEN, Formatting.BOLD);
            response.line("Name: " + zoneName, Formatting.AQUA);
            response.line("ID: " + zoneId.substring(0, 8) + "...", Formatting.GRAY);
            response.line("Center: " + String.format("(%.1f, %.1f, %.1f)", playerPos.x, playerPos.y, playerPos.z), Formatting.YELLOW);
            response.line("Radii: " + String.format("%.1f x %.1f x %.1f", xRadius, yRadius, zRadius), Formatting.YELLOW);
            response.line("Total zones: " + ZoneUtils.getZoneCount(), Formatting.WHITE);
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to create zone: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }

    /**
     * List all zones with detailed information and indices for deletion.
     * Usage: /mochamix zones list
     */
    public static int listZones(CommandContext<FabricClientCommandSource> ctx) {
        try {
            List<ZoneData> zones = ZoneUtils.getAllZones();
            
            if (zones.isEmpty()) {
                TextBuilder response = new TextBuilder();
                response.line("No zones found", Formatting.YELLOW);
                response.line("Use '/mochamix zones create_here <name> <radius> <radius> <radius>' to create one", Formatting.GRAY);
                ctx.getSource().sendFeedback(response.build());
                return 1;
            }
            
            TextBuilder response = new TextBuilder();
            response.line("=== Zone List ===", Formatting.GOLD, Formatting.BOLD);
            response.line("Total zones: " + zones.size(), Formatting.WHITE);
            response.newline();
            
            for (int i = 0; i < zones.size(); i++) {
                ZoneData zone = zones.get(i);
                
                // Index and name
                response.raw("[" + (i + 1) + "] ", Formatting.LIGHT_PURPLE, Formatting.BOLD);
                response.raw(zone.getZoneName(), Formatting.AQUA, Formatting.BOLD);
                response.newline();
                
                // ID (shortened)
                response.raw("    ID: ", Formatting.GRAY);
                response.raw(zone.getUniqueId().substring(0, 8) + "...", Formatting.LIGHT_PURPLE);
                response.newline();
                
                // Coordinates
                response.raw("    Min: ", Formatting.GRAY);
                response.raw(String.format("(%.1f, %.1f, %.1f)", 
                    zone.minX(), zone.minY(), zone.minZ()), Formatting.YELLOW);
                response.newline();
                
                response.raw("    Max: ", Formatting.GRAY);
                response.raw(String.format("(%.1f, %.1f, %.1f)", 
                    zone.maxX(), zone.maxY(), zone.maxZ()), Formatting.YELLOW);
                response.newline();
                
                // Dimensions
                double width = zone.maxX() - zone.minX();
                double height = zone.maxY() - zone.minY();
                double depth = zone.maxZ() - zone.minZ();
                response.raw("    Size: ", Formatting.GRAY);
                response.raw(String.format("%.1f x %.1f x %.1f", width, height, depth), Formatting.GREEN);
                response.newline();
                
                // Volume
                double volume = width * height * depth;
                response.raw("    Volume: ", Formatting.GRAY);
                response.raw(String.format("%.0f blocks³", volume), Formatting.GREEN);
                response.newline();
                
                // Timestamps
                response.raw("    Created: ", Formatting.GRAY);
                response.raw(new java.util.Date(zone.getCreatedTimestamp()).toString(), Formatting.WHITE);
                response.newline();
                
                if (i < zones.size() - 1) {
                    response.newline();
                }
            }
            
            response.newline();
            response.line("Use '/mochamix zones delete <index>' to delete a zone", Formatting.GRAY);
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to list zones: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }

    /**
     * Delete a zone by its index in the list (1-based indexing).
     * Usage: /mochamix zones delete <index>
     */
    public static int deleteZoneByIndex(CommandContext<FabricClientCommandSource> ctx) {
        try {
            int index = IntegerArgumentType.getInteger(ctx, "index");
            List<ZoneData> zones = ZoneUtils.getAllZones();
            
            if (zones.isEmpty()) {
                TextBuilder response = new TextBuilder();
                response.line("No zones to delete", Formatting.YELLOW);
                ctx.getSource().sendFeedback(response.build());
                return 1;
            }
            
            // Convert to 0-based indexing
            int zeroBasedIndex = index - 1;
            
            if (zeroBasedIndex < 0 || zeroBasedIndex >= zones.size()) {
                TextBuilder error = new TextBuilder();
                error.line("Invalid index: " + index, Formatting.RED);
                error.line("Valid range: 1 to " + zones.size(), Formatting.YELLOW);
                error.line("Use '/mochamix zones list' to see all zones", Formatting.GRAY);
                ctx.getSource().sendFeedback(error.build());
                return 0;
            }
            
            ZoneData zoneToDelete = zones.get(zeroBasedIndex);
            boolean deleted = ZoneUtils.deleteZone(zoneToDelete.getUniqueId());
            
            if (deleted) {
                TextBuilder response = new TextBuilder();
                response.line("Zone Deleted Successfully!", Formatting.GREEN, Formatting.BOLD);
                response.line("Name: " + zoneToDelete.getZoneName(), Formatting.AQUA);
                response.line("ID: " + zoneToDelete.getUniqueId().substring(0, 8) + "...", Formatting.GRAY);
                response.line("Remaining zones: " + (ZoneUtils.getZoneCount()), Formatting.WHITE);
                
                ctx.getSource().sendFeedback(response.build());
                return 1;
            } else {
                TextBuilder error = new TextBuilder();
                error.line("Failed to delete zone", Formatting.RED);
                ctx.getSource().sendFeedback(error.build());
                return 0;
            }
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to delete zone: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }

    /**
     * Get information about a specific zone by index.
     * Usage: /mochamix zones info <index>
     */
    public static int getZoneInfo(CommandContext<FabricClientCommandSource> ctx) {
        try {
            int index = IntegerArgumentType.getInteger(ctx, "index");
            List<ZoneData> zones = ZoneUtils.getAllZones();
            
            if (zones.isEmpty()) {
                TextBuilder response = new TextBuilder();
                response.line("No zones found", Formatting.YELLOW);
                ctx.getSource().sendFeedback(response.build());
                return 1;
            }
            
            // Convert to 0-based indexing
            int zeroBasedIndex = index - 1;
            
            if (zeroBasedIndex < 0 || zeroBasedIndex >= zones.size()) {
                TextBuilder error = new TextBuilder();
                error.line("Invalid index: " + index, Formatting.RED);
                error.line("Valid range: 1 to " + zones.size(), Formatting.YELLOW);
                ctx.getSource().sendFeedback(error.build());
                return 0;
            }
            
            ZoneData zone = zones.get(zeroBasedIndex);
            
            // Get additional calculated info
            Optional<MinecraftVector3> center = ZoneUtils.getZoneCenter(zone.getUniqueId());
            Optional<Double> volume = ZoneUtils.getZoneVolume(zone.getUniqueId());
            List<ZoneData> overlapping = ZoneUtils.getOverlappingZones(zone.getUniqueId());
            
            TextBuilder response = new TextBuilder();
            response.line("=== Zone Information ===", Formatting.GOLD, Formatting.BOLD);
            response.line("Index: [" + index + "]", Formatting.WHITE);
            response.line("Name: " + zone.getZoneName(), Formatting.AQUA, Formatting.BOLD);
            response.line("Full ID: " + zone.getUniqueId(), Formatting.GRAY);
            response.newline();
            
            // Coordinates
            response.line("Coordinates:", Formatting.WHITE, Formatting.BOLD);
            response.line("  Min: " + String.format("(%.1f, %.1f, %.1f)", 
                zone.minX(), zone.minY(), zone.minZ()), Formatting.YELLOW);
            response.line("  Max: " + String.format("(%.1f, %.1f, %.1f)", 
                zone.maxX(), zone.maxY(), zone.maxZ()), Formatting.YELLOW);
            
            if (center.isPresent()) {
                MinecraftVector3 c = center.get();
                response.line("  Center: " + String.format("(%.1f, %.1f, %.1f)", 
                    c.asNativeVec3d().x, c.asNativeVec3d().y, c.asNativeVec3d().z), Formatting.GREEN);
            }
            
            // Dimensions and volume
            double width = zone.maxX() - zone.minX();
            double height = zone.maxY() - zone.minY();
            double depth = zone.maxZ() - zone.minZ();
            response.line("Dimensions: " + String.format("%.1f x %.1f x %.1f", width, height, depth), Formatting.GREEN);
            
            if (volume.isPresent()) {
                response.line("Volume: " + String.format("%.0f blocks³", volume.get()), Formatting.GREEN);
            }
            
            // Timestamps
            response.line("Created: " + new java.util.Date(zone.getCreatedTimestamp()), Formatting.GRAY);
            response.line("Modified: " + new java.util.Date(zone.getModifiedTimestamp()), Formatting.GRAY);
            
            // Overlapping zones
            if (!overlapping.isEmpty()) {
                response.line("Overlapping with " + overlapping.size() + " other zones:", Formatting.YELLOW);
                for (ZoneData overlap : overlapping) {
                    response.line("  - " + overlap.getZoneName(), Formatting.GOLD);
                }
            } else {
                response.line("No overlapping zones", Formatting.GREEN);
            }
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to get zone info: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }

    /**
     * Clear all zones (with confirmation).
     * Usage: /mochamix zones clear
     */
    public static int clearAllZones(CommandContext<FabricClientCommandSource> ctx) {
        try {
            int zoneCount = ZoneUtils.getZoneCount();
            
            if (zoneCount == 0) {
                TextBuilder response = new TextBuilder();
                response.line("No zones to clear", Formatting.YELLOW);
                ctx.getSource().sendFeedback(response.build());
                return 1;
            }
            
            // Clear all zones
            ZoneUtils.getAllZones().forEach(zone -> ZoneUtils.deleteZone(zone.getUniqueId()));
            
            TextBuilder response = new TextBuilder();
            response.line("All Zones Cleared!", Formatting.RED, Formatting.BOLD);
            response.line("Deleted " + zoneCount + " zones", Formatting.WHITE);
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to clear zones: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }

    /**
     * Toggle zone rendering on/off.
     * Usage: /mochamix zones render toggle
     */
    public static int toggleZoneRendering(CommandContext<FabricClientCommandSource> ctx) {
        try {
            rocamocha.mochamix.render.ZoneDebugRenderer.setEnabled(
                !rocamocha.mochamix.render.ZoneDebugRenderer.isEnabled());
            
            boolean enabled = rocamocha.mochamix.render.ZoneDebugRenderer.isEnabled();
            
            TextBuilder response = new TextBuilder();
            if (enabled) {
                response.line("Zone Rendering Enabled!", Formatting.GREEN, Formatting.BOLD);
                response.line("Zones will now be visible as colored outlines", Formatting.WHITE);
                // Trigger immediate sync
                rocamocha.mochamix.render.ZoneDebugRenderer.syncWithPersistedZones();
            } else {
                response.line("Zone Rendering Disabled", Formatting.YELLOW, Formatting.BOLD);
                response.line("Zone outlines are no longer visible", Formatting.WHITE);
            }
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to toggle zone rendering: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }
    
    /**
     * Enable zone rendering.
     * Usage: /mochamix zones render on
     */
    public static int enableZoneRendering(CommandContext<FabricClientCommandSource> ctx) {
        try {
            rocamocha.mochamix.render.ZoneDebugRenderer.setEnabled(true);
            rocamocha.mochamix.render.ZoneDebugRenderer.syncWithPersistedZones();
            
            TextBuilder response = new TextBuilder();
            response.line("Zone Rendering Enabled!", Formatting.GREEN, Formatting.BOLD);
            response.line("All " + rocamocha.mochamix.zones.ZoneUtils.getZoneCount() + " zones are now visible", Formatting.WHITE);
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to enable zone rendering: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }
    
    /**
     * Disable zone rendering.
     * Usage: /mochamix zones render off
     */
    public static int disableZoneRendering(CommandContext<FabricClientCommandSource> ctx) {
        try {
            rocamocha.mochamix.render.ZoneDebugRenderer.setEnabled(false);
            
            TextBuilder response = new TextBuilder();
            response.line("Zone Rendering Disabled", Formatting.YELLOW, Formatting.BOLD);
            response.line("Zone outlines are no longer visible", Formatting.WHITE);
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to disable zone rendering: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }
    
    /**
     * Show zone rendering status and settings.
     * Usage: /mochamix zones render status
     */
    public static int showZoneRenderStatus(CommandContext<FabricClientCommandSource> ctx) {
        try {
            boolean enabled = rocamocha.mochamix.render.ZoneDebugRenderer.isEnabled();
            boolean autoSync = rocamocha.mochamix.render.ZoneDebugRenderer.isAutoSyncEnabled();
            int activeZones = rocamocha.mochamix.render.ZoneDebugRenderer.getActiveZones().size();
            int totalZones = rocamocha.mochamix.zones.ZoneUtils.getZoneCount();
            
            TextBuilder response = new TextBuilder();
            response.line("=== Zone Rendering Status ===", Formatting.GOLD, Formatting.BOLD);
            response.line("Rendering: " + (enabled ? "Enabled" : "Disabled"), 
                enabled ? Formatting.GREEN : Formatting.RED);
            response.line("Auto-Sync: " + (autoSync ? "Enabled" : "Disabled"), 
                autoSync ? Formatting.GREEN : Formatting.RED);
            response.line("Active Zones: " + activeZones + "/" + totalZones, Formatting.WHITE);
            
            if (!enabled) {
                response.line("Use '/mochamix zones render on' to enable", Formatting.GRAY);
            } else if (activeZones != totalZones) {
                response.line("Use '/mochamix zones render sync' to refresh", Formatting.GRAY);
            }
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to get zone render status: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }
    
    /**
     * Manually sync zone rendering with persisted zones.
     * Usage: /mochamix zones render sync
     */
    public static int syncZoneRendering(CommandContext<FabricClientCommandSource> ctx) {
        try {
            rocamocha.mochamix.render.ZoneDebugRenderer.syncWithPersistedZones();
            
            int activeZones = rocamocha.mochamix.render.ZoneDebugRenderer.getActiveZones().size();
            
            TextBuilder response = new TextBuilder();
            response.line("Zone Rendering Synced!", Formatting.GREEN, Formatting.BOLD);
            response.line("Now displaying " + activeZones + " zones", Formatting.WHITE);
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to sync zone rendering: " + e.getMessage(), Formatting.RED);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }

    /**
     * Debug command to show zone file path and system information.
     * Usage: /mochamix zones debug
     */
    public static int debugZoneSystem(CommandContext<FabricClientCommandSource> ctx) {
        try {
            // Import the ZoneDataManager
            rocamocha.mochamix.zones.ZoneDataManager manager = rocamocha.mochamix.zones.ZoneDataManager.getInstance();
            String debugInfo = manager.getZonesFileDebugInfo();
            
            TextBuilder response = new TextBuilder();
            response.line("=== Zone System Debug ===", Formatting.GOLD, Formatting.BOLD);
            response.newline();
            
            // Split debug info into lines and format them
            String[] lines = debugInfo.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                if (line.startsWith("===")) {
                    response.line(line, Formatting.GOLD, Formatting.BOLD);
                } else if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        response.raw(parts[0] + ": ", Formatting.GRAY);
                        response.raw(parts[1].trim(), Formatting.WHITE);
                        response.newline();
                    } else {
                        response.line(line, Formatting.WHITE);
                    }
                } else {
                    response.line(line, Formatting.WHITE);
                }
            }
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to get debug info: " + e.getMessage(), Formatting.RED);
            error.line("Stack trace: " + e.getClass().getSimpleName(), Formatting.GRAY);
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }
}