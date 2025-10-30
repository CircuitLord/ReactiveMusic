package rocamocha.lootsparkle;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for Loot Sparkle mod
 *
 * This mod adds per-player instanced particle effects ("sparkles") at random block locations
 * that contain loot table-generated inventories. Players can interact with sparkles by
 * crouching near them to open an inventory GUI.
 */
public class LootSparkle implements ModInitializer {
    public static final String MOD_ID = "loot-sparkle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Loot Sparkle...");

        // Load configuration first
        LootSparkleConfig.loadConfig();

        // Register sparkle entities/effects
        SparkleManager.initialize();

        // Register loot table integration
        LootTableIntegration.initialize();

        // Initialize networking
        SparkleNetworking.initialize();

        LOGGER.info("Loot Sparkle initialized successfully!");
    }
}