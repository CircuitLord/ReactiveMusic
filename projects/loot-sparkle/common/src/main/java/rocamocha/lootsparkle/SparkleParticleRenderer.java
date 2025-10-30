package rocamocha.lootsparkle;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * Handles client-side particle rendering for sparkles
 *
 * Manages:
 * - Particle effect spawning
 * - Particle animation and positioning
 * - Performance optimization for multiple sparkles
 */
public class SparkleParticleRenderer {
    // Compass location enum
    private enum CompassLocation {
        NONE,
        MAIN_HAND,
        OFF_HAND,
        HOTBAR
    }

    // Particle spawn rate (every N ticks) - reduced for shorter particle lifetimes
    // Particle spawn rate (every N ticks) - reduced for shorter particle lifetimes
    private static final int PARTICLE_SPAWN_RATE = 5;

    // Number of particles per sparkle - reduced since we spawn more frequently
    private static final int PARTICLES_PER_SPARKLE = 2;

    // Compass-guided particle settings
    private static final double COMPASS_FAIRY_DISTANCE = 4; // Distance in front of player (increased)
    private static final double COMPASS_FAIRY_HEIGHT = 1.7; // Height above player (increased)
    private static final double COMPASS_FAIRY_FIGURE_EIGHT_SIZE = 0.43; // Size of figure-eight pattern
    private static final double LOOKING_THRESHOLD = 0.7; // Cosine of angle threshold (about 45 degrees)

    // Directional particle settings
    private static final int DIRECTIONAL_PARTICLES_PER_UNIT = 10; // More particles per distance unit
    private static final double DIRECTIONAL_PARTICLE_SPACING = 0.5; // Distance between particles
    private static final double DIRECTIONAL_DRIFT_AMPLITUDE_BASE = 0.6; // Base drift amplitude (doubled)
    private static final double DIRECTIONAL_DRIFT_AMPLITUDE_MAX = 3.4; // Maximum drift amplitude (doubled)
    private static final double DIRECTIONAL_DRIFT_DISTANCE_SCALE = 0.4; // How much distance affects drift
    private static final double DIRECTIONAL_DRIFT_SPEED = 0.15; // Animation speed for drift

    // Player-surrounding particle settings
    private static final int PLAYER_SURROUND_PARTICLES_PER_LAYER = 8; // Particles per layer
    private static final int PLAYER_SURROUND_LAYERS = 3; // Number of concentric layers
    private static final double[] PLAYER_SURROUND_RADII = {2.5, 3.6, 4.4}; // Radii for each layer
    private static final double PLAYER_SURROUND_HEIGHT_VARIATION = 2.0; // Height variation
    private static final double[] PLAYER_SURROUND_SPEEDS = {0.3, 0.2, 0.5}; // Rotation speeds for each layer

    private static int tickCounter = 0;
    private static int compassTickCounter = 0;
    private static double fairyAnimationTime = 0.0; // For figure-eight animation

    public static void initialize() {
        LootSparkle.LOGGER.info("Initializing sparkle particle renderer...");

        // Register world render event to spawn particles
        WorldRenderEvents.END.register(context -> {
            tickCounter++;
            compassTickCounter++;

            // Spawn sparkle particles
            if (tickCounter >= PARTICLE_SPAWN_RATE) {
                tickCounter = 0;
                spawnSparkleParticles(context.world());
            }

            // Spawn compass-guided particles
            if (compassTickCounter >= getCompassParticleRate(context.world())) {
                compassTickCounter = 0;
                spawnCompassParticles(context.world());
            }
        });
    }

    /**
     * Spawns particles for all active sparkles
     */
    private static void spawnSparkleParticles(ClientWorld world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        UUID playerId = client.player.getUuid();
        List<ClientSparkleManager.ClientSparkle> sparkles = ClientSparkleManager.getPlayerSparkles(playerId);

        ParticleManager particleManager = client.particleManager;

        for (ClientSparkleManager.ClientSparkle sparkle : sparkles) {
            spawnParticlesForSparkle(world, particleManager, sparkle);
        }
    }

    /**
     * Spawns particles for a single sparkle
     */
    private static void spawnParticlesForSparkle(ClientWorld world, ParticleManager particleManager, ClientSparkleManager.ClientSparkle sparkle) {
        BlockPos pos = sparkle.getPosition();
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);

        // Spawn multiple particles around the sparkle position
        for (int i = 0; i < PARTICLES_PER_SPARKLE; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
            double offsetY = world.random.nextDouble() * 0.2;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;

            Vec3d particlePos = center.add(offsetX, offsetY, offsetZ);

            // Spawn pink dust particles (bright pink color: RGB 1.0, 0.0, 1.0)
            DustParticleEffect pinkDust = new DustParticleEffect(new Vector3f(1.0f, 0.0f, 1.0f), 1.0f);
            particleManager.addParticle(
                pinkDust,
                particlePos.x, particlePos.y, particlePos.z,
                0.0, 0.01, 0.0 // Slight upward motion
            );
        }
    }

    /**
     * Checks if the player has a compass and returns where it's located
     */
    private static CompassLocation getCompassLocation(MinecraftClient client) {
        if (client.player == null) return CompassLocation.NONE;

        // Check main hand
        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.getItem() == Items.COMPASS) return CompassLocation.MAIN_HAND;

        // Check off hand
        ItemStack offHand = client.player.getOffHandStack();
        if (offHand.getItem() == Items.COMPASS) return CompassLocation.OFF_HAND;

        // Check hotbar slots (0-8)
        for (int slot = 0; slot < 9; slot++) {
            ItemStack hotbarStack = client.player.getInventory().getStack(slot);
            if (hotbarStack.getItem() == Items.COMPASS) return CompassLocation.HOTBAR;
        }

        return CompassLocation.NONE;
    }

    /**
     * Checks if the player has a compass (in hand or hotbar)
     */
    private static boolean isPlayerHoldingCompass(MinecraftClient client) {
        return getCompassLocation(client) != CompassLocation.NONE;
    }

    /**
     * Checks if the player is holding the compass in their hand (main or off)
     */
    private static boolean isPlayerHoldingCompassInHand(MinecraftClient client) {
        CompassLocation location = getCompassLocation(client);
        return location == CompassLocation.MAIN_HAND || location == CompassLocation.OFF_HAND;
    }

    /**
     * Finds the nearest sparkle to the player
     */
    private static ClientSparkleManager.ClientSparkle findNearestSparkle(ClientWorld world, MinecraftClient client) {
        if (client.player == null) return null;

        UUID playerId = client.player.getUuid();
        List<ClientSparkleManager.ClientSparkle> sparkles = ClientSparkleManager.getPlayerSparkles(playerId);

        ClientSparkleManager.ClientSparkle nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Vec3d playerPos = client.player.getPos();

        for (ClientSparkleManager.ClientSparkle sparkle : sparkles) {
            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            double distance = playerPos.distanceTo(sparklePos);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = sparkle;
            }
        }

        return nearest;
    }

    /**
     * Calculates the compass particle spawn rate based on distance to nearest sparkle
     */
    private static int getCompassParticleRate(ClientWorld world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isPlayerHoldingCompass(client)) return Integer.MAX_VALUE; // Don't spawn if no compass

        return 10; // Fixed rate for ring particles
    }

    /**
     * Spawns a single compass fairy particle in front and to the left of the player
     */
    private static void spawnCompassParticles(ClientWorld world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !isPlayerHoldingCompass(client)) return;

        ClientSparkleManager.ClientSparkle nearestSparkle = findNearestSparkle(world, client);
        if (nearestSparkle == null) return;

        ParticleManager particleManager = client.particleManager;
        Vec3d playerPos = client.player.getPos();

        // Update fairy animation time for figure-eight pattern
        fairyAnimationTime += 0.1; // Slow, smooth animation

        // Determine fairy color based on sparkle targeting
        Vector3f particleColor = getFairyColor(client, nearestSparkle);

        // Get player's facing direction
        Vec3d playerFacing = client.player.getRotationVector();
        Vec3d frontOffset = new Vec3d(playerFacing.x, 0, playerFacing.z).normalize().multiply(COMPASS_FAIRY_DISTANCE);

        // Calculate left offset (opposite of right - cross product with down vector)
        Vec3d leftOffset = frontOffset.crossProduct(new Vec3d(0, -1, 0)).normalize().multiply(0.7);

        // Calculate figure-eight pattern offset
        double figureEightX = Math.sin(fairyAnimationTime) * COMPASS_FAIRY_FIGURE_EIGHT_SIZE;
        double figureEightY = Math.sin(fairyAnimationTime * 2) * COMPASS_FAIRY_FIGURE_EIGHT_SIZE * 0.5;

        // Position fairy with figure-eight animation
        Vec3d fairyPos = playerPos.add(frontOffset).add(leftOffset)
            .add(figureEightX, figureEightY + COMPASS_FAIRY_HEIGHT, 0);

        // Create colored fairy particle
        DustParticleEffect fairyParticle = new DustParticleEffect(particleColor, 1.0f);
        particleManager.addParticle(
            fairyParticle,
            fairyPos.x, fairyPos.y, fairyPos.z,
            0.0, 0.01, 0.0 // Slight upward drift for magical effect
        );

        // Spawn directional particles if compass is held in hand
        if (isPlayerHoldingCompassInHand(client)) {
            spawnDirectionalParticles(world, particleManager, nearestSparkle, fairyPos, particleColor);
            spawnPlayerSurroundParticles(world, particleManager, playerPos);
        }
    }

    /**
     * Spawns directional particles along the path from sparkle to fairy with magical drift
     */
    private static void spawnDirectionalParticles(ClientWorld world, ParticleManager particleManager,
                                                 ClientSparkleManager.ClientSparkle sparkle, Vec3d fairyPos, Vector3f color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
        Vec3d playerPos = client.player.getPos();

        // Calculate direction vector from sparkle to fairy
        Vec3d direction = fairyPos.subtract(sparklePos);
        double distance = direction.length();

        // Calculate distance from player to sparkle for drift scaling
        double playerToSparkleDistance = playerPos.distanceTo(sparklePos);

        // Scale drift amplitude based on distance from player to sparkle
        // Further away = more drift for adventurous exploration
        double driftAmplitude = DIRECTIONAL_DRIFT_AMPLITUDE_BASE +
            Math.min(DIRECTIONAL_DRIFT_AMPLITUDE_MAX - DIRECTIONAL_DRIFT_AMPLITUDE_BASE,
                    playerToSparkleDistance * DIRECTIONAL_DRIFT_DISTANCE_SCALE);

        // Don't spawn particles if too close (would be cluttered)
        if (distance < 3.0) return;

        // Calculate number of particles based on distance and spacing
        int numParticles = Math.max(5, (int)(distance / DIRECTIONAL_PARTICLE_SPACING));

        // Create particles along the path with magical drift
        for (int i = 0; i < numParticles; i++) {
            double t = (double) i / (numParticles - 1); // 0 to 1 along the path
            Vec3d basePos = sparklePos.add(direction.multiply(t));

            // Add magical drift animation for each particle
            // Each particle has its own animation phase based on its position and time
            double particlePhase = fairyAnimationTime * DIRECTIONAL_DRIFT_SPEED + (i * 0.5); // Unique phase per particle

            // Create unique random characteristics for each particle
            long particleSeed = (long)(sparkle.getSparkleId().hashCode() + i * 31); // Unique seed per particle
            double randomOffset1 = (new java.util.Random(particleSeed).nextDouble() - 0.5) * 2.0; // -1 to 1
            double randomOffset2 = (new java.util.Random(particleSeed + 1).nextDouble() - 0.5) * 2.0; // -1 to 1
            double randomRotation = new java.util.Random(particleSeed + 2).nextDouble() * Math.PI * 2; // 0 to 2π
            double randomSpeed = 0.8 + new java.util.Random(particleSeed + 3).nextDouble() * 0.4; // 0.8 to 1.2

            // Apply random rotation to the drift pattern for uniqueness
            double rotatedPhase1 = particlePhase * randomSpeed + randomOffset1;
            double rotatedPhase2 = (particlePhase * 1.3 + Math.PI * 0.5) * randomSpeed + randomOffset2;
            double rotatedPhase3 = (particlePhase * 0.8 + randomRotation) * randomSpeed;

            // Create whimsical drift using multiple sine waves, scaled by distance
            double driftX = Math.sin(rotatedPhase1) * driftAmplitude;
            double driftY = Math.sin(rotatedPhase2) * driftAmplitude * 0.6;
            double driftZ = Math.cos(rotatedPhase3) * driftAmplitude;

            // Apply drift to base position
            Vec3d particlePos = basePos.add(driftX, driftY, driftZ);

            // Add some additional randomness for extra magic
            double randomOffsetX = (world.random.nextDouble() - 0.5) * 0.1;
            double randomOffsetY = (world.random.nextDouble() - 0.5) * 0.1;
            double randomOffsetZ = (world.random.nextDouble() - 0.5) * 0.1;
            particlePos = particlePos.add(randomOffsetX, randomOffsetY, randomOffsetZ);

            // Create directional particle with magical color variation
            Vector3f particleColor = new Vector3f(color);
            // Add slight color variation based on particle position
            float colorVariation = 0.1f + (float)(Math.sin(particlePhase * 2) * 0.05f);
            particleColor.add(colorVariation, colorVariation * 0.5f, colorVariation * 0.8f);

            DustParticleEffect directionalParticle = new DustParticleEffect(particleColor, 0.6f);

            particleManager.addParticle(
                directionalParticle,
                particlePos.x, particlePos.y, particlePos.z,
                0.0, 0.002, 0.0 // Very gentle upward motion
            );
        }
    }

    /**
     * Spawns whimsical particles that circle around the player in multiple layers
     */
    private static void spawnPlayerSurroundParticles(ClientWorld world, ParticleManager particleManager, Vec3d playerPos) {
        // Create multiple layers of particles circling around the player
        for (int layer = 0; layer < PLAYER_SURROUND_LAYERS; layer++) {
            double radius = PLAYER_SURROUND_RADII[layer];
            double speed = PLAYER_SURROUND_SPEEDS[layer];

            // Create particles for this layer
            for (int i = 0; i < PLAYER_SURROUND_PARTICLES_PER_LAYER; i++) {
                // Calculate angle for this particle in the circle
                double angle = (fairyAnimationTime * speed) + (i * (Math.PI * 2 / PLAYER_SURROUND_PARTICLES_PER_LAYER));

                // Add some randomness to make it less uniform
                long seedBase = layer * 100 + i * 7; // Unique seed per layer and particle
                double randomAngleOffset = (new java.util.Random(seedBase).nextDouble() - 0.5) * 0.5;
                double randomRadiusOffset = (new java.util.Random(seedBase + 1).nextDouble() - 0.5) * 0.3;

                // Dynamic height variation that changes over time
                long heightSeed = (long)(fairyAnimationTime * 1000) + layer * 1000 + i * 100;
                double randomHeightOffset = (new java.util.Random(heightSeed).nextDouble() - 0.5) * PLAYER_SURROUND_HEIGHT_VARIATION;

                double finalAngle = angle + randomAngleOffset;
                double finalRadius = radius + randomRadiusOffset;
                double height = playerPos.y + 1.0 + randomHeightOffset; // Eye level + variation

                // Calculate position on the circle
                double x = playerPos.x + Math.cos(finalAngle) * finalRadius;
                double z = playerPos.z + Math.sin(finalAngle) * finalRadius;

                // Generate random color for each particle (changes over time)
                long timeBasedSeed = (long)(fairyAnimationTime * 1000) + layer * 1000 + i * 100;
                java.util.Random colorRandom = new java.util.Random(timeBasedSeed);
                float red = colorRandom.nextFloat();
                float green = colorRandom.nextFloat();
                float blue = colorRandom.nextFloat();

                // Ensure colors are bright enough (avoid too dark)
                red = Math.max(red, 0.3f);
                green = Math.max(green, 0.3f);
                blue = Math.max(blue, 0.3f);

                // Add layer-based color tinting for visual distinction
                switch (layer) {
                    case 0: // Inner layer - slightly more saturated
                        red *= 1.1f;
                        green *= 1.1f;
                        blue *= 1.1f;
                        break;
                    case 1: // Middle layer - normal
                        break;
                    case 2: // Outer layer - slightly more pastel
                        red = (red + 0.5f) / 1.5f;
                        green = (green + 0.5f) / 1.5f;
                        blue = (blue + 0.5f) / 1.5f;
                        break;
                }

                Vector3f particleColor = new Vector3f(red, green, blue);

                // Create the particle
                DustParticleEffect surroundParticle = new DustParticleEffect(particleColor, 0.8f);
                particleManager.addParticle(
                    surroundParticle,
                    x, height, z,
                    0.0, 0.005, 0.0 // Gentle upward drift
                );
            }
        }
    }

    /**
     * Checks if the player is looking toward the nearest sparkle within the angle threshold
     */
    private static boolean isPlayerLookingAtSparkle(MinecraftClient client, ClientSparkleManager.ClientSparkle sparkle) {
        Vec3d playerPos = client.player.getPos();
        Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());

        // Get direction vectors
        Vec3d toSparkle = sparklePos.subtract(playerPos).normalize();
        Vec3d playerFacing = client.player.getRotationVector();

        // Calculate dot product to find angle between vectors
        double dotProduct = toSparkle.dotProduct(playerFacing);

        // If dot product is above threshold, player is looking in the right direction
        return dotProduct > LOOKING_THRESHOLD;
    }

    /**
     * Determines the fairy's color based on sparkle targeting
     */
    private static Vector3f getFairyColor(MinecraftClient client, ClientSparkleManager.ClientSparkle nearestSparkle) {
        if (client.player == null) return new Vector3f(0.5f, 0.5f, 0.5f); // Gray fallback

        UUID playerId = client.player.getUuid();
        List<ClientSparkleManager.ClientSparkle> allSparkles = ClientSparkleManager.getPlayerSparkles(playerId);

        Vec3d playerPos = client.player.getPos();
        Vec3d playerFacing = client.player.getRotationVector();

        ClientSparkleManager.ClientSparkle targetedSparkle = null;
        boolean isNearest = false;

        // Find if player is looking at any sparkle
        for (ClientSparkleManager.ClientSparkle sparkle : allSparkles) {
            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            Vec3d toSparkle = sparklePos.subtract(playerPos).normalize();

            double dotProduct = toSparkle.dotProduct(playerFacing);

            if (dotProduct > LOOKING_THRESHOLD) {
                targetedSparkle = sparkle;
                isNearest = sparkle == nearestSparkle;
                break; // Found the first (closest in angle) sparkle being targeted
            }
        }

        if (targetedSparkle == null) {
            // Not looking at any sparkle
            return new Vector3f(0.7f, 0.7f, 0.7f); // Light gray/white
        } else if (isNearest) {
            // Looking at the nearest sparkle
            return new Vector3f(0.2f, 0.6f, 1.0f); // Blue
        } else {
            // Looking at a sparkle but not the nearest
            return new Vector3f(1.0f, 1.0f, 0.2f); // Yellow
        }
    }
}