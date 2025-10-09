package rocamocha.logicalloadouts.client;

import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.client.storage.LocalLoadoutStorage;
import rocamocha.logicalloadouts.data.Loadout;
import rocamocha.logicalloadouts.network.packets.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hybrid client-side manager for loadout data.
 * Handles both local storage (client-only mode) and server synchronization.
 * Provides 3 global loadout slots that work across all worlds and servers.
 */
public class LoadoutClientManager {
    private static LoadoutClientManager instance;
    
    // Server loadouts (when connected to a server with loadout support)
    private final Map<UUID, Loadout> serverLoadouts = new LinkedHashMap<>();
    
    // Local storage for client-side and global loadouts
    private final LocalLoadoutStorage localStorage = LocalLoadoutStorage.getInstance();
    
    private final List<LoadoutUpdateListener> listeners = new CopyOnWriteArrayList<>();
    
    // Operation state tracking
    private boolean isConnectedToServer = false;
    private String lastOperationResult = null;
    private boolean lastOperationSuccess = false;
    
    public enum LoadoutType {
        GLOBAL,    // Cross-world/server persistent (slots 1-3)
        LOCAL,     // Client-side only, world-independent
        SERVER     // Server-managed (when available)
    }
    
    private LoadoutClientManager() {
        // Private constructor for singleton
    }
    
    public static LoadoutClientManager getInstance() {
        if (instance == null) {
            instance = new LoadoutClientManager();
        }
        return instance;
    }
    
    /**
     * Check if there are any loadouts available
     */
    public boolean hasLoadouts() {
        return !getAllLoadouts().isEmpty();
    }
    
    /**
     * Get the total number of loadouts
     */
    public int getLoadoutCount() {
        return getAllLoadouts().size();
    }
    
    /**
     * Get loadouts by type
     */
    public List<Loadout> getLoadouts(LoadoutType type) {
        switch (type) {
            case GLOBAL:
                List<Loadout> globalList = new ArrayList<>();
                for (Loadout loadout : localStorage.getGlobalLoadouts()) {
                    if (loadout != null) {
                        globalList.add(loadout);
                    }
                }
                return globalList;
            case LOCAL:
                return localStorage.getLocalLoadouts();
            case SERVER:
                return new ArrayList<>(serverLoadouts.values());
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Get all loadouts (global + local + server)
     */
    public List<Loadout> getAllLoadouts() {
        List<Loadout> allLoadouts = new ArrayList<>();
        
        // Add global loadouts first (these are always available)
        for (Loadout loadout : localStorage.getGlobalLoadouts()) {
            if (loadout != null) {
                allLoadouts.add(loadout);
            }
        }
        
        // Add local loadouts
        allLoadouts.addAll(localStorage.getLocalLoadouts());
        
        // Add server loadouts if connected
        if (isConnectedToServer) {
            allLoadouts.addAll(serverLoadouts.values());
        }
        
        return allLoadouts;
    }
    
    /**
     * Get global loadouts array (for quick access)
     */
    public Loadout[] getGlobalLoadouts() {
        return localStorage.getGlobalLoadouts();
    }
    
    /**
     * Apply a loadout by UUID
     */
    public boolean applyLoadout(UUID loadoutId) {
        System.out.println("DEBUG: applyLoadout called for UUID: " + loadoutId);
        
        // Check global loadouts first
        Loadout[] globalLoadouts = localStorage.getGlobalLoadouts();
        for (Loadout loadout : globalLoadouts) {
            if (loadout != null && loadout.getId().equals(loadoutId)) {
                System.out.println("DEBUG: Found in global loadouts, applying...");
                return applyGlobalLoadout(loadout);
            }
        }
        
        // Check local loadouts
        for (Loadout loadout : localStorage.getLocalLoadouts()) {
            if (loadout.getId().equals(loadoutId)) {
                System.out.println("DEBUG: Found in local loadouts, applying...");
                return applyLocalLoadout(loadout);
            }
        }
        
        // Check server loadouts if connected
        if (isConnectedToServer && serverLoadouts.containsKey(loadoutId)) {
            System.out.println("DEBUG: Found in server loadouts, applying...");
            return applyServerLoadout(loadoutId);
        }
        
        lastOperationSuccess = false;
        lastOperationResult = "Loadout not found: " + loadoutId;
        return false;
    }
    
    /**
     * Apply a global loadout directly
     */
    public boolean applyGlobalLoadout(Loadout loadout) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                System.out.println("DEBUG: applyGlobalLoadout - checking if server-side sync needed");
                
                // Check if we're in a world where server-side synchronization is needed
                if (client.getNetworkHandler() != null && client.getServer() != null) {
                    // Single-player or integrated server - need server-side sync for survival mode
                    System.out.println("DEBUG: applyGlobalLoadout - taking server path for sync, sending loadout data");
                    
                    // Send the global loadout data to server for proper synchronization
                    ClientPlayNetworking.send(new ApplyLocalLoadoutPayload(loadout));
                    
                    lastOperationSuccess = true;
                    lastOperationResult = "Sent global loadout data to server for sync: " + loadout.getName();
                } else {
                    // Pure client-side (shouldn't normally happen for global loadouts)
                    System.out.println("DEBUG: applyGlobalLoadout - taking client-only path");
                    loadout.applyToPlayer(client.player);
                    
                    lastOperationSuccess = true;
                    lastOperationResult = "Applied global loadout client-side: " + loadout.getName();
                }
                
                notifyListeners();
                return true;
            }
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to apply loadout: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to apply global loadout", e);
        }
        return false;
    }
    
    /**
     * Apply a global loadout by slot (1-3)
     */
    public boolean applyGlobalLoadout(int slot) {
        if (slot < 1 || slot > 3) {
            lastOperationSuccess = false;
            lastOperationResult = "Invalid global slot: " + slot + " (must be 1-3)";
            return false;
        }
        
        Loadout loadout = localStorage.getGlobalLoadouts()[slot - 1];
        if (loadout == null) {
            lastOperationSuccess = false;
            lastOperationResult = "Global slot " + slot + " is empty";
            return false;
        }
        
        return applyGlobalLoadout(loadout);
    }
    
    /**
     * Apply a local loadout
     */
    private boolean applyLocalLoadout(Loadout loadout) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                System.out.println("DEBUG: applyLocalLoadout - checking if server-side sync needed");
                
                // Check if we're in a world where server-side synchronization is needed
                if (client.getNetworkHandler() != null && client.getServer() != null) {
                    // Single-player or integrated server - need server-side sync for survival mode
                    System.out.println("DEBUG: applyLocalLoadout - taking server path for sync, sending loadout data");
                    
                    // Send the local loadout data to server for proper synchronization
                    ClientPlayNetworking.send(new ApplyLocalLoadoutPayload(loadout));
                    
                    lastOperationSuccess = true;
                    lastOperationResult = "Sent local loadout data to server for sync: " + loadout.getName();
                } else {
                    // Pure client-side (shouldn't normally happen for local loadouts)
                    System.out.println("DEBUG: applyLocalLoadout - taking client-only path");
                    loadout.applyToPlayer(client.player);
                    
                    lastOperationSuccess = true;
                    lastOperationResult = "Applied local loadout client-side: " + loadout.getName();
                }
                
                notifyListeners();
                return true;
            }
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to apply loadout: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to apply local loadout", e);
        }
        return false;
    }
    
    /**
     * Apply a server loadout
     */
    private boolean applyServerLoadout(UUID loadoutId) {
        try {
            // Send apply loadout request to server using modern CustomPayload system
            ClientPlayNetworking.send(new ApplyLoadoutPayload(loadoutId));
            
            lastOperationSuccess = true;
            lastOperationResult = "Sent apply request to server for loadout: " + loadoutId;
            return true;
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to send apply request: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to apply server loadout", e);
            return false;
        }
    }
    
    /**
     * Update an existing local loadout with current player inventory
     */
    public boolean updateLocalLoadout(UUID loadoutId) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                // Find the existing loadout
                Loadout existingLoadout = null;
                for (Loadout loadout : localStorage.getLocalLoadouts()) {
                    if (loadout.getId().equals(loadoutId)) {
                        existingLoadout = loadout;
                        break;
                    }
                }
                
                if (existingLoadout == null) {
                    lastOperationSuccess = false;
                    lastOperationResult = "Loadout not found for update: " + loadoutId;
                    return false;
                }
                
                // Create new loadout from current inventory with the same ID and name
                Loadout updatedLoadout = new Loadout(existingLoadout.getId(), existingLoadout.getName());
                updatedLoadout.copyFromPlayer(client.player);
                
                // Save the updated loadout
                localStorage.saveLocalLoadoutWithoutCopy(updatedLoadout);
                
                lastOperationSuccess = true;
                lastOperationResult = "Updated local loadout: " + existingLoadout.getName();
                
                notifyListeners();
                return true;
            }
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to update local loadout: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to update local loadout", e);
        }
        return false;
    }

    /**
     * Save current inventory to a global slot (1-3)
     */
    public boolean saveToGlobalSlot(int slot, String name) {
        if (slot < 1 || slot > 3) {
            lastOperationSuccess = false;
            lastOperationResult = "Invalid global slot: " + slot + " (must be 1-3)";
            return false;
        }
        
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                Loadout loadout = Loadout.fromPlayer(client.player, name);
                localStorage.saveGlobalLoadout(loadout, slot - 1);
                
                lastOperationSuccess = true;
                lastOperationResult = "Saved to global slot " + slot + ": " + name;
                
                notifyListeners();
                return true;
            }
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to save to global slot: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to save to global slot", e);
        }
        return false;
    }
    
    /**
     * Save current inventory to local storage
     */
    public boolean saveLocalLoadout(String name) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                Loadout loadout = Loadout.fromPlayer(client.player, name);
                localStorage.saveLocalLoadout(loadout);
                
                lastOperationSuccess = true;
                lastOperationResult = "Saved local loadout: " + name;
                
                notifyListeners();
                return true;
            }
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to save local loadout: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to save local loadout", e);
        }
        return false;
    }
    
    /**
     * Create a new loadout (hybrid: local if offline, server if online)
     */
    public boolean createLoadout(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean hasIntegratedServer = client.getServer() != null;
        boolean compatibleServer = isConnectedToCompatibleServer();
        
        System.out.println("CreateLoadout: isConnectedToServer = " + isConnectedToServer);
        System.out.println("CreateLoadout: hasIntegratedServer = " + hasIntegratedServer);
        System.out.println("CreateLoadout: isConnectedToCompatibleServer = " + compatibleServer);
        
        if (compatibleServer) {
            System.out.println("Taking server path...");
            return createServerLoadout(name);
        } else {
            System.out.println("Taking local path...");
            return saveLocalLoadout(name);
        }
    }
    
    /**
     * Create a server loadout
     */
    private boolean createServerLoadout(String name) {
        try {
            // Send create loadout request to server using modern CustomPayload system
            System.out.println("CLIENT: Sending CreateLoadoutPayload to server for: " + name);
            ClientPlayNetworking.send(new CreateLoadoutPayload(name));
            
            lastOperationSuccess = true;
            lastOperationResult = "Sent create request to server for loadout: " + name;
            return true;
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to send create request: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to create server loadout", e);
            return false;
        }
    }
    
    /**
     * Delete a loadout by UUID
     */
    public boolean deleteLoadout(UUID loadoutId) {
        System.out.println("DeleteLoadout called for: " + loadoutId);
        
        // Debug: Show all available loadouts
        System.out.println("Available local loadouts:");
        for (Loadout loadout : localStorage.getLocalLoadouts()) {
            System.out.println("  - " + loadout.getName() + " (" + loadout.getId() + ")");
        }
        System.out.println("Available global loadouts:");
        Loadout[] globalLoadouts = localStorage.getGlobalLoadouts();
        for (int i = 0; i < globalLoadouts.length; i++) {
            if (globalLoadouts[i] != null) {
                System.out.println("  - Global slot " + i + ": " + globalLoadouts[i].getName() + " (" + globalLoadouts[i].getId() + ")");
            }
        }
        
        // Check global loadouts first
        for (int i = 0; i < globalLoadouts.length; i++) {
            if (globalLoadouts[i] != null && globalLoadouts[i].getId().equals(loadoutId)) {
                System.out.println("Found loadout in global slot " + i + ", clearing it...");
                if (localStorage.clearGlobalLoadout(i)) {
                    System.out.println("Successfully cleared global slot " + i);
                    lastOperationSuccess = true;
                    lastOperationResult = "Deleted global loadout from slot " + i;
                    notifyListeners();
                    return true;
                }
            }
        }
        
        // Check local loadouts
        System.out.println("Calling localStorage.deleteLocalLoadout...");
        if (localStorage.deleteLocalLoadout(loadoutId)) {
            System.out.println("Successfully deleted from local storage");
            lastOperationSuccess = true;
            lastOperationResult = "Deleted local loadout";
            notifyListeners();
            return true;
        } else {
            System.out.println("localStorage.deleteLocalLoadout returned false");
        }
        
        // Check server loadouts if connected
        if (isConnectedToServer && serverLoadouts.containsKey(loadoutId)) {
            System.out.println("Attempting to delete from server...");
            return deleteServerLoadout(loadoutId);
        }
        
        System.out.println("Loadout not found in local or server storage");
        System.out.println("Attempting to fix broken storage and retry...");
        localStorage.fixBrokenStorage();
        
        // Retry after fixing storage
        if (localStorage.deleteLocalLoadout(loadoutId)) {
            System.out.println("Successfully deleted after storage fix");
            lastOperationSuccess = true;
            lastOperationResult = "Deleted local loadout after fixing storage";
            notifyListeners();
            return true;
        }
        
        lastOperationSuccess = false;
        lastOperationResult = "Loadout not found for deletion: " + loadoutId;
        return false;
    }
    
    /**
     * Delete a server loadout
     */
    private boolean deleteServerLoadout(UUID loadoutId) {
        try {
            // Send delete loadout request to server using modern CustomPayload system
            ClientPlayNetworking.send(new DeleteLoadoutPayload(loadoutId));
            
            lastOperationSuccess = true;
            lastOperationResult = "Sent delete request to server for loadout: " + loadoutId;
            return true;
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to send delete request: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to delete server loadout", e);
            return false;
        }
    }
    
    /**
     * Clear a global slot
     */
    public boolean clearGlobalSlot(int slot) {
        if (slot < 1 || slot > 3) {
            lastOperationSuccess = false;
            lastOperationResult = "Invalid global slot: " + slot + " (must be 1-3)";
            return false;
        }
        
        localStorage.clearGlobalLoadout(slot - 1);
        
        lastOperationSuccess = true;
        lastOperationResult = "Cleared global slot " + slot;
        
        notifyListeners();
        return true;
    }
    
    /**
     * Get operation mode description
     */
    public String getOperationMode() {
        if (isConnectedToServer) {
            return "Server Mode (with Global Slots)";
        } else {
            return "Local Mode (with Global Slots)";
        }
    }
    
    /**
     * Check if connected to server with loadout support
     */
    public boolean isConnectedToServer() {
        return isConnectedToServer;
    }
    
    /**
     * Set server connection state
     */
    public void setConnectedToServer(boolean connected) {
        this.isConnectedToServer = connected;
        
        // In single-player mode, we're always "connected" to the integrated server
        MinecraftClient client = MinecraftClient.getInstance();
        boolean actuallyConnected = connected || (client.getServer() != null);
        
        if (!actuallyConnected) {
            serverLoadouts.clear();
        } else {
            // When connecting to a server (or in single-player), request loadouts
            requestLoadoutsFromServer();
        }
        notifyListeners();
    }
    
    /**
     * Check if connected to a server that supports loadouts
     */
    public boolean isConnectedToCompatibleServer() {
        // Always return true to force networking even in single-player
        // Single-player has an integrated server that supports our mod
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getServer() != null || isConnectedToServer;
    }
    
    /**
     * Handle server loadout data update
     */
    public void updateServerLoadouts(Map<UUID, Loadout> loadouts) {
        this.serverLoadouts.clear();
        this.serverLoadouts.putAll(loadouts);
        notifyListeners();
    }
    
    /**
     * Add a listener for loadout updates
     */
    public void addListener(LoadoutUpdateListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener
     */
    public void removeListener(LoadoutUpdateListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify all listeners of changes
     */
    private void notifyListeners() {
        for (LoadoutUpdateListener listener : listeners) {
            try {
                listener.onLoadoutsUpdated();
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error notifying loadout listener", e);
            }
        }
    }
    
    /**
     * Get last operation result
     */
    public String getLastOperationResult() {
        return lastOperationResult;
    }
    
    /**
     * Check if last operation was successful
     */
    public boolean wasLastOperationSuccessful() {
        return lastOperationSuccess;
    }
    
    /**
     * Handle loadouts synchronization from server
     */
    public void handleServerLoadoutsSync(List<Loadout> serverLoadouts) {
        this.serverLoadouts.clear();
        
        // Convert list to map with UUID -> Loadout mapping
        for (Loadout loadout : serverLoadouts) {
            this.serverLoadouts.put(loadout.getId(), loadout);
        }
        
        lastOperationSuccess = true;
        lastOperationResult = "Synchronized " + serverLoadouts.size() + " loadouts from server";
        
        notifyListeners();
    }
    
    /**
     * Handle operation result from server
     */
    public void handleServerOperationResult(String operation, boolean success, String message) {
        lastOperationSuccess = success;
        lastOperationResult = message;
        
        LogicalLoadouts.LOGGER.debug("Server operation {}: {} - {}", operation, success ? "SUCCESS" : "FAILED", message);
        
        // If operation was successful and affects loadout list, request refresh
        if (success && (operation.equals("create") || operation.equals("delete") || operation.equals("update"))) {
            requestLoadoutsFromServer();
        }
        
        notifyListeners();
    }
    
    /**
     * Handle loadout applied notification from server
     */
    public void handleLoadoutApplied(String loadoutName) {
        lastOperationSuccess = true;
        lastOperationResult = "Applied loadout: " + loadoutName;
        
        LogicalLoadouts.LOGGER.debug("Server applied loadout: {}", loadoutName);
        
        notifyListeners();
    }
    
    /**
     * Request loadouts from server (used when connecting or refreshing)
     */
    public void requestLoadoutsFromServer() {
        if (!isConnectedToCompatibleServer()) {
            return;
        }
        
        try {
            // Send request for all loadouts from server
            ClientPlayNetworking.send(new RequestLoadoutsPayload());
            LogicalLoadouts.LOGGER.debug("Requested loadouts from server");
        } catch (Exception e) {
            LogicalLoadouts.LOGGER.error("Failed to request loadouts from server", e);
        }
    }
    
    /**
     * Interface for loadout update listeners
     */
    public interface LoadoutUpdateListener {
        void onLoadoutsUpdated();
    }
}