package rocamocha.logicalloadouts.server;

import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.data.Loadout;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side manager for player loadouts with persistent storage and validation.
 * Handles all server-side operations including permissions, limits, and data persistence.
 */
public class LoadoutManager {
    private static final String LOADOUTS_DIRECTORY = "logical-loadouts";
    private static final String LOADOUTS_FILE_EXTENSION = ".nbt";
    private static final int DEFAULT_MAX_LOADOUTS_PER_PLAYER = Integer.MAX_VALUE;
    
    private final MinecraftServer server;
    private final Path loadoutsPath;
    
    // In-memory cache for active players (UUID -> Map of loadout ID -> Loadout)
    private final Map<UUID, Map<UUID, Loadout>> playerLoadouts = new ConcurrentHashMap<>();
    
    // In-memory cache for server-shared loadouts (available to all players)
    private final Map<UUID, Loadout> serverSharedLoadouts = new ConcurrentHashMap<>();
    
    // Path to server-shared loadouts directory
    private final Path serverLoadoutsPath;
    
    // Configuration
    private int maxLoadoutsPerPlayer = DEFAULT_MAX_LOADOUTS_PER_PLAYER;
    private final Set<String> bannedItems = new HashSet<>(); // Item IDs that can't be stored in loadouts
    
    public LoadoutManager(MinecraftServer server) {
        this.server = server;
        this.loadoutsPath = server.getSavePath(WorldSavePath.ROOT).resolve(LOADOUTS_DIRECTORY);
        this.serverLoadoutsPath = loadoutsPath.resolve("server");
        
        // Ensure the main loadouts directory exists
        try {
            Files.createDirectories(loadoutsPath);
            LogicalLoadouts.LOGGER.info("Created/verified loadouts directory: {}", loadoutsPath);
        } catch (IOException e) {
            LogicalLoadouts.LOGGER.error("Failed to create loadouts directory: {}", loadoutsPath, e);
            throw new RuntimeException("Could not create loadouts directory", e);
        }
        
        // Ensure the server loadouts directory exists
        try {
            Files.createDirectories(serverLoadoutsPath);
            LogicalLoadouts.LOGGER.info("Created/verified server loadouts directory: {}", serverLoadoutsPath);
        } catch (IOException e) {
            LogicalLoadouts.LOGGER.error("Failed to create server loadouts directory: {}", serverLoadoutsPath, e);
            throw new RuntimeException("Could not create server loadouts directory", e);
        }
        
        // Load server-shared loadouts
        loadServerSharedLoadouts();
        
        LogicalLoadouts.LOGGER.info("LoadoutManager initialized with storage at: {}", loadoutsPath);
    }
    
