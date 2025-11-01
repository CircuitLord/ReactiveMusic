package rocamocha.lootsparkle;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side sparkle manager that stores sparkle data received from server
 */
public class ClientSparkleManager {
    // Client-side storage of sparkles received from server
    private static final Map<UUID, Map<UUID, ClientSparkle>> playerSparkles = new ConcurrentHashMap<>();

    public static void initialize() {
        LootSparkle.LOGGER.info("Initializing client sparkle manager...");

        // Register packet receivers
        ClientPlayNetworking.registerGlobalReceiver(SparkleNetworking.SYNC_SPARKLE,
            (packet, context) -> {
                context.client().execute(() -> {
                    syncSparkle(packet.sparkleId(), packet.playerId(), packet.position(), packet.tierLevel());
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(SparkleNetworking.REMOVE_SPARKLE,
            (packet, context) -> {
                context.client().execute(() -> {
                    removeSparkle(packet.sparkleId(), packet.playerId());
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(SparkleNetworking.INTERACTION_FAILED,
            (packet, context) -> {
                context.client().execute(() -> {
                    handleInteractionFailed(packet.reason());
                });
            });
    }

    /**
     * Called when server sends a sparkle sync packet
     */
    private static void syncSparkle(UUID sparkleId, UUID playerId, BlockPos position, int tierLevel) {
        Map<UUID, ClientSparkle> sparkles = playerSparkles.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        sparkles.put(sparkleId, new ClientSparkle(sparkleId, playerId, position, tierLevel));
        LootSparkle.LOGGER.debug("Synced sparkle {} for player {} at {} with tier {}", sparkleId, playerId, position, tierLevel);
    }

    /**
     * Called when server sends a sparkle removal packet
     */
    private static void removeSparkle(UUID sparkleId, UUID playerId) {
        Map<UUID, ClientSparkle> sparkles = playerSparkles.get(playerId);
        if (sparkles != null) {
            sparkles.remove(sparkleId);
            LootSparkle.LOGGER.debug("Removed sparkle {} for player {}", sparkleId, playerId);
        }
    }

    /**
     * Called when server sends an interaction failed packet
     */
    private static void handleInteractionFailed(String reason) {
        LootSparkle.LOGGER.info("Sparkle interaction failed: {}", reason);
        // TODO: Could show a client-side message to the player
    }

    /**
     * Get all sparkles for a player (client-side)
     */
    public static List<ClientSparkle> getPlayerSparkles(UUID playerId) {
        Map<UUID, ClientSparkle> sparkles = playerSparkles.get(playerId);
        return sparkles != null ? new ArrayList<>(sparkles.values()) : Collections.emptyList();
    }

    /**
     * Client-side sparkle representation (simplified)
     */
    public static class ClientSparkle {
        private final UUID sparkleId;
        private final UUID playerId;
        private final BlockPos position;
        private final int tierLevel;

        public ClientSparkle(UUID sparkleId, UUID playerId, BlockPos position, int tierLevel) {
            this.sparkleId = sparkleId;
            this.playerId = playerId;
            this.position = position;
            this.tierLevel = tierLevel;
        }

        public UUID getSparkleId() { return sparkleId; }
        public UUID getPlayerId() { return playerId; }
        public BlockPos getPosition() { return position; }
        public int getTierLevel() { return tierLevel; }
    }
}