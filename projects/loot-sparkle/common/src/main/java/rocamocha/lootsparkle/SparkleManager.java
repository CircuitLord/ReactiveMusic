package rocamocha.lootsparkle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
    private static final int SPAWN_RADIUS = 36;

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

        // Check if sky is visible to the player at their current position
        boolean skyVisibleToPlayer = world.isSkyVisible(center);

        // Find a random valid position with sky visibility constraints
        BlockPos spawnPos = findValidSpawnPosition(world, center, SPAWN_RADIUS, skyVisibleToPlayer);
        if (spawnPos != null) {
            Sparkle sparkle = new Sparkle(playerId, spawnPos, world);
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
     * Forces all sparkles to expire immediately (debug command)
     * @return The number of sparkles that were expired
     */
    public static int expireAllSparkles() {
        int expiredCount = 0;
        for (List<Sparkle> playerSparkleList : playerSparkles.values()) {
            expiredCount += playerSparkleList.size();
            playerSparkleList.clear();
        }
        LootSparkle.LOGGER.info("Debug command: Expired {} sparkles", expiredCount);
        return expiredCount;
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
                if (sparkle.isExpired() || isSparkleExpiredDueToDistance(sparkle, world)) {
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

    private static BlockPos findValidSpawnPosition(World world, BlockPos center, int radius, boolean skyVisibleToPlayer) {
        Random random = new Random();

        for (int attempts = 0; attempts < 50; attempts++) {
            // Generate random position within radius
            int x = center.getX() + random.nextInt(radius * 2) - radius;
            int z = center.getZ() + random.nextInt(radius * 2) - radius;

            // For cave spawning, search within vertical radius around player
            int playerY = center.getY();
            int verticalRadius = LootSparkleConfig.getVerticalSpawnRadius();

            // Search for valid positions within vertical range
            for (int yOffset = -verticalRadius; yOffset <= verticalRadius; yOffset++) {
                int y = playerY + yOffset;

                // Stay within world bounds
                if (y <= world.getBottomY() + 1 || y >= world.getTopY() - 1) {
                    continue;
                }

                BlockPos pos = new BlockPos(x, y, z);

                // Check if position is valid
                if (isValidSpawnPosition(world, pos)) {
                    // Check sky visibility constraint
                    boolean skyVisibleAtPos = world.isSkyVisible(pos);
                    if (skyVisibleToPlayer == skyVisibleAtPos) {  // Match sky visibility
                        // If spawning in non-sky-visible location, check reachability with simple pathfinding
                        if (!skyVisibleAtPos && !isReachable(world, center, pos, 36)) {
                            continue;  // Skip if not reachable
                        }
                        return pos;
                    }
                }
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
                    sparkle.getPosition(),
                    sparkle.getTier().getLevel()
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
     * Sends an interaction failed packet to the player
     */
    private static void sendInteractionFailedPacket(ServerPlayerEntity player, String reason) {
        ServerPlayNetworking.send(player, new SparkleNetworking.InteractionFailedPacket(reason));
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
                    // Verify player is close enough to the sparkle (same distance check as client)
                    double distance = player.getPos().distanceTo(Vec3d.ofCenter(sparkle.getPosition()));
                    if (distance <= 3.0) {
                        // Spawn experience orbs based on sparkle tier
                        int experienceAmount = getExperienceForTier(sparkle.getTier());
                        spawnExperienceOrbs(player.getWorld(), sparkle.getPosition(), experienceAmount);
                        
                        openSparkleInventory(player.getUuid(), sparkle);
                        LootSparkle.LOGGER.debug("Player {} successfully interacted with sparkle at {} and spawned {} experience orbs", 
                            player.getUuid(), sparkle.getPosition(), experienceAmount);
                    } else {
                        sendInteractionFailedPacket(player, "You are too far away from the sparkle");
                        LootSparkle.LOGGER.debug("Player {} attempted to interact with sparkle at {} but is too far ({} blocks away)",
                            player.getUuid(), sparkle.getPosition(), distance);
                    }
                    return;
                }
            }
        }
        sendInteractionFailedPacket(player, "This sparkle no longer exists");
        LootSparkle.LOGGER.debug("Player {} attempted to interact with sparkle {} but it no longer exists", player.getUuid(), sparkleId);
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

    /**
     * Gets the experience amount to award for interacting with a sparkle of the given tier
     */
    private static int getExperienceForTier(SparkleTier tier) {
        return switch (tier) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 5;
            case LEGENDARY -> 8;
            case DIVINE -> 12;
        };
    }

    /**
     * Spawns experience orbs at the specified position
     */
    private static void spawnExperienceOrbs(World world, BlockPos position, int experienceAmount) {
        if (world instanceof ServerWorld serverWorld) {
            // Create experience orb at the sparkle's position with a small random offset
            double x = position.getX() + 0.5 + (world.getRandom().nextDouble() - 0.5) * 0.5;
            double y = position.getY() + 0.5;
            double z = position.getZ() + 0.5 + (world.getRandom().nextDouble() - 0.5) * 0.5;
            
            ExperienceOrbEntity orb = new ExperienceOrbEntity(serverWorld, x, y, z, experienceAmount);
            serverWorld.spawnEntity(orb);
        }
    }

    /**
     * Checks if a sparkle should expire due to player distance
     */
    private static boolean isSparkleExpiredDueToDistance(Sparkle sparkle, ServerWorld world) {
        // Get the player associated with this sparkle
        ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(sparkle.getPlayerId());
        if (player == null) {
            // Player is not online, don't expire due to distance
            return false;
        }

        // Calculate distance between player and sparkle
        double distance = player.getPos().distanceTo(Vec3d.ofCenter(sparkle.getPosition()));
        
        // Base lifetime in milliseconds
        long baseLifetime = LootSparkleConfig.getSparkleLifetimeMs();
        
        // Distance-based lifetime multiplier
        // Closer = longer lifetime, further = shorter lifetime
        // At 0 blocks: 1.0x lifetime (normal)
        // At 32 blocks: 0.5x lifetime (half)
        // At 64 blocks: 0.25x lifetime (quarter)
        // At 128+ blocks: 0.1x lifetime (tenth)
        double distanceMultiplier = Math.max(0.1, 1.0 - (distance / 128.0));
        
        // Calculate effective lifetime
        long effectiveLifetime = (long) (baseLifetime * distanceMultiplier);
        
        // Check if sparkle has exceeded its effective lifetime
        long age = System.currentTimeMillis() - sparkle.getCreationTime();
        return age > effectiveLifetime;
    }

    /**
     * Checks if a position is valid for sparkle spawning
     */
    private static boolean isValidSpawnPosition(World world, BlockPos pos) {
        // Must be air block
        if (!world.getBlockState(pos).isAir()) {
            return false;
        }

        // Must have solid block below
        BlockPos below = pos.down();
        return world.getBlockState(below).isSolidBlock(world, below);
    }

    /**
     * Simple pathfinding check to ensure the sparkle is reachable within the given radius
     * Uses a basic breadth-first search to check if there's a clear path
     */
    private static boolean isReachable(World world, BlockPos start, BlockPos end, int maxRadius) {
        // Check distance first
        if (start.getSquaredDistance(end) > maxRadius * maxRadius) {
            return false;
        }

        // Simple BFS for reachability (checks for solid blocks blocking the path)
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        int[][] directions = {
            {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0},  // Horizontal movement
            {0, 1, 0}, {0, -1, 0}  // Vertical movement for caves
        };

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (current.equals(end)) {
                return true;
            }

            for (int[] dir : directions) {
                BlockPos next = current.add(dir[0], dir[1], dir[2]);
                if (!visited.contains(next) && next.getSquaredDistance(start) <= maxRadius * maxRadius) {
                    // Check if the block is passable and grounded
                    if (isPassableAndGrounded(world, next)) {
                        visited.add(next);
                        queue.add(next);
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if a block position is passable and has solid ground within 4 blocks below
     * This prevents pathfinding through large open spaces (flying)
     */
    private static boolean isPassableAndGrounded(World world, BlockPos pos) {
        // First check if the block itself is passable
        if (!world.getBlockState(pos).isAir() && world.getBlockState(pos).isSolidBlock(world, pos)) {
            return false; // Solid blocks are not passable
        }

        // Check for solid ground within 4 blocks below
        for (int yOffset = 1; yOffset <= 4; yOffset++) {
            BlockPos checkPos = pos.down(yOffset);
            if (world.getBlockState(checkPos).isSolidBlock(world, checkPos)) {
                return true; // Found solid ground within 4 blocks
            }
        }

        return false; // No solid ground found within 4 blocks below
    }
}