    /**
     * Load server-shared loadouts from the logical-loadouts/server directory
     */
    private void loadServerSharedLoadouts() {
        serverSharedLoadouts.clear();
        
        if (!Files.exists(serverLoadoutsPath)) {
            LogicalLoadouts.LOGGER.warn("Server loadouts directory does not exist (this should not happen): {}", serverLoadoutsPath);
            return;
        }
        
        if (!Files.isDirectory(serverLoadoutsPath)) {
            LogicalLoadouts.LOGGER.error("Server loadouts path exists but is not a directory: {}", serverLoadoutsPath);
            return;
        }
        
        try {
            Files.walk(serverLoadoutsPath)
                .filter(path -> path.toString().endsWith(LOADOUTS_FILE_EXTENSION))
                .forEach(path -> {
                    try {
                        NbtCompound nbt = NbtIo.readCompressed(path, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
                        Loadout loadout = Loadout.fromNbt(nbt);
                        
                        if (loadout.isValid()) {
                            serverSharedLoadouts.put(loadout.getId(), loadout);
                            LogicalLoadouts.LOGGER.debug("Loaded server-shared loadout: {}", loadout.getName());
                        } else {
                            LogicalLoadouts.LOGGER.warn("Invalid server-shared loadout found: {}", path.getFileName());
                        }
                    } catch (Exception e) {
                        LogicalLoadouts.LOGGER.error("Failed to load server-shared loadout: {}", path.getFileName(), e);
                    }
                });
                
            LogicalLoadouts.LOGGER.info("Loaded {} server-shared loadouts from {}", serverSharedLoadouts.size(), serverLoadoutsPath);
        } catch (Exception e) {
            LogicalLoadouts.LOGGER.error("Failed to load server-shared loadouts from {}", serverLoadoutsPath, e);
        }
    }
    
    /**
     * Reload server-shared loadouts from disk
     */
    public void reloadServerSharedLoadouts() {
        LogicalLoadouts.LOGGER.info("Reloading server-shared loadouts...");
        loadServerSharedLoadouts();
        LogicalLoadouts.LOGGER.info("Reloaded {} server-shared loadouts", serverSharedLoadouts.size());
    }
    
    /**
     * Load a player's loadouts from disk when they join
     */
    public void loadPlayerData(UUID playerUuid) {
        if (playerLoadouts.containsKey(playerUuid)) {
            return; // Already loaded
        }
        
        Map<UUID, Loadout> loadouts = new HashMap<>();
        Path playerFile = getPlayerLoadoutsFile(playerUuid);
        
        if (Files.exists(playerFile)) {
            try {
                NbtCompound playerData = NbtIo.readCompressed(playerFile, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
                
                if (playerData.contains("loadouts")) {
                    NbtCompound loadoutsNbt = playerData.getCompound("loadouts");
                    
                    for (String key : loadoutsNbt.getKeys()) {
                        try {
                            UUID loadoutId = UUID.fromString(key);
                            Loadout loadout = Loadout.fromNbt(loadoutsNbt.getCompound(key));
                            
                            if (loadout.isValid()) {
                                loadouts.put(loadoutId, loadout);
                            } else {
                                LogicalLoadouts.LOGGER.warn("Invalid loadout found for player {}: {}", playerUuid, loadout.getName());
                            }
                        } catch (Exception e) {
                            LogicalLoadouts.LOGGER.error("Failed to load loadout for player " + playerUuid, e);
                        }
                    }
                }
                
                LogicalLoadouts.LOGGER.debug("Loaded {} loadouts for player {}", loadouts.size(), playerUuid);
            } catch (IOException e) {
                LogicalLoadouts.LOGGER.error("Failed to read loadouts file for player " + playerUuid, e);
            }
        }
        
        playerLoadouts.put(playerUuid, loadouts);
    }
    
    /**
     * Save a player's loadouts to disk
     */
    public void savePlayerData(UUID playerUuid) {
        Map<UUID, Loadout> loadouts = playerLoadouts.get(playerUuid);
        if (loadouts == null || loadouts.isEmpty()) {
            return;
        }
        
        try {
            NbtCompound playerData = new NbtCompound();
            NbtCompound loadoutsNbt = new NbtCompound();
            
            for (Map.Entry<UUID, Loadout> entry : loadouts.entrySet()) {
                loadoutsNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
            }
            
            playerData.put("loadouts", loadoutsNbt);
            playerData.putLong("lastSaved", System.currentTimeMillis());
            
            Path playerFile = getPlayerLoadoutsFile(playerUuid);
            Files.createDirectories(playerFile.getParent());
            NbtIo.writeCompressed(playerData, playerFile);
            
            LogicalLoadouts.LOGGER.debug("Saved {} loadouts for player {}", loadouts.size(), playerUuid);
        } catch (IOException e) {
            LogicalLoadouts.LOGGER.error("Failed to save loadouts for player " + playerUuid, e);
        }
    }
    
    /**
     * Unload a player's data when they leave (saves to disk)
     */
    public void unloadPlayerData(UUID playerUuid) {
        savePlayerData(playerUuid);
        playerLoadouts.remove(playerUuid);
        LogicalLoadouts.LOGGER.debug("Unloaded data for player {}", playerUuid);
    }
    
    /**
     * Create a new loadout for a player from their current inventory
     */
    public LoadoutOperationResult createLoadout(net.minecraft.entity.player.PlayerEntity player, String name) {
        UUID playerUuid = player.getUuid();
        Map<UUID, Loadout> loadouts = playerLoadouts.get(playerUuid);
        if (loadouts == null) {
            return LoadoutOperationResult.error("Player data not loaded");
        }
        
        String error = getCreateLoadoutError(loadouts, name);
        if (error != null) {
            return LoadoutOperationResult.error(error);
        }
        
        try {
            // Create loadout from player's current inventory
            Loadout newLoadout = Loadout.fromPlayer(player, name);
            
            // Check if the loadout would be empty
            if (isLoadoutEmpty(newLoadout)) {
                return LoadoutOperationResult.error("Cannot create empty loadout - no items found in inventory");
            }
            
            loadouts.put(newLoadout.getId(), newLoadout);
            
            LogicalLoadouts.LOGGER.debug("Created loadout '{}' for player {} with current inventory", name, playerUuid);
            return LoadoutOperationResult.success(newLoadout);
        } catch (IllegalArgumentException e) {
            return LoadoutOperationResult.error(e.getMessage());
        }
    }
    
    /**
     * Check if a loadout can be created and return error message if not
     */
    private String getCreateLoadoutError(Map<UUID, Loadout> loadouts, String name) {
        if (loadouts.size() >= maxLoadoutsPerPlayer) {
            return "Maximum number of loadouts reached (" + maxLoadoutsPerPlayer + ")";
        }
        
        for (Loadout existingLoadout : loadouts.values()) {
            if (existingLoadout.getName().equalsIgnoreCase(name)) {
                return "A loadout with that name already exists";
            }
        }
        
        return null;
    }
    /**
     * Create a new loadout for a player from provided loadout data
     */
    public LoadoutOperationResult createLoadoutFromData(net.minecraft.entity.player.PlayerEntity player, Loadout loadout) {
        UUID playerUuid = player.getUuid();
        Map<UUID, Loadout> loadouts = playerLoadouts.get(playerUuid);
        if (loadouts == null) {
            return LoadoutOperationResult.error("Player data not loaded");
        }
        
        String error = getCreateLoadoutError(loadouts, loadout.getName());
        if (error != null) {
            return LoadoutOperationResult.error(error);
        }
        
        try {
            // Validate the provided loadout
            if (!loadout.isValid()) {
                return LoadoutOperationResult.error("Invalid loadout data");
            }
            
            // Check if the loadout would be empty
            if (isLoadoutEmpty(loadout)) {
                return LoadoutOperationResult.error("Cannot create empty loadout - no items found");
            }
            
            // Additional server-side validation
            if (!validateLoadoutForServer(loadout)) {
                return LoadoutOperationResult.error("Loadout contains forbidden items");
            }
            
            // Create a new loadout with the provided data but generate a new ID
            Loadout newLoadout = new Loadout(loadout.getName());
            // Copy all the inventory data using setter methods
            for (int i = 0; i < Loadout.HOTBAR_SIZE; i++) {
                newLoadout.setHotbarSlot(i, loadout.getHotbar()[i]);
            }
            for (int i = 0; i < Loadout.MAIN_INVENTORY_SIZE; i++) {
                newLoadout.setMainInventorySlot(i, loadout.getMainInventory()[i]);
            }
            for (int i = 0; i < Loadout.ARMOR_SIZE; i++) {
                newLoadout.setArmorSlot(i, loadout.getArmor()[i]);
            }
            for (int i = 0; i < Loadout.OFFHAND_SIZE; i++) {
                newLoadout.setOffhandSlot(i, loadout.getOffhand()[i]);
            }
            
            loadouts.put(newLoadout.getId(), newLoadout);
            
            LogicalLoadouts.LOGGER.debug("Created loadout '{}' for player {} from provided data", loadout.getName(), playerUuid);
            return LoadoutOperationResult.success(newLoadout);
        } catch (IllegalArgumentException e) {
            return LoadoutOperationResult.error(e.getMessage());
        }
    }
    
    /**
     * Create a new loadout for a player (deprecated - use createLoadout(PlayerEntity, String) instead)
     */
    // public LoadoutOperationResult createLoadout(UUID playerUuid, String name) {
    //     Map<UUID, Loadout> loadouts = playerLoadouts.get(playerUuid);
    //     if (loadouts == null) {
    //         return LoadoutOperationResult.error("Player data not loaded");
    //     }
        
    //     // Check limits
    //     if (loadouts.size() >= maxLoadoutsPerPlayer) {
    //         return LoadoutOperationResult.error("Maximum number of loadouts reached (" + maxLoadoutsPerPlayer + ")");
    //     }
        
    //     // Check for duplicate names
    //     for (Loadout existingLoadout : loadouts.values()) {
    //         if (existingLoadout.getName().equalsIgnoreCase(name)) {
    //             return LoadoutOperationResult.error("A loadout with that name already exists");
    //         }
    //     }
        
    //     // This method is deprecated - use createLoadout(PlayerEntity, String) instead
    //     return LoadoutOperationResult.error("createLoadout requires player entity - use createLoadout(PlayerEntity, String) instead");
    // }
    
    /**
     * Delete a loadout
     */
    public LoadoutOperationResult deleteLoadout(UUID playerUuid, UUID loadoutId) {
        Map<UUID, Loadout> loadouts = playerLoadouts.get(playerUuid);
        if (loadouts == null) {
            return LoadoutOperationResult.error("Player data not loaded");
        }
        
        Loadout removed = loadouts.remove(loadoutId);
        if (removed == null) {
            return LoadoutOperationResult.error("Loadout not found");
        }
        
        LogicalLoadouts.LOGGER.debug("Deleted loadout '{}' for player {}", removed.getName(), playerUuid);
        return LoadoutOperationResult.success(removed);
    }
    
    /**
     * Get a specific loadout
     */
    public LoadoutOperationResult getLoadout(UUID playerUuid, UUID loadoutId) {
        Map<UUID, Loadout> loadouts = playerLoadouts.get(playerUuid);
        if (loadouts == null) {
            return LoadoutOperationResult.error("Player data not loaded");
        }
        
        Loadout loadout = loadouts.get(loadoutId);
        if (loadout == null) {
            return LoadoutOperationResult.error("Loadout not found");
        }
        
        return LoadoutOperationResult.success(loadout);
    }
    
    /**
     * Get all loadouts for a player (includes both personal and server-shared loadouts)
     */
    public List<Loadout> getPlayerLoadouts(UUID playerUuid) {
        List<Loadout> allLoadouts = new ArrayList<>();
        
        // Add server-shared loadouts first (available to all players)
        allLoadouts.addAll(serverSharedLoadouts.values());
        
        // Add player's personal loadouts
        Map<UUID, Loadout> playerLoadouts = this.playerLoadouts.get(playerUuid);
        if (playerLoadouts != null) {
            allLoadouts.addAll(playerLoadouts.values());
        }
        
        return allLoadouts;
    }
    
    /**
     * Get only personal loadouts for a player
     */
    public List<Loadout> getPersonalLoadouts(UUID playerUuid) {
        Map<UUID, Loadout> playerLoadouts = this.playerLoadouts.get(playerUuid);
        if (playerLoadouts == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(playerLoadouts.values());
    }
    
    /**
     * Get server-shared loadouts (available to all players)
     */
    public List<Loadout> getServerSharedLoadouts() {
        return new ArrayList<>(serverSharedLoadouts.values());
    }
    
    /**
     * Update an existing loadout
     */
    public LoadoutOperationResult updateLoadout(UUID playerUuid, Loadout loadout) {
        Map<UUID, Loadout> loadouts = playerLoadouts.get(playerUuid);
        if (loadouts == null) {
            return LoadoutOperationResult.error("Player data not loaded");
        }
        
        if (!loadouts.containsKey(loadout.getId())) {
            return LoadoutOperationResult.error("Loadout not found");
        }
        
        if (!loadout.isValid()) {
            return LoadoutOperationResult.error("Invalid loadout data");
        }
        
        // Additional server-side validation
        if (!validateLoadoutForServer(loadout)) {
            return LoadoutOperationResult.error("Loadout contains forbidden items");
        }
        
        loadouts.put(loadout.getId(), loadout);
        LogicalLoadouts.LOGGER.debug("Updated loadout '{}' for player {}", loadout.getName(), playerUuid);
        return LoadoutOperationResult.success(loadout);
    }
    
    /**
     * Server-side validation for loadouts (checks banned items, etc.)
     */
    private boolean validateLoadoutForServer(Loadout loadout) {
        // Check for banned items
        net.minecraft.item.ItemStack[][] allArrays = {loadout.getHotbar(), loadout.getMainInventory(), loadout.getArmor(), loadout.getOffhand()};
        for (net.minecraft.item.ItemStack[] array : allArrays) {
            if (containsBannedItems(array)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if a loadout is completely empty (all sections are empty)
     */
    private boolean isLoadoutEmpty(Loadout loadout) {
        net.minecraft.item.ItemStack[][] allArrays = {loadout.getHotbar(), loadout.getMainInventory(), loadout.getArmor(), loadout.getOffhand()};
        for (net.minecraft.item.ItemStack[] array : allArrays) {
            for (net.minecraft.item.ItemStack item : array) {
                if (!item.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean containsBannedItems(net.minecraft.item.ItemStack[] items) {
        for (net.minecraft.item.ItemStack item : items) {
            if (!item.isEmpty()) {
                String itemId = net.minecraft.registry.Registries.ITEM.getId(item.getItem()).toString();
                if (bannedItems.contains(itemId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if a player has permission for a certain operation
     */
    public boolean hasPermission(ServerPlayerEntity player, String permission) {
        // Basic implementation - can be extended with permission mods
        return switch (permission) {
            case "logical-loadouts.create" -> true; // All players can create loadouts
            case "logical-loadouts.unlimited" -> player.hasPermissionLevel(2); // Ops can exceed limits
            case "logical-loadouts.admin" -> player.hasPermissionLevel(2); // Admin commands
            default -> false;
        };
    }
    
    /**
     * Get the maximum number of loadouts for a player (respects permissions)
     */
    public int getMaxLoadouts(ServerPlayerEntity player) {
        if (hasPermission(player, "logical-loadouts.unlimited")) {
            return Integer.MAX_VALUE;
        }
        return maxLoadoutsPerPlayer;
    }
    
    private Path getPlayerLoadoutsFile(UUID playerUuid) {
        return loadoutsPath.resolve(playerUuid.toString() + LOADOUTS_FILE_EXTENSION);
    }
    
    // Configuration methods
    public void setMaxLoadoutsPerPlayer(int max) {
        this.maxLoadoutsPerPlayer = Math.max(1, max);
    }
    
    public int getMaxLoadoutsPerPlayer() {
        return maxLoadoutsPerPlayer;
    }
    
    public void addBannedItem(String itemId) {
        bannedItems.add(itemId);
    }
    
    public void removeBannedItem(String itemId) {
        bannedItems.remove(itemId);
    }
    
    public Set<String> getBannedItems() {
        return new HashSet<>(bannedItems);
    }
    
    /**
     * Save all loaded player data (called on server shutdown)
     */
    public void saveAll() {
        LogicalLoadouts.LOGGER.info("Saving all loadout data...");
        for (UUID playerUuid : playerLoadouts.keySet()) {
            savePlayerData(playerUuid);
        }
        LogicalLoadouts.LOGGER.info("All loadout data saved");
    }
    
    /**
     * Result wrapper for loadout operations
     */
    public static class LoadoutOperationResult {
        private final boolean success;
        private final String message;
        private final Loadout loadout;
        
        private LoadoutOperationResult(boolean success, String message, Loadout loadout) {
            this.success = success;
            this.message = message;
            this.loadout = loadout;
        }
        
        public static LoadoutOperationResult success(Loadout loadout) {
            return new LoadoutOperationResult(true, null, loadout);
        }
        
        public static LoadoutOperationResult error(String message) {
            return new LoadoutOperationResult(false, message, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Loadout getLoadout() { return loadout; }
    }
}