package rocamocha.lootsparkle;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration manager for the Loot Sparkle mod
 *
 * Handles loading and saving configuration from config/loot-sparkle.properties
 */
public class LootSparkleConfig {
    private static final String CONFIG_FILE_NAME = "loot-sparkle.properties";
    private static final String SPARKLE_LIFETIME_KEY = "sparkle_lifetime_minutes";
    private static final String VERTICAL_RADIUS_KEY = "vertical_spawn_radius";
    private static final int DEFAULT_SPARKLE_LIFETIME_MINUTES = 10;
    private static final int DEFAULT_VERTICAL_RADIUS = 16;

    private static int sparkleLifetimeMinutes = DEFAULT_SPARKLE_LIFETIME_MINUTES;
    private static int verticalRadius = DEFAULT_VERTICAL_RADIUS;

    /**
     * Loads the configuration from the config file
     */
    public static void loadConfig() {
        try {
            // Get the config directory (works for both client and server)
            Path configDir = Paths.get("config");
            Path configFile = configDir.resolve(CONFIG_FILE_NAME);

            // Create config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Properties properties = new Properties();

            // Load existing config if it exists
            if (Files.exists(configFile)) {
                try (FileInputStream fis = new FileInputStream(configFile.toFile())) {
                    properties.load(fis);
                }
                LootSparkle.LOGGER.info("Loaded Loot Sparkle config from {}", configFile);
            } else {
                // Create default config file
                LootSparkle.LOGGER.info("Creating default Loot Sparkle config at {}", configFile);
            }

            // Read sparkle lifetime setting
            String lifetimeStr = properties.getProperty(SPARKLE_LIFETIME_KEY,
                String.valueOf(DEFAULT_SPARKLE_LIFETIME_MINUTES));
            try {
                sparkleLifetimeMinutes = Integer.parseInt(lifetimeStr);
                if (sparkleLifetimeMinutes <= 0) {
                    LootSparkle.LOGGER.warn("Invalid sparkle lifetime {}, using default of {} minutes",
                        sparkleLifetimeMinutes, DEFAULT_SPARKLE_LIFETIME_MINUTES);
                    sparkleLifetimeMinutes = DEFAULT_SPARKLE_LIFETIME_MINUTES;
                }
            } catch (NumberFormatException e) {
                LootSparkle.LOGGER.warn("Invalid sparkle lifetime value '{}', using default of {} minutes",
                    lifetimeStr, DEFAULT_SPARKLE_LIFETIME_MINUTES);
                sparkleLifetimeMinutes = DEFAULT_SPARKLE_LIFETIME_MINUTES;
            }

            // Read vertical radius setting
            String verticalRadiusStr = properties.getProperty(VERTICAL_RADIUS_KEY,
                String.valueOf(DEFAULT_VERTICAL_RADIUS));
            try {
                verticalRadius = Integer.parseInt(verticalRadiusStr);
                if (verticalRadius < 0) {
                    LootSparkle.LOGGER.warn("Invalid vertical radius {}, using default of {} blocks",
                        verticalRadius, DEFAULT_VERTICAL_RADIUS);
                    verticalRadius = DEFAULT_VERTICAL_RADIUS;
                }
            } catch (NumberFormatException e) {
                LootSparkle.LOGGER.warn("Invalid vertical radius value '{}', using default of {} blocks",
                    verticalRadiusStr, DEFAULT_VERTICAL_RADIUS);
                verticalRadius = DEFAULT_VERTICAL_RADIUS;
            }

            // Save the config (this will create the file with current values if it doesn't exist)
            saveConfig();

            LootSparkle.LOGGER.info("Loot Sparkle sparkle lifetime set to {} minutes", sparkleLifetimeMinutes);
            LootSparkle.LOGGER.info("Loot Sparkle vertical spawn radius set to {} blocks", verticalRadius);

        } catch (Exception e) {
            LootSparkle.LOGGER.error("Failed to load Loot Sparkle config, using defaults", e);
            sparkleLifetimeMinutes = DEFAULT_SPARKLE_LIFETIME_MINUTES;
        }
    }

    /**
     * Saves the current configuration to the config file
     */
    private static void saveConfig() {
        try {
            Path configDir = Paths.get("config");
            Path configFile = configDir.resolve(CONFIG_FILE_NAME);

            Properties properties = new Properties();
            properties.setProperty(SPARKLE_LIFETIME_KEY, String.valueOf(sparkleLifetimeMinutes));
            properties.setProperty(VERTICAL_RADIUS_KEY, String.valueOf(verticalRadius));

            // Add comments
            String comments = "Loot Sparkle Mod Configuration\n" +
                "sparkle_lifetime_minutes: How long sparkles last before disappearing (in minutes)\n" +
                "vertical_spawn_radius: Maximum vertical distance sparkles can spawn from player (in blocks)";

            try (FileOutputStream fos = new FileOutputStream(configFile.toFile())) {
                properties.store(fos, comments);
            }

        } catch (Exception e) {
            LootSparkle.LOGGER.error("Failed to save Loot Sparkle config", e);
        }
    }

    /**
     * Gets the sparkle lifetime in milliseconds
     */
    public static long getSparkleLifetimeMs() {
        return sparkleLifetimeMinutes * 60 * 1000L;
    }

    /**
     * Gets the sparkle lifetime in minutes
     */
    public static int getSparkleLifetimeMinutes() {
        return sparkleLifetimeMinutes;
    }

    /**
     * Gets the vertical spawn radius in blocks
     */
    public static int getVerticalSpawnRadius() {
        return verticalRadius;
    }
}