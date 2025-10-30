package rocamocha.logicalloadouts.client;

import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.client.storage.LocalLoadoutStorage;
import rocamocha.logicalloadouts.data.Loadout;
import rocamocha.logicalloadouts.network.packets.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.nio.file.Path;

/**
 * Hybrid client-side manager for loadout data.
 * Handles both local storage (client-only mode) and server synchronization.
 * Provides 3 global loadout slots that work across all worlds and servers.
 */
public class LoadoutClientManager {
    private static LoadoutClientManager instance;
    
    // Server loadouts (when connected to a server with loadout support)
    private final Map<UUID, Loadout> serverLoadouts = new LinkedHashMap<>();
    private final Map<UUID, Loadout> serverSharedLoadouts = new LinkedHashMap<>();
    
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
                return new ArrayList<>(serverSharedLoadouts.values());
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Get personal loadouts (global + local + personal server loadouts)
     */
    public List<Loadout> getPersonalLoadouts() {
        List<Loadout> personalLoadouts = new ArrayList<>();
        
        // Add global loadouts first (these are always available)
        for (Loadout loadout : localStorage.getGlobalLoadouts()) {
            if (loadout != null) {
                personalLoadouts.add(loadout);
            }
        }
        
        // Add local loadouts
        personalLoadouts.addAll(localStorage.getLocalLoadouts());
        
        // Add personal server loadouts if connected
        if (isConnectedToServer) {
            personalLoadouts.addAll(serverLoadouts.values());
        }
        
        return personalLoadouts;
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
            allLoadouts.addAll(serverSharedLoadouts.values());
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
            Loadout loadout = serverLoadouts.get(loadoutId);
            return applyLocalLoadout(loadout);  // Use same approach as local/global loadouts
        }
        
        // Check server-shared loadouts if connected
        if (isConnectedToServer && serverSharedLoadouts.containsKey(loadoutId)) {
            System.out.println("DEBUG: Found in server-shared loadouts, applying...");
            Loadout loadout = serverSharedLoadouts.get(loadoutId);
            return applyLocalLoadout(loadout);  // Use same approach as local/global loadouts
        }
        
