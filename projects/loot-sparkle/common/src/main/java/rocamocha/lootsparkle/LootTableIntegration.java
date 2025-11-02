package rocamocha.lootsparkle;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resource.ResourceManager;

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
        // Loot tables will be loaded directly from mod resources
    }

    /**
     * Generates loot for a sparkle's inventory based on tier and world context
     */
    public static void generateLootForSparkle(SimpleInventory inventory, SparkleTier tier, World world, BlockPos position) {
        LootSparkle.LOGGER.info("generateLootForSparkle called for tier {} at {}", tier.getName(), position);

        if (!(world instanceof ServerWorld serverWorld)) {
            LootSparkle.LOGGER.warn("Attempted to generate loot on client side, skipping");
            return;
        }

        LootSparkle.LOGGER.debug("Generating loot for sparkle tier {} at {}", tier.getName(), position);

        try {
            LootSparkle.LOGGER.info("Starting loot generation for tier {}", tier.getName());
            // Get all applicable loot table IDs for this tier and context
            List<String> lootTableIds = tier.getLootTableIds(world, position);
            LootSparkle.LOGGER.info("Loot table IDs for tier {}: {}", tier.getName(), lootTableIds);
            List<LootTable> lootTables = new ArrayList<>();

            // Load loot tables directly from mod resources
            ResourceManager resourceManager = serverWorld.getServer().getResourceManager();
            for (String tableId : lootTableIds) {
                try {
                    Identifier identifier = Identifier.of(tableId);
                    Identifier resourceId = Identifier.of(identifier.getNamespace(), "loot_tables/" + identifier.getPath() + ".json");
                    var resource = resourceManager.getResource(resourceId);
                    if (resource.isPresent()) {
                        var reader = resource.get().getReader();
                        var jsonElement = JsonParser.parseReader(reader);
                        var lootTableResult = LootTable.CODEC.parse(JsonOps.INSTANCE, jsonElement);
                        lootTableResult.resultOrPartial(error -> LootSparkle.LOGGER.error("Error parsing loot table {}: {}", tableId, error));
                        var lootTable = lootTableResult.result().orElse(LootTable.EMPTY);
                        if (lootTable != LootTable.EMPTY) {
                            lootTables.add(lootTable);
                            LootSparkle.LOGGER.info("Loaded loot table: {} for tier {}", tableId, tier.getName());
                        } else {
                            LootSparkle.LOGGER.warn("Loot table {} is empty for tier {}", tableId, tier.getName());
                        }
                        reader.close();
                    } else {
                        LootSparkle.LOGGER.warn("Loot table resource {} not found for tier {}", resourceId, tier.getName());
                    }
                } catch (Exception e) {
                    LootSparkle.LOGGER.error("Error loading loot table {}: {}", tableId, e.getMessage());
                }
            }

            // If no loot tables found, fall back to basic generation
            if (lootTables.isEmpty()) {
                LootSparkle.LOGGER.warn("No loot tables found for tier {}, using fallback loot generation", tier.getName());
                generateFallbackLoot(inventory, tier);
                return;
            }

            // Generate loot from all applicable tables
            List<net.minecraft.item.ItemStack> allLootItems = new ArrayList<>();
            for (LootTable lootTable : lootTables) {
                try {
                    // Create loot context
                    LootContextParameterSet parameterSet = new LootContextParameterSet.Builder(serverWorld)
                        .add(LootContextParameters.ORIGIN, position.toCenterPos())
                        .build(net.minecraft.loot.context.LootContextTypes.CHEST);

                    // Generate loot and collect items
                    var random = net.minecraft.util.math.random.Random.create();
                    ObjectArrayList<net.minecraft.item.ItemStack> lootItems = lootTable.generateLoot(parameterSet, random);
                    LootSparkle.LOGGER.info("Generated {} items from loot table for tier {}", lootItems.size(), tier.getName());
                    for (var itemStack : lootItems) {
                        LootSparkle.LOGGER.info("Generated item: {} x{} for tier {}", itemStack.getItem().toString(), itemStack.getCount(), tier.getName());
                        allLootItems.add(itemStack);
                    }
                } catch (Exception e) {
                    LootSparkle.LOGGER.error("Error generating loot from table: {}", e.getMessage());
                }
            }

            // Place items in random slots for a more natural chest-like distribution
            if (!allLootItems.isEmpty()) {
                List<Integer> availableSlots = new ArrayList<>();
                for (int i = 0; i < inventory.size(); i++) {
                    availableSlots.add(i);
                }
                Collections.shuffle(availableSlots);

                int slotIndex = 0;
                for (var itemStack : allLootItems) {
                    if (slotIndex < availableSlots.size()) {
                        int randomSlot = availableSlots.get(slotIndex);
                        inventory.setStack(randomSlot, itemStack);
                        slotIndex++;
                    }
                }
                LootSparkle.LOGGER.info("Placed {} items in random slots for tier {}", allLootItems.size(), tier.getName());
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
            List<net.minecraft.item.ItemStack> fallbackItems = new ArrayList<>();

            // Basic tier-based loot
            switch (tier) {
                case COMMON:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.COAL, 3));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.STICK, 2));
                    break;
                case UNCOMMON:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_INGOT, 2));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_NUGGET, 4));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.BREAD, 2));
                    break;
                case RARE:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.EMERALD, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_INGOT, 3));
                    break;
                case EPIC:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND, 2));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_SCRAP, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.ENCHANTED_BOOK, 1));
                    break;
                case LEGENDARY:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_INGOT, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.ENCHANTED_GOLDEN_APPLE, 1));
                    break;
                case DIVINE:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.DRAGON_EGG, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHER_STAR, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.BEACON, 1));
                    break;
            }

            // Place items in random slots
            if (!fallbackItems.isEmpty()) {
                List<Integer> availableSlots = new ArrayList<>();
                for (int i = 0; i < inventory.size(); i++) {
                    availableSlots.add(i);
                }
                Collections.shuffle(availableSlots);

                int slotIndex = 0;
                for (var itemStack : fallbackItems) {
                    if (slotIndex < availableSlots.size()) {
                        int randomSlot = availableSlots.get(slotIndex);
                        inventory.setStack(randomSlot, itemStack);
                        slotIndex++;
                    }
                }
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
        // Load directly from resources
        ResourceManager resourceManager = world.getServer().getResourceManager();
        Identifier resourceId = Identifier.of(LootSparkle.MOD_ID, "loot_tables/tiers/common.json");
        try {
            var resource = resourceManager.getResource(resourceId);
            if (resource.isPresent()) {
                var reader = resource.get().getReader();
                var jsonElement = JsonParser.parseReader(reader);
                var lootTableResult = LootTable.CODEC.parse(JsonOps.INSTANCE, jsonElement);
                reader.close();
                return lootTableResult.result().orElse(LootTable.EMPTY);
            }
        } catch (Exception e) {
            LootSparkle.LOGGER.error("Error loading legacy loot table", e);
        }
        return LootTable.EMPTY;
    }
}