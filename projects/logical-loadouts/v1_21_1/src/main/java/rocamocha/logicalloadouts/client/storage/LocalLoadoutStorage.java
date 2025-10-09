package rocamocha.logicalloadouts.client.storage;

import net.minecraft.item.ItemStack;
import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.data.Loadout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages client-side loadout storage that persists across worlds and servers.
 * Provides 3 global loadout slots plus unlimited regular local loadouts.
 */
public class LocalLoadoutStorage {
    
    private static final String LOADOUTS_DIR = "logical-loadouts";
    private static final String LOCAL_LOADOUTS_FILE = "local-loadouts.nbt";
    private static final String GLOBAL_LOADOUTS_FILE = "global-loadouts.nbt";
    private static final int GLOBAL_LOADOUT_SLOTS = 3;
    
    private static LocalLoadoutStorage instance;
    
    private final Map<UUID, Loadout> localLoadouts = new ConcurrentHashMap<>();
    private final Loadout[] globalLoadouts = new Loadout[GLOBAL_LOADOUT_SLOTS];
    private final Path storageDir;
    
    private LocalLoadoutStorage() {
        MinecraftClient client = MinecraftClient.getInstance();
        this.storageDir = client.runDirectory.toPath().resolve("config").resolve(LOADOUTS_DIR);
        
        try {
            Files.createDirectories(storageDir);
            loadFromDisk();
        } catch (IOException e) {
            LogicalLoadouts.LOGGER.error("Failed to create loadout storage directory", e);
        }
    }
    
    public static LocalLoadoutStorage getInstance() {
        if (instance == null) {
            instance = new LocalLoadoutStorage();
        }
        return instance;
    }
    
    /**
     * Save a loadout to local storage without copying (for updates)
     */
    public void saveLocalLoadoutWithoutCopy(Loadout loadout) {
        System.out.println("Updating loadout: " + loadout.getName() + " with " + countNonEmptyItems(loadout) + " items");
        localLoadouts.put(loadout.getId(), loadout);
        saveLocalLoadoutsToDisk();
        LogicalLoadouts.LOGGER.debug("Updated local loadout: {}", loadout.getName());
    }

    /**
     * Save a loadout to local storage
     */
    public void saveLocalLoadout(Loadout loadout) {
        System.out.println("Saving loadout: " + loadout.getName() + " with " + countNonEmptyItems(loadout) + " items");
        Loadout copyToStore = loadout.copy();
        System.out.println("Copy has: " + countNonEmptyItems(copyToStore) + " items");
        localLoadouts.put(copyToStore.getId(), copyToStore);
        saveLocalLoadoutsToDisk();
        LogicalLoadouts.LOGGER.debug("Saved local loadout: {}", copyToStore.getName());
    }
    
    /**
     * Save a loadout to a global slot (0-2)
     */
    public boolean saveGlobalLoadout(Loadout loadout, int slot) {
        if (slot < 0 || slot >= GLOBAL_LOADOUT_SLOTS) {
            LogicalLoadouts.LOGGER.warn("Invalid global loadout slot: {}", slot);
            return false;
        }
        
        globalLoadouts[slot] = loadout.copy();
        saveGlobalLoadoutsToDisk();
        LogicalLoadouts.LOGGER.debug("Saved global loadout '{}' to slot {}", loadout.getName(), slot);
        return true;
    }
    
    /**
     * Get all local loadouts (excluding global ones)
     */
    public List<Loadout> getLocalLoadouts() {
        return new ArrayList<>(localLoadouts.values());
    }
    
    /**
     * Fix broken UUID mappings in storage (temporary fix for existing loadouts)
     */
    public void fixBrokenStorage() {
        System.out.println("Fixing broken storage mappings...");
        Map<UUID, Loadout> fixedMap = new HashMap<>();
        
        for (Loadout loadout : localLoadouts.values()) {
            // Use the loadout's actual UUID as the key
            fixedMap.put(loadout.getId(), loadout);
            System.out.println("Fixed mapping: " + loadout.getName() + " (" + loadout.getId() + ")");
        }
        
        localLoadouts.clear();
        localLoadouts.putAll(fixedMap);
        saveLocalLoadoutsToDisk();
        System.out.println("Storage fix complete. Map now has " + localLoadouts.size() + " entries.");
    }
    
