package rocamocha.lootsparkle;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side entry point for Loot Sparkle mod
 *
 * Handles client-specific functionality including:
 * - Particle effects rendering
 * - Key binding registration
 * - Client-side sparkle interaction
 */
public class LootSparkleClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(LootSparkle.MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Loot Sparkle client...");

        // Initialize particle effects
        SparkleParticleRenderer.initialize();

        // Initialize client-side sparkle manager for network sync
        ClientSparkleManager.initialize();

        // Register client-side packet codecs for packets the client receives
        SparkleNetworking.registerClientCodecs();

        LOGGER.info("Loot Sparkle client initialized successfully!");
    }
}