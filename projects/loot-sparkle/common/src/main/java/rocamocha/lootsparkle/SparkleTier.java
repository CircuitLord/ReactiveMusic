package rocamocha.lootsparkle;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the tier/rarity of a sparkle, which determines loot quality and sources
 */
public enum SparkleTier {
    COMMON(0, "common", 60, List.of(
        "loot-sparkle:tiers/common/basic",
        "loot-sparkle:tiers/common/underground"
    )),
    UNCOMMON(1, "uncommon", 25, List.of(
        "loot-sparkle:tiers/uncommon/treasure",
        "loot-sparkle:tiers/uncommon/overworld"
    )),
    RARE(2, "rare", 12, List.of(
        "loot-sparkle:tiers/rare/valuable",
        "loot-sparkle:tiers/rare/special"
    )),
    EPIC(3, "epic", 3, List.of(
        "loot-sparkle:tiers/epic/legendary",
        "loot-sparkle:tiers/epic/enchanted"
    )),
    LEGENDARY(4, "legendary", 1, List.of(
        "loot-sparkle:tiers/legendary/mythical",
        "loot-sparkle:tiers/legendary/artifacts"
    )),
    DIVINE(5, "divine", 0, List.of(
        "loot-sparkle:tiers/divine/divine",
        "loot-sparkle:tiers/divine/celestial"
    ));

    private final int level;
    private final String name;
    private final int weight;
    private final List<String> lootTableIds;

    SparkleTier(int level, String name, int weight, List<String> lootTableIds) {
        this.level = level;
        this.name = name;
        this.weight = weight;
        this.lootTableIds = lootTableIds;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public List<String> getLootTableIds() {
        return lootTableIds;
    }

    /**
     * Selects a random tier based on weights, with optional modifiers from world context
     */
    public static SparkleTier selectRandomTier(World world, BlockPos position) {
        net.minecraft.util.math.random.Random random = world.getRandom();
        int totalWeight = 0;

        // Calculate base weights
        for (SparkleTier tier : values()) {
            totalWeight += tier.getWeight();
        }

        // Apply world context modifiers
        int modifiedTotalWeight = totalWeight;
        SparkleTier[] tiers = values();

        // Biome-based modifiers
        Biome biome = world.getBiome(position).value();
        if (biome.getTemperature() < 0.3) { // Cold biomes
            // Increase rare/epic chances in cold biomes
            modifiedTotalWeight += 8; // Add more weight for better tiers
        } else if (biome.getTemperature() > 0.8) { // Hot biomes
            // Slightly increase uncommon chances in hot biomes
            modifiedTotalWeight += 3;
        }

        // Y-level based modifiers
        int y = position.getY();
        if (y < 0) { // Deep underground
            // Significantly increase rare/epic chances deep underground
            modifiedTotalWeight += 20;
        } else if (y > 100) { // High altitudes
            // Slightly increase uncommon chances at high altitudes
            modifiedTotalWeight += 4;
        }

        // Special conditions for legendary/divine tiers
        boolean canSpawnLegendary = false;
        boolean canSpawnDivine = false;

        // Legendary conditions: Very deep underground OR specific rare biomes
        if (y < -32 || (biome.getTemperature() < 0.1 && y < 32)) {
            canSpawnLegendary = true;
            modifiedTotalWeight += 5; // Add legendary weight
        }

        // Divine conditions: Extremely rare - only in the deepest depths or most extreme conditions
        if (y < -64 || (biome.getTemperature() < 0.05 && y < -32)) {
            canSpawnDivine = true;
            modifiedTotalWeight += 2; // Add divine weight
        }

        // Select tier based on modified weights
        int roll = random.nextInt(modifiedTotalWeight);
        int currentWeight = 0;

        for (SparkleTier tier : tiers) {
            int tierWeight = tier.getWeight();

            // Add legendary weight if conditions met
            if (tier == LEGENDARY && canSpawnLegendary) {
                tierWeight += 5;
            }
            // Add divine weight if conditions met
            if (tier == DIVINE && canSpawnDivine) {
                tierWeight += 2;
            }

            currentWeight += tierWeight;
            if (roll < currentWeight) {
                return tier;
            }
        }

        // Fallback to common
        return COMMON;
    }

    /**
     * Gets loot table IDs for this tier, considering biome and Y-level modifiers
     */
    public List<String> getLootTableIds(World world, BlockPos position) {
        // Start with base loot tables (create mutable copy)
        List<String> tables = new ArrayList<>(lootTableIds);

        // Add biome-specific tables
        Biome biome = world.getBiome(position).value();
        String biomeCategory = getBiomeCategory(biome);

        if (biomeCategory != null) {
            tables.add("loot-sparkle:tiers/" + name + "/biomes/" + biomeCategory);
        }

        // Add Y-level specific tables
        int y = position.getY();
        String heightCategory = getHeightCategory(y);

        if (heightCategory != null) {
            tables.add("loot-sparkle:tiers/" + name + "/heights/" + heightCategory);
        }

        return tables;
    }

    private String getBiomeCategory(Biome biome) {
        // Temperature-based categories
        if (biome.getTemperature() < 0.1) {
            return "frozen";
        } else if (biome.getTemperature() < 0.3) {
            return "cold";
        } else if (biome.getTemperature() > 0.9) {
            return "hot";
        } else if (biome.getTemperature() > 0.7) {
            return "warm";
        }

        // For now, skip precipitation-based categories as the API might be different
        // TODO: Add precipitation-based categories when API is clarified

        return null; // No specific category
    }

    private String getHeightCategory(int y) {
        if (y < -32) {
            return "deep_caverns";
        } else if (y < 0) {
            return "underground";
        } else if (y > 128) {
            return "sky_high";
        } else if (y > 64) {
            return "mountains";
        }

        return null; // Surface level
    }
}