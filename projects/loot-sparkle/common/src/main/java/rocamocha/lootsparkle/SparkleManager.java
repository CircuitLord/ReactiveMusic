package rocamocha.lootsparkle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * Manages sparkle entities/effects in the world
 *
 * Handles:
 * - Sparkle creation and spawning
 * - Sparkle lifecycle management
 * - Per-player sparkle instances
 */
public class SparkleManager {
    // Map of player UUID to their active sparkles
    private static final Map<UUID, List<Sparkle>> playerSparkles = new HashMap<>();

    // Maximum sparkles per player
    private static final int MAX_SPARKLES_PER_PLAYER = 5;

    // Sparkle spawn radius
    private static final int SPAWN_RADIUS = 32;

    // Reference to server for networking
    private static net.minecraft.server.MinecraftServer server;

    public static void initialize() {
        // Store server reference for networking
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);

        // Register server tick event to update sparkles
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            for (ServerWorld world : s.getWorlds()) {
                updateSparkles(world);
            }
        });
    }

    /**
     * Spawns a new sparkle for a player at a random location
     */
    public static void spawnSparkleForPlayer(UUID playerId, World world, BlockPos center) {
        List<Sparkle> sparkles = playerSparkles.computeIfAbsent(playerId, k -> new ArrayList<>());

        // Remove expired sparkles
        sparkles.removeIf(Sparkle::isExpired);

        // Check if we can spawn more sparkles
        if (sparkles.size() >= MAX_SPARKLES_PER_PLAYER) {
            return;
        }

        // Find a random valid position
        BlockPos spawnPos = findValidSpawnPosition(world, center, SPAWN_RADIUS);
        if (spawnPos != null) {
            Sparkle sparkle = new Sparkle(playerId, spawnPos);
            sparkles.add(sparkle);

            // Send sync packet to the player
            sendSparkleSyncPacket(playerId, sparkle);

            LootSparkle.LOGGER.info("Spawned sparkle for player {} at {}", playerId, spawnPos);
        }
    }

    /**
     * Gets all active sparkles for a player
     */
    public static List<Sparkle> getPlayerSparkles(UUID playerId) {
        return playerSparkles.getOrDefault(playerId, Collections.emptyList());
    }

    /**
     * Removes a sparkle
     */
    public static void removeSparkle(UUID playerId, Sparkle sparkle) {
        List<Sparkle> sparkles = playerSparkles.get(playerId);
        if (sparkles != null && sparkles.remove(sparkle)) {
            // Send remove packet to the player
            sendSparkleRemovePacket(playerId, sparkle.getSparkleId());
        }
    }

    private static void updateSparkles(ServerWorld world) {
        // Update all sparkles and remove expired ones
        playerSparkles.values().forEach(sparkles ->
            sparkles.removeIf(sparkle -> {
                sparkle.update(world);
                if (sparkle.isExpired()) {
                    // Send remove packet to the player
                    sendSparkleRemovePacket(sparkle.getPlayerId(), sparkle.getSparkleId());
                    LootSparkle.LOGGER.info("Removed expired sparkle for player {} at {}", sparkle.getPlayerId(), sparkle.getPosition());
                    return true;
                }
                return false;
            })
        );

        // Spawn sparkles for active players
        spawnSparklesForActivePlayers(world);
    }

    private static void spawnSparklesForActivePlayers(ServerWorld world) {
        // Get all players in the world
        for (ServerPlayerEntity player : world.getPlayers()) {
            // Only spawn for players who are not in spectator mode and are alive
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }

            // Check if this player already has too many sparkles
            List<Sparkle> playerSparklesList = playerSparkles.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());
            if (playerSparklesList.size() >= MAX_SPARKLES_PER_PLAYER) {
                continue;
            }

            // Random chance to spawn a sparkle (adjust probability as needed)
            if (world.getRandom().nextFloat() < 0.02f) { // 2% chance per tick
                spawnSparkleForPlayer(player.getUuid(), world, player.getBlockPos());
            }
        }
    }

    private static BlockPos findValidSpawnPosition(World world, BlockPos center, int radius) {
        Random random = new Random();

        for (int attempts = 0; attempts < 50; attempts++) {
            int x = center.getX() + random.nextInt(radius * 2) - radius;
            int z = center.getZ() + random.nextInt(radius * 2) - radius;
            int y = world.getTopY() - 1; // Start from surface

            BlockPos pos = new BlockPos(x, y, z);

            // Find a valid position above solid ground
            while (y > world.getBottomY()) {
                if (world.getBlockState(pos).isAir() &&
                    world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                    return pos;
                }
                y--;
                pos = new BlockPos(x, y, z);
            }
        }

        return null;
    }

    /**
     * Sends a sparkle sync packet to the player
     */
    private static void sendSparkleSyncPacket(UUID playerId, Sparkle sparkle) {
        if (server != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null) {
                ServerPlayNetworking.send(player, new SparkleNetworking.SyncSparklePacket(
                    sparkle.getSparkleId(),
                    playerId,
                    sparkle.getPosition()
                ));
            }
        }
    }

    /**
     * Sends a sparkle remove packet to the player
     */
    private static void sendSparkleRemovePacket(UUID playerId, UUID sparkleId) {
        if (server != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null) {
                ServerPlayNetworking.send(player, new SparkleNetworking.RemoveSparklePacket(sparkleId, playerId));
            }
        }
    }

    /**
     * Remove a sparkle if its inventory is empty
     */
    public static void removeSparkleIfEmpty(UUID playerId, SimpleInventory inventory) {
        List<Sparkle> sparkles = playerSparkles.get(playerId);
        if (sparkles != null) {
            sparkles.removeIf(sparkle -> {
                if (sparkle.getInventory() == inventory && sparkle.isInventoryEmpty()) {
                    // Send remove packet to the player
                    sendSparkleRemovePacket(playerId, sparkle.getSparkleId());
                    LootSparkle.LOGGER.info("Removed empty sparkle for player {} at {}", playerId, sparkle.getPosition());
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Trigger sparkle interaction for a player
     */
    public static void triggerSparkleInteraction(ServerPlayerEntity player, UUID sparkleId) {
        List<Sparkle> sparkles = playerSparkles.get(player.getUuid());
        if (sparkles != null) {
            for (Sparkle sparkle : sparkles) {
                if (sparkle.getSparkleId().equals(sparkleId)) {
                    openSparkleInventory(player.getUuid(), sparkle);
                    return;
                }
            }
        }
    }

    /**
     * Open sparkle inventory for a player
     */
    private static void openSparkleInventory(UUID playerId, Sparkle sparkle) {
        if (server != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null) {
                // Open the sparkle inventory screen
                player.openHandledScreen(new SparkleScreenHandler.Factory(sparkle));
                LootSparkle.LOGGER.info("Player {} opened sparkle inventory at {}", playerId, sparkle.getPosition());
            }
        }
    }
}