        lastOperationSuccess = false;
        lastOperationResult = "Loadout not found: " + loadoutId;
        return false;
    }
    
    /**
     * Apply a loadout with server synchronization if connected
     */
    private boolean applyLoadoutWithSync(Loadout loadout, String syncMessage) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                if (client.getNetworkHandler() != null) {
                    // Connected to server - send for sync
                    ClientPlayNetworking.send(new ApplyLocalLoadoutPayload(loadout, false)); // Global loadouts are not consumed
                    lastOperationSuccess = true;
                    lastOperationResult = syncMessage;
                } else {
                    // Not connected - apply locally
                    loadout.applyToPlayer(client.player);
                    lastOperationSuccess = true;
                    lastOperationResult = syncMessage.replace("Sent", "Applied").replace("data to server for sync", "locally");
                }
                notifyListeners();
                return true;
            }
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to apply loadout: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to apply loadout", e);
        }
        return false;
    }
    
    /**
     * Apply a global loadout directly
     */
    public boolean applyGlobalLoadout(Loadout loadout) {
        return applyLoadoutWithSync(loadout, "Sent global loadout data to server for sync: " + loadout.getName());
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
        return applyLoadoutWithSync(loadout, "Sent local loadout data to server for sync: " + loadout.getName());
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
     * Update a loadout object (hybrid: local or server)
     */
    public boolean updateLoadout(Loadout loadout) {
        try {
            // Check if this is a server loadout
            if (isConnectedToCompatibleServer() && (serverLoadouts.containsKey(loadout.getId()) || serverSharedLoadouts.containsKey(loadout.getId()))) {
                // Send update request to server
                ClientPlayNetworking.send(new UpdateLoadoutPayload(loadout.toNbt()));
                lastOperationSuccess = true;
                lastOperationResult = "Sent update request to server for loadout: " + loadout.getName();
                return true;
            } else {
                // Local loadout - save directly
                localStorage.saveLocalLoadoutWithoutCopy(loadout);
                lastOperationSuccess = true;
                lastOperationResult = "Updated local loadout: " + loadout.getName();
                notifyListeners();
                return true;
            }
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to update loadout: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to update loadout", e);
            return false;
        }
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
     * Create a new loadout from provided data (hybrid: local if offline, server if online)
     */
    public boolean createLoadoutFromData(Loadout loadout) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean hasIntegratedServer = client.getServer() != null;
        boolean compatibleServer = isConnectedToCompatibleServer();
        
        System.out.println("CreateLoadoutFromData: isConnectedToServer = " + isConnectedToServer);
        System.out.println("CreateLoadoutFromData: hasIntegratedServer = " + hasIntegratedServer);
        System.out.println("CreateLoadoutFromData: isConnectedToCompatibleServer = " + compatibleServer);
        
        if (compatibleServer) {
            System.out.println("Taking server path...");
            return createServerLoadoutFromData(loadout);
        } else {
            System.out.println("Taking local path...");
            return saveLocalLoadoutFromData(loadout);
        }
    }
    
    /**
     * Create a server loadout from provided data
     */
    private boolean createServerLoadoutFromData(Loadout loadout) {
        try {
            // Send create loadout from data request to server using modern CustomPayload system
            System.out.println("CLIENT: Sending CreateLoadoutFromDataPayload to server for: " + loadout.getName());
            ClientPlayNetworking.send(new CreateLoadoutFromDataPayload(loadout));
            
            lastOperationSuccess = true;
            lastOperationResult = "Sent create request to server for loadout: " + loadout.getName();
            return true;
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to send create request: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to create server loadout from data", e);
            return false;
        }
    }
    
    /**
     * Save provided loadout data to local storage
     */
    public boolean saveLocalLoadoutFromData(Loadout loadout) {
        try {
            localStorage.saveLocalLoadout(loadout);
            
            lastOperationSuccess = true;
            lastOperationResult = "Saved local loadout: " + loadout.getName();
            
            notifyListeners();
            return true;
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to save local loadout: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to save local loadout from data", e);
            return false;
        }
    }
    public boolean exportLoadout(UUID loadoutId, String filename) {
        try {
            // Find the loadout
            Loadout loadout = null;
            
            // Check global loadouts first
            Loadout[] globalLoadouts = localStorage.getGlobalLoadouts();
            for (Loadout globalLoadout : globalLoadouts) {
                if (globalLoadout != null && globalLoadout.getId().equals(loadoutId)) {
                    loadout = globalLoadout;
                    break;
                }
            }
            
            // Check local loadouts if not found in global
            if (loadout == null) {
                for (Loadout localLoadout : localStorage.getLocalLoadouts()) {
                    if (localLoadout.getId().equals(loadoutId)) {
                        loadout = localLoadout;
                        break;
                    }
                }
            }
            
            // Check server loadouts if connected and not found elsewhere
            if (loadout == null && isConnectedToServer && serverLoadouts.containsKey(loadoutId)) {
                loadout = serverLoadouts.get(loadoutId);
            }
            
            // Check server-shared loadouts if connected and not found elsewhere
            if (loadout == null && isConnectedToServer && serverSharedLoadouts.containsKey(loadoutId)) {
                loadout = serverSharedLoadouts.get(loadoutId);
            }
            
            if (loadout == null) {
                lastOperationSuccess = false;
                lastOperationResult = "Loadout not found for export: " + loadoutId;
                return false;
            }
            
            // Create a copy of the loadout with the new name (remove .nbt extension if present)
            String loadoutName = filename;
            if (loadoutName.toLowerCase().endsWith(".nbt")) {
                loadoutName = loadoutName.substring(0, loadoutName.length() - 4);
            }
            Loadout exportLoadout = loadout.copyWithName(loadoutName);
            
            // Get the exported directory path
            MinecraftClient client = MinecraftClient.getInstance();
            Path exportedPath;
            
            if (client.getServer() != null) {
                // Single-player mode - use integrated server save path
                exportedPath = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve("logical-loadouts").resolve("exported");
            } else {
                // Multiplayer client - use local directory (limited functionality)
                exportedPath = java.nio.file.Paths.get("logical-loadouts", "exported");
            }
            
            // Create the exported directory if it doesn't exist
            java.nio.file.Files.createDirectories(exportedPath);
            
            // Sanitize filename and add .nbt extension if not present
            if (!filename.toLowerCase().endsWith(".nbt")) {
                filename += ".nbt";
            }
            filename = filename.replaceAll("[^a-zA-Z0-9_.-]", "_");
            
            // Save the loadout
            Path exportFile = exportedPath.resolve(filename);
            net.minecraft.nbt.NbtIo.writeCompressed(exportLoadout.toNbt(), exportFile);
            
            lastOperationSuccess = true;
            lastOperationResult = "Exported loadout '" + exportLoadout.getName() + "' to: " + exportFile.toString();
            
            LogicalLoadouts.LOGGER.info("Exported loadout '{}' to {}", exportLoadout.getName(), exportFile);
            
            // Send chat message with clickable file link in single-player mode
            if (client.player != null) {
                Text message = Text.literal("Exported loadout '").append(
                    Text.literal(exportLoadout.getName()).formatted(Formatting.GREEN)
                ).append(Text.literal("' to: ")).append(
                    Text.literal(exportedPath.toString()).styled(style -> 
                        style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, exportedPath.toString()))
                            .withColor(Formatting.BLUE)
                            .withUnderline(true)
                    )
                );
                client.player.sendMessage(message, false);
            }
            
            return true;
            
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to export loadout: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to export loadout", e);
            return false;
        }
    }
    
    /**
     * Load server-shared loadouts from the logical-loadouts/server directory
     */
    public List<Loadout> loadServerSharedLoadouts() {
        List<Loadout> serverLoadouts = new ArrayList<>();
        
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Path serverLoadoutsPath;
            
            if (client.getServer() != null) {
                // Single-player mode - use integrated server save path
                serverLoadoutsPath = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve("logical-loadouts").resolve("server");
            } else {
                // Multiplayer client - this shouldn't happen for server loadouts
                LogicalLoadouts.LOGGER.warn("Attempted to load server loadouts in multiplayer client mode");
                return serverLoadouts;
            }
            
            if (!java.nio.file.Files.exists(serverLoadoutsPath)) {
                java.nio.file.Files.createDirectories(serverLoadoutsPath);
                return serverLoadouts;
            }
            
            // Load all .nbt files from the server directory
            try (java.nio.file.DirectoryStream<java.nio.file.Path> stream = 
                 java.nio.file.Files.newDirectoryStream(serverLoadoutsPath, "*.nbt")) {
                for (java.nio.file.Path file : stream) {
                    try {
                        net.minecraft.nbt.NbtCompound nbt = net.minecraft.nbt.NbtIo.readCompressed(file, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
                        Loadout loadout = Loadout.fromNbt(nbt);
                        
                        if (loadout.isValid()) {
                            serverLoadouts.add(loadout);
                        } else {
                            LogicalLoadouts.LOGGER.warn("Invalid server loadout found: {}", file.getFileName());
                        }
                    } catch (Exception e) {
                        LogicalLoadouts.LOGGER.error("Failed to load server loadout: {}", file.getFileName(), e);
                    }
                }
            }
            
            LogicalLoadouts.LOGGER.debug("Loaded {} server-shared loadouts", serverLoadouts.size());
            
        } catch (Exception e) {
            LogicalLoadouts.LOGGER.error("Failed to load server-shared loadouts", e);
        }
        
        return serverLoadouts;
    }
    
    /**
     * Save a loadout to the server-shared directory
     */
    public boolean saveServerSharedLoadout(Loadout loadout) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Path serverLoadoutsPath;
            
            if (client.getServer() != null) {
                // Single-player mode - use integrated server save path
                serverLoadoutsPath = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve("logical-loadouts").resolve("server");
            } else {
                // Multiplayer client - cannot save server loadouts
                lastOperationSuccess = false;
                lastOperationResult = "Cannot save server loadouts from multiplayer client";
                return false;
            }
            
            // Create directory if it doesn't exist
            java.nio.file.Files.createDirectories(serverLoadoutsPath);
            
            // Sanitize filename
            String filename = loadout.getName().replaceAll("[^a-zA-Z0-9_-]", "_") + ".nbt";
            Path loadoutFile = serverLoadoutsPath.resolve(filename);
            
            // Save the loadout
            net.minecraft.nbt.NbtIo.writeCompressed(loadout.toNbt(), loadoutFile);
            
            lastOperationSuccess = true;
            lastOperationResult = "Saved server-shared loadout: " + loadout.getName();
            
            LogicalLoadouts.LOGGER.info("Saved server-shared loadout '{}' to {}", loadout.getName(), loadoutFile);
            return true;
            
        } catch (Exception e) {
            lastOperationSuccess = false;
            lastOperationResult = "Failed to save server-shared loadout: " + e.getMessage();
            LogicalLoadouts.LOGGER.error("Failed to save server-shared loadout", e);
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
        
        // Check server-shared loadouts if connected (send to server for permission validation)
        if (isConnectedToServer && serverSharedLoadouts.containsKey(loadoutId)) {
            System.out.println("Attempting to delete server-shared loadout from server...");
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
            serverSharedLoadouts.clear();
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
    public void handleServerLoadoutsSync(List<Loadout> personalLoadouts, List<Loadout> serverSharedLoadouts) {
        this.serverLoadouts.clear();
        this.serverSharedLoadouts.clear();
        
        // Convert personal loadouts list to map with UUID -> Loadout mapping
        for (Loadout loadout : personalLoadouts) {
            this.serverLoadouts.put(loadout.getId(), loadout);
        }
        
        // Convert server-shared loadouts list to map with UUID -> Loadout mapping
        for (Loadout loadout : serverSharedLoadouts) {
            this.serverSharedLoadouts.put(loadout.getId(), loadout);
        }
        
        lastOperationSuccess = true;
        lastOperationResult = "Synchronized " + personalLoadouts.size() + " personal and " + serverSharedLoadouts.size() + " server-shared loadouts from server";
        
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
        // Skip for section operations since server already sends updated loadouts directly
        if (success && (operation.equals("create") || operation.equals("delete") || operation.equals("update"))) {
            // Don't request loadouts for section operations - server sends them directly
            if (!operation.equals("apply_section") && !operation.equals("deposit_section")) {
                requestLoadoutsFromServer();
            }
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