    private int countNonEmptyItems(Loadout loadout) {
        int count = 0;
        // Count hotbar items
        ItemStack[] hotbar = loadout.getHotbar();
        for (ItemStack item : hotbar) {
            if (item != null && !item.isEmpty()) count++;
        }
        // Count main inventory items
        ItemStack[] mainInventory = loadout.getMainInventory();
        for (ItemStack item : mainInventory) {
            if (item != null && !item.isEmpty()) count++;
        }
        // Count armor items
        ItemStack[] armor = loadout.getArmor();
        for (ItemStack item : armor) {
            if (item != null && !item.isEmpty()) count++;
        }
        // Count offhand items
        ItemStack[] offhand = loadout.getOffhand();
        for (ItemStack item : offhand) {
            if (item != null && !item.isEmpty()) count++;
        }
        return count;
    }
    
    /**
     * Get global loadout from slot (0-2)
     */
    public Loadout getGlobalLoadout(int slot) {
        if (slot < 0 || slot >= GLOBAL_LOADOUT_SLOTS) {
            return null;
        }
        return globalLoadouts[slot];
    }
    
    /**
     * Get all global loadouts
     */
    public Loadout[] getGlobalLoadouts() {
        return Arrays.copyOf(globalLoadouts, GLOBAL_LOADOUT_SLOTS);
    }
    
    /**
     * Delete a local loadout
     */
    public boolean deleteLocalLoadout(UUID loadoutId) {
        System.out.println("LocalLoadoutStorage.deleteLocalLoadout called for: " + loadoutId);
        System.out.println("Current localLoadouts map size: " + localLoadouts.size());
        System.out.println("Does map contain key? " + localLoadouts.containsKey(loadoutId));
        
        // Debug: Show all actual keys in the map
        System.out.println("Actual keys in map:");
        for (UUID key : localLoadouts.keySet()) {
            Loadout value = localLoadouts.get(key);
            System.out.println("  Key: " + key + " -> Value: " + value.getName() + " (" + value.getId() + ")");
        }
        
        Loadout removed = localLoadouts.remove(loadoutId);
        if (removed != null) {
            System.out.println("Successfully removed loadout: " + removed.getName());
            saveLocalLoadoutsToDisk();
            LogicalLoadouts.LOGGER.debug("Deleted local loadout: {}", removed.getName());
            return true;
        } else {
            System.out.println("Failed to remove loadout - not found in map");
        }
        return false;
    }
    
    /**
     * Clear a global loadout slot
     */
    public boolean clearGlobalLoadout(int slot) {
        if (slot < 0 || slot >= GLOBAL_LOADOUT_SLOTS) {
            return false;
        }
        
        if (globalLoadouts[slot] != null) {
            LogicalLoadouts.LOGGER.debug("Cleared global loadout slot {}: {}", slot, globalLoadouts[slot].getName());
            globalLoadouts[slot] = null;
            saveGlobalLoadoutsToDisk();
            return true;
        }
        return false;
    }
    
    /**
     * Get a loadout by ID from local storage
     */
    public Loadout getLocalLoadout(UUID loadoutId) {
        return localLoadouts.get(loadoutId);
    }
    
    /**
     * Check if a loadout exists in local storage
     */
    public boolean hasLocalLoadout(UUID loadoutId) {
        return localLoadouts.containsKey(loadoutId);
    }
    
    /**
     * Get total count of local loadouts
     */
    public int getLocalLoadoutCount() {
        return localLoadouts.size();
    }
    
    /**
     * Get count of occupied global slots
     */
    public int getOccupiedGlobalSlots() {
        int count = 0;
        for (Loadout loadout : globalLoadouts) {
            if (loadout != null) count++;
        }
        return count;
    }
    
    /**
     * Load loadouts from disk
     */
    private void loadFromDisk() {
        loadLocalLoadoutsFromDisk();
        loadGlobalLoadoutsFromDisk();
    }
    
