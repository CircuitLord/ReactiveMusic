package rocamocha.lootsparkle;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

/**
 * Handles client-side sparkle interaction logic
 *
 * Manages:
 * - Detecting nearby sparkles when crouching
 * - Opening sparkle inventory GUI
 * - Client-server communication for interactions
 */
public class SparkleInteractionHandler {
    // Interaction radius in blocks
    private static final double INTERACTION_RADIUS = 3.0;

    /**
     * Handles client-side sparkle interaction
     */
    public static void handleInteraction(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Only allow interaction when crouching
        if (!player.isSneaking()) return;

        // Find nearby sparkles
        ClientSparkleManager.ClientSparkle nearbySparkle = findNearbySparkle(player);
        if (nearbySparkle != null) {
            // Open the sparkle inventory GUI
            openSparkleInventory(client, nearbySparkle);
        }
    }

    /**
     * Finds a sparkle near the player's position
     */
    private static ClientSparkleManager.ClientSparkle findNearbySparkle(ClientPlayerEntity player) {
        UUID playerId = player.getUuid();
        List<ClientSparkleManager.ClientSparkle> playerSparkles = ClientSparkleManager.getPlayerSparkles(playerId);

        Vec3d playerPos = player.getPos();

        for (ClientSparkleManager.ClientSparkle sparkle : playerSparkles) {
            BlockPos sparklePos = sparkle.getPosition();
            Vec3d sparkleVec = new Vec3d(sparklePos.getX() + 0.5, sparklePos.getY() + 0.5, sparklePos.getZ() + 0.5);

            double distance = playerPos.distanceTo(sparkleVec);
            if (distance <= INTERACTION_RADIUS) {
                return sparkle;
            }
        }

        return null;
    }

    /**
     * Opens the inventory GUI for a sparkle
     */
    private static void openSparkleInventory(MinecraftClient client, ClientSparkleManager.ClientSparkle sparkle) {
        LootSparkle.LOGGER.info("Opening sparkle inventory at {}", sparkle.getPosition());

        // TODO: Implement GUI opening
        // This would create and open a custom Screen for the sparkle inventory
        // Similar to how chests/barrels open their GUIs

        // For now, just log the interaction
        LootSparkle.LOGGER.debug("Player interacted with sparkle containing items");
    }
}