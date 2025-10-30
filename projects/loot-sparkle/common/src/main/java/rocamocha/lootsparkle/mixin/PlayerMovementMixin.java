package rocamocha.lootsparkle.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import rocamocha.lootsparkle.ClientSparkleManager;
import rocamocha.lootsparkle.SparkleManager;
import rocamocha.lootsparkle.SparkleNetworking;

/**
 * Mixin to handle player movement and sparkle spawning/interaction
 *
 * Injects into player movement to potentially spawn sparkles and detect crouching near them
 */
@Mixin(PlayerEntity.class)
public class PlayerMovementMixin {

    // Track previous crouching state to detect when crouching starts
    private boolean wasSneaking = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onPlayerTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        World world = player.getWorld();

        // Only spawn sparkles on server side
        if (world.isClient()) {
            // Handle client-side crouching detection
            handleClientCrouchingDetection(player);
            return;
        }

        // Server-side sparkle spawning
        // Random chance to spawn sparkle (adjust probability as needed)
        if (world.random.nextFloat() < 0.01f) { // 1% chance per tick for testing
            BlockPos playerPos = player.getBlockPos();
            SparkleManager.spawnSparkleForPlayer(player.getUuid(), world, playerPos);
        }
    }

    private void handleClientCrouchingDetection(PlayerEntity player) {
        boolean isCurrentlySneaking = player.isSneaking();

        // Detect when player starts crouching (was not sneaking, now is)
        if (!wasSneaking && isCurrentlySneaking) {
            checkForNearbySparkles(player);
        }

        // Update previous state
        wasSneaking = isCurrentlySneaking;
    }

    private void checkForNearbySparkles(PlayerEntity player) {
        // Check if player is near any sparkles
        ClientSparkleManager.ClientSparkle nearbySparkle = findNearbySparkle(player);
        if (nearbySparkle != null) {
            // Send interaction packet to server
            ClientPlayNetworking.send(new SparkleNetworking.InteractSparklePacket(nearbySparkle.getSparkleId()));
        }
    }

    private ClientSparkleManager.ClientSparkle findNearbySparkle(PlayerEntity player) {
        var playerSparkles = ClientSparkleManager.getPlayerSparkles(player.getUuid());

        Vec3d playerPos = player.getPos();
        final double INTERACTION_RADIUS = 3.0;

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
}