    /**
     * Load local loadouts from disk
     */
    private void loadLocalLoadoutsFromDisk() {
        Path localFile = storageDir.resolve(LOCAL_LOADOUTS_FILE);
        if (!Files.exists(localFile)) {
            LogicalLoadouts.LOGGER.debug("No local loadouts file found, starting fresh");
            return;
        }
        
        try {
            NbtCompound data = NbtIo.readCompressed(localFile, NbtSizeTracker.ofUnlimitedBytes());
            
            if (data.contains("loadouts")) {
                NbtCompound loadoutsNbt = data.getCompound("loadouts");
                
                for (String key : loadoutsNbt.getKeys()) {
                    try {
                        UUID loadoutId = UUID.fromString(key);
                        NbtCompound loadoutNbt = loadoutsNbt.getCompound(key);
                        Loadout loadout = Loadout.fromNbt(loadoutNbt);
                        localLoadouts.put(loadoutId, loadout);
                        System.out.println("Loaded loadout: " + loadout.getName() + " with " + countNonEmptyItems(loadout) + " items");
                    } catch (Exception e) {
                        LogicalLoadouts.LOGGER.error("Failed to load local loadout: {}", key, e);
                    }
                }
            }
            
            LogicalLoadouts.LOGGER.info("Loaded {} local loadouts", localLoadouts.size());
        } catch (IOException e) {
            LogicalLoadouts.LOGGER.error("Failed to load local loadouts from disk", e);
        }
    }
    
    /**
     * Load global loadouts from disk
     */
    private void loadGlobalLoadoutsFromDisk() {
        Path globalFile = storageDir.resolve(GLOBAL_LOADOUTS_FILE);
        if (!Files.exists(globalFile)) {
            LogicalLoadouts.LOGGER.debug("No global loadouts file found, starting fresh");
            return;
        }
        
        try {
            NbtCompound data = NbtIo.readCompressed(globalFile, NbtSizeTracker.ofUnlimitedBytes());
            
            for (int i = 0; i < GLOBAL_LOADOUT_SLOTS; i++) {
                String slotKey = "slot_" + i;
                if (data.contains(slotKey)) {
                    try {
                        NbtCompound loadoutNbt = data.getCompound(slotKey);
                        globalLoadouts[i] = Loadout.fromNbt(loadoutNbt);
                    } catch (Exception e) {
                        LogicalLoadouts.LOGGER.error("Failed to load global loadout from slot {}", i, e);
                    }
                }
            }
            
            LogicalLoadouts.LOGGER.info("Loaded {} global loadouts", getOccupiedGlobalSlots());
        } catch (IOException e) {
            LogicalLoadouts.LOGGER.error("Failed to load global loadouts from disk", e);
        }
    }
    
    /**
     * Save local loadouts to disk
     */
    private void saveLocalLoadoutsToDisk() {
        try {
            NbtCompound data = new NbtCompound();
            NbtCompound loadoutsNbt = new NbtCompound();
            
            for (Map.Entry<UUID, Loadout> entry : localLoadouts.entrySet()) {
                loadoutsNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
            }
            
            data.put("loadouts", loadoutsNbt);
            data.putLong("lastSaved", System.currentTimeMillis());
            
            Path localFile = storageDir.resolve(LOCAL_LOADOUTS_FILE);
            NbtIo.writeCompressed(data, localFile);
            
            LogicalLoadouts.LOGGER.debug("Saved {} local loadouts to disk", localLoadouts.size());
        } catch (IOException e) {
            LogicalLoadouts.LOGGER.error("Failed to save local loadouts to disk", e);
        }
    }
    
    /**
     * Save global loadouts to disk
     */
    private void saveGlobalLoadoutsToDisk() {
        try {
            NbtCompound data = new NbtCompound();
            
            for (int i = 0; i < GLOBAL_LOADOUT_SLOTS; i++) {
                if (globalLoadouts[i] != null) {
                    data.put("slot_" + i, globalLoadouts[i].toNbt());
                }
            }
            
            data.putLong("lastSaved", System.currentTimeMillis());
            
            Path globalFile = storageDir.resolve(GLOBAL_LOADOUTS_FILE);
            NbtIo.writeCompressed(data, globalFile);
            
            LogicalLoadouts.LOGGER.debug("Saved {} global loadouts to disk", getOccupiedGlobalSlots());
        } catch (IOException e) {
            LogicalLoadouts.LOGGER.error("Failed to save global loadouts to disk", e);
        }
    }
    
    /**
     * Create a loadout from current player inventory
     */
    public Loadout createLoadoutFromInventory(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return null;
        }
        
        return Loadout.fromPlayer(client.player, name);
    }
    
    /**
     * Apply a loadout to the current player
     */
    public boolean applyLoadout(Loadout loadout) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }
        
        loadout.applyToPlayer(client.player);
        LogicalLoadouts.LOGGER.debug("Applied loadout '{}' locally", loadout.getName());
        return true;
    }
}