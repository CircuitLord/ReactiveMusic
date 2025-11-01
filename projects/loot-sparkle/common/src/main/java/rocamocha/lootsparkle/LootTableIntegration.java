package rocamocha.lootsparkle;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles loot table integration for sparkle inventories
 *
 * Manages:
 * - Loot table registration
 * - Tier-based loot generation
 * - Biome and Y-level based loot table selection
 * - Datapack integration
 */
public class LootTableIntegration {
    // Base loot table identifiers
    public static final Identifier COMMON_LOOT_TABLE = Identifier.of(LootSparkle.MOD_ID, "tiers/common");
    public static final Identifier UNCOMMON_LOOT_TABLE = Identifier.of(LootSparkle.MOD_ID, "tiers/uncommon");
    public static final Identifier RARE_LOOT_TABLE = Identifier.of(LootSparkle.MOD_ID, "tiers/rare");
    public static final Identifier EPIC_LOOT_TABLE = Identifier.of(LootSparkle.MOD_ID, "tiers/epic");

    public static void initialize() {
        LootSparkle.LOGGER.info("Initializing loot table integration...");

        // Register the built-in datapack
        FabricLoader.getInstance().getModContainer(LootSparkle.MOD_ID).ifPresent(modContainer -> {
            ResourceManagerHelper.registerBuiltinResourcePack(
                Identifier.of(LootSparkle.MOD_ID, "sparkle_datapack"),
                modContainer,
                ResourcePackActivationType.ALWAYS_ENABLED
            );
        });
    }

    /**
     * Generates loot for a sparkle's inventory based on tier and world context
     */
    public static void generateLootForSparkle(SimpleInventory inventory, SparkleTier tier, World world, BlockPos position) {
        if (!(world instanceof ServerWorld serverWorld)) {
            LootSparkle.LOGGER.warn("Attempted to generate loot on client side, skipping");
            return;
        }

        LootSparkle.LOGGER.debug("Generating loot for sparkle tier {} at {}", tier.getName(), position);

        try {
            // Get all applicable loot table IDs for this tier and context
            List<String> lootTableIds = tier.getLootTableIds(world, position);
            List<LootTable> lootTables = new ArrayList<>();

            // Check if loot table registry is available
            var registryManager = serverWorld.getServer().getRegistryManager();
            var lootTableRegistry = registryManager.getOptional(RegistryKeys.LOOT_TABLE);

            if (lootTableRegistry.isEmpty()) {
                LootSparkle.LOGGER.debug("Loot table registry not available, using fallback loot generation");
                generateFallbackLoot(inventory, tier);
                return;
            }

            // Load all applicable loot tables
            for (String tableId : lootTableIds) {
                Identifier identifier = Identifier.of(tableId);
                LootTable lootTable = lootTableRegistry.get().get(identifier);
                if (lootTable != null && lootTable != LootTable.EMPTY) {
                    lootTables.add(lootTable);
                } else {
                    LootSparkle.LOGGER.debug("Loot table {} not found, skipping", tableId);
                }
            }

            // If no loot tables found, fall back to basic generation
            if (lootTables.isEmpty()) {
                generateFallbackLoot(inventory, tier);
                return;
            }

            // Generate loot from all applicable tables
            for (LootTable lootTable : lootTables) {
                // Create loot context
                LootContextParameterSet parameterSet = new LootContextParameterSet.Builder(serverWorld)
                    .add(LootContextParameters.ORIGIN, position.toCenterPos())
                    .build(net.minecraft.loot.context.LootContextTypes.CHEST);

                LootContext context = new LootContext.Builder(parameterSet).build(java.util.Optional.empty());

                // Generate loot and add to inventory
                lootTable.generateLoot(context, itemStack -> {
                    // Find first empty slot
                    for (int i = 0; i < inventory.size(); i++) {
                        if (inventory.getStack(i).isEmpty()) {
                            inventory.setStack(i, itemStack);
                            break;
                        }
                    }
                });
            }

            LootSparkle.LOGGER.debug("Generated loot for tier {} sparkle with {} loot tables", tier.getName(), lootTables.size());

        } catch (Exception e) {
            LootSparkle.LOGGER.error("Failed to generate loot for sparkle tier {}", tier.getName(), e);
            // Fallback to basic loot generation
            generateFallbackLoot(inventory, tier);
        }
    }

    /**
     * Generates basic fallback loot when loot tables are not available
     */
    private static void generateFallbackLoot(SimpleInventory inventory, SparkleTier tier) {
        LootSparkle.LOGGER.debug("Using fallback loot generation for tier {}", tier.getName());

        try {
            // Basic tier-based loot
            switch (tier) {
                case COMMON:
                    inventory.setStack(0, new net.minecraft.item.ItemStack(net.minecraft.item.Items.COAL, 3));
                    inventory.setStack(1, new net.minecraft.item.ItemStack(net.minecraft.item.Items.STICK, 2));
                    break;
                case UNCOMMON:
                    inventory.setStack(0, new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_INGOT, 2));
                    inventory.setStack(1, new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_NUGGET, 4));
                    inventory.setStack(2, new net.minecraft.item.ItemStack(net.minecraft.item.Items.BREAD, 2));
                    break;
                case RARE:
                    inventory.setStack(0, new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND, 1));
                    inventory.setStack(1, new net.minecraft.item.ItemStack(net.minecraft.item.Items.EMERALD, 1));
                    inventory.setStack(2, new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_INGOT, 3));
                    break;
                case EPIC:
                    inventory.setStack(0, new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND, 2));
                    inventory.setStack(1, new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_SCRAP, 1));
                    inventory.setStack(2, new net.minecraft.item.ItemStack(net.minecraft.item.Items.ENCHANTED_BOOK, 1));
                    break;
                case LEGENDARY:
                    inventory.setStack(0, new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_INGOT, 1));
                    inventory.setStack(1, new net.minecraft.item.ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING, 1));
                    inventory.setStack(2, new net.minecraft.item.ItemStack(net.minecraft.item.Items.ENCHANTED_GOLDEN_APPLE, 1));
                    break;
                case DIVINE:
                    inventory.setStack(0, new net.minecraft.item.ItemStack(net.minecraft.item.Items.DRAGON_EGG, 1));
                    inventory.setStack(1, new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHER_STAR, 1));
                    inventory.setStack(2, new net.minecraft.item.ItemStack(net.minecraft.item.Items.BEACON, 1));
                    break;
            }

            LootSparkle.LOGGER.debug("Generated fallback loot for tier {}", tier.getName());
        } catch (Exception e) {
            LootSparkle.LOGGER.error("Failed to generate fallback loot", e);
        }
    }

    /**
     * Gets the sparkle loot table from the server (legacy method)
     * @deprecated Use generateLootForSparkle with tier instead
     */
    @Deprecated
    public static LootTable getSparkleLootTable(ServerWorld world) {
        return world.getServer().getRegistryManager().get(RegistryKeys.LOOT_TABLE).get(COMMON_LOOT_TABLE);
    }
}