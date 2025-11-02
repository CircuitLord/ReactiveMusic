package rocamocha.lootsparkle;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Represents a single sparkle entity/effect
 *
 * Each sparkle has:
 * - A position in the world
 * - An inventory generated from loot tables
 * - A lifetime
 * - Particle effects
 * - A tier that determines loot quality
 */
public class Sparkle {
    private final UUID sparkleId;
    private final UUID playerId;
    private final BlockPos position;
    private final SimpleInventory inventory;
    private final long creationTime;
    private final long lifetime; // milliseconds
    private final SparkleTier tier;

    public Sparkle(UUID playerId, BlockPos position, World world) {
        this.sparkleId = UUID.randomUUID();
        this.playerId = playerId;
        this.position = position;
        this.inventory = new SimpleInventory(27); // 27 slots like a chest
        this.creationTime = System.currentTimeMillis();
        this.lifetime = LootSparkleConfig.getSparkleLifetimeMs();

        LootSparkle.LOGGER.info("Creating sparkle at {} for player {}", position, playerId);

        // Determine sparkle tier based on world context
        this.tier = SparkleTier.selectRandomTier(world, position);

        LootSparkle.LOGGER.info("Selected tier {} for sparkle", this.tier.getName());

        // Generate loot for this sparkle based on tier
        LootTableIntegration.generateLootForSparkle(this.inventory, this.tier, world, position);
    }

    public UUID getSparkleId() {
        return sparkleId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public BlockPos getPosition() {
        return position;
    }

    public SparkleTier getTier() {
        return tier;
    }

    public SimpleInventory getInventory() {
        return inventory;
    }

    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Checks if the sparkle should be removed (empty inventory or expired)
     */
    public boolean isExpired() {
        return isInventoryEmpty() || isLifetimeExpired();
    }

    /**
     * Checks if the inventory is empty
     */
    public boolean isInventoryEmpty() {
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the sparkle has exceeded its lifetime
     */
    public boolean isLifetimeExpired() {
        return System.currentTimeMillis() - creationTime > lifetime;
    }

    /**
     * Updates the sparkle (called each server tick)
     */
    public void update(World world) {
        // Check if inventory became empty
        if (isInventoryEmpty()) {
            LootSparkle.LOGGER.debug("Sparkle at {} expired due to empty inventory", position);
        }
    }

    /**
     * Gets the remaining lifetime in milliseconds
     */
    public long getRemainingLifetime() {
        return Math.max(0, lifetime - (System.currentTimeMillis() - creationTime));
    }
}