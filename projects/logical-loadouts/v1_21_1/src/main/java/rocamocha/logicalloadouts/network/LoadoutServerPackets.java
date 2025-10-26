package rocamocha.logicalloadouts.network;

import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.data.Loadout;
import rocamocha.logicalloadouts.network.packets.*;
import rocamocha.logicalloadouts.server.LoadoutManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Server-side packet handlers for loadout operations.
 * All operations are validated server-side before execution.
 */
public class LoadoutServerPackets {
    
    /**
     * Handle client request to create a new loadout
     */
    public static void handleCreateLoadout(CreateLoadoutPayload payload, ServerPlayNetworking.Context context) {
        LogicalLoadouts.LOGGER.info("SERVER: Received CreateLoadoutPayload from client");
        System.out.println("SERVER: handleCreateLoadout called for loadout: " + payload.loadoutName());
        
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        String loadoutName = payload.loadoutName();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                // Ensure player data is loaded for single-player mode
                ensurePlayerDataLoaded(manager, player.getUuid());
                LoadoutManager.LoadoutOperationResult result = manager.createLoadout(player, loadoutName);
                
                if (result.isSuccess()) {
                    // Clear the player's inventory after creating the loadout (bank behavior)
                    player.getInventory().clear();
                    player.getInventory().markDirty();
                }
                
                sendOperationResult(player, "create", result);
                
                if (result.isSuccess()) {
                    // Send updated loadout list to client
                    sendLoadoutsSync(player, manager);
                }
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling create loadout packet", e);
                sendOperationResult(player, "create", LoadoutManager.LoadoutOperationResult.error("Internal server error"));
            }
        });
    }
    
    /**
     * Handle client request to create a new loadout from provided data
     */
    public static void handleCreateLoadoutFromData(CreateLoadoutFromDataPayload payload, ServerPlayNetworking.Context context) {
        LogicalLoadouts.LOGGER.info("SERVER: Received CreateLoadoutFromDataPayload from client");
        System.out.println("SERVER: handleCreateLoadoutFromData called for loadout: " + payload.loadout().getName());
        
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        Loadout loadout = payload.loadout();
        
        // Debug: Check what items are in the received loadout
        System.out.println("SERVER: Received loadout '" + loadout.getName() + "' with:");
        System.out.println("  Hotbar: " + countNonEmptyItems(loadout.getHotbar()) + " items");
        System.out.println("  MainInventory: " + countNonEmptyItems(loadout.getMainInventory()) + " items");
        System.out.println("  Armor: " + countNonEmptyItems(loadout.getArmor()) + " items");
        System.out.println("  Offhand: " + (loadout.getOffhand().length > 0 && !loadout.getOffhand()[0].isEmpty() ? 1 : 0) + " items");
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                // Ensure player data is loaded for single-player mode
                ensurePlayerDataLoaded(manager, player.getUuid());
                
                // Use the provided loadout data instead of capturing from player
                LoadoutManager.LoadoutOperationResult result = manager.createLoadoutFromData(player, loadout);
                
                // NOTE: Section loadouts should NOT clear the player's inventory
                // Only personal loadouts (full inventory deposits) clear the inventory
                
                sendOperationResult(player, "create", result);
                
                if (result.isSuccess()) {
                    // Send updated loadout list to client
                    sendLoadoutsSync(player, manager);
                }
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling create loadout from data packet", e);
                sendOperationResult(player, "create", LoadoutManager.LoadoutOperationResult.error("Internal server error"));
            }
        });
    }
    
    // Helper method for debug output
    private static int countNonEmptyItems(net.minecraft.item.ItemStack[] items) {
        int count = 0;
        for (net.minecraft.item.ItemStack item : items) {
            if (item != null && !item.isEmpty()) count++;
        }
        return count;
    }
    
    /**
     * Handle client request to delete a loadout
     */
    public static void handleDeleteLoadout(DeleteLoadoutPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        UUID loadoutId = payload.loadoutId();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                LoadoutManager.LoadoutOperationResult result = manager.deleteLoadout(player.getUuid(), loadoutId);
                
                sendOperationResult(player, "delete", result);
                
                if (result.isSuccess()) {
                    // Send updated loadout list to client
                    sendLoadoutsSync(player, manager);
                }
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling delete loadout packet", e);
                sendOperationResult(player, "delete", LoadoutManager.LoadoutOperationResult.error("Internal server error"));
            }
        });
    }
    
    /**
     * Handle client request to update a loadout
     */
    public static void handleUpdateLoadout(UpdateLoadoutPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        NbtCompound loadoutNbt = payload.loadoutData();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                Loadout loadout = Loadout.fromNbt(loadoutNbt);
                
                LoadoutManager.LoadoutOperationResult result = manager.updateLoadout(player.getUuid(), loadout);
                
                sendOperationResult(player, "update", result);
                
                if (result.isSuccess()) {
                    // Send updated loadout list to client
                    sendLoadoutsSync(player, manager);
                }
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling update loadout packet", e);
                sendOperationResult(player, "update", LoadoutManager.LoadoutOperationResult.error("Invalid loadout data"));
            }
        });
    }
    
    /**
     * Handle client request to apply a loadout to their inventory
     */
    public static void handleApplyLoadout(ApplyLoadoutPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        UUID loadoutId = payload.loadoutId();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                // Ensure player data is loaded for single-player mode
                ensurePlayerDataLoaded(manager, player.getUuid());
                LoadoutManager.LoadoutOperationResult result = manager.getLoadout(player.getUuid(), loadoutId);
                
                if (result.isSuccess()) {
                    Loadout loadout = result.getLoadout();
                    applyLoadoutToPlayer(player, loadout);
                    
                    // Delete the loadout after applying (bank behavior)
                    LoadoutManager.LoadoutOperationResult deleteResult = manager.deleteLoadout(player.getUuid(), loadoutId);
                    if (!deleteResult.isSuccess()) {
                        LogicalLoadouts.LOGGER.warn("Failed to delete loadout after applying: {}", deleteResult.getMessage());
                    }
                    
                    // Notify client that loadout was applied
                    ServerPlayNetworking.send(player, new LoadoutAppliedPayload(loadout.getName()));
                    
                    // Send updated loadout list to client (since we deleted one)
                    sendLoadoutsSync(player, manager);
                    
                    LogicalLoadouts.LOGGER.debug("Applied and deleted loadout '{}' for player {}", loadout.getName(), player.getName().getString());
                } else {
                    sendOperationResult(player, "apply", result);
                }
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling apply loadout packet", e);
                sendOperationResult(player, "apply", LoadoutManager.LoadoutOperationResult.error("Failed to apply loadout"));
            }
        });
    }
    
    /**
     * Handle applying a local loadout (includes loadout data)
     */
    public static void handleApplyLocalLoadout(ApplyLocalLoadoutPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        Loadout loadout = payload.loadout();
        
        server.execute(() -> {
            try {
                System.out.println("DEBUG: Server received local loadout: " + loadout.getName() + " for player " + player.getName().getString());
                
                LoadoutManager manager = getLoadoutManager(server);
                ensurePlayerDataLoaded(manager, player.getUuid());
                
                // Check if this loadout exists in server storage for swapping behavior
                LoadoutManager.LoadoutOperationResult getResult = manager.getLoadout(player.getUuid(), loadout.getId());
                
                if (getResult.isSuccess()) {
                    // Server-stored loadout - implement swap behavior
                    System.out.println("DEBUG: Found server loadout, implementing swap behavior");
                    
                    // Capture current player inventory before applying the loadout
                    Loadout currentInventory = capturePlayerInventory(player, getResult.getLoadout());
                    
                    // Apply the received loadout to player
                    applyLoadoutToPlayer(player, loadout);
                    
                    // Update the server-stored loadout with the captured inventory
                    LoadoutManager.LoadoutOperationResult updateResult = manager.updateLoadout(player.getUuid(), currentInventory);
                    if (updateResult.isSuccess()) {
                        System.out.println("DEBUG: Successfully swapped inventory with server loadout");
                        // Send updated loadout list to client
                        sendLoadoutsSync(player, manager);
                    } else {
                        LogicalLoadouts.LOGGER.warn("Failed to update server loadout after swap: {}", updateResult.getMessage());
                    }
                } else {
                    // Not a server-stored loadout (local/global) - just apply it
                    System.out.println("DEBUG: Local/global loadout, just applying");
                    applyLoadoutToPlayer(player, loadout);
                }
                
                // Notify client that loadout was applied
                ServerPlayNetworking.send(player, new LoadoutAppliedPayload(loadout.getName()));
                
                LogicalLoadouts.LOGGER.debug("Applied loadout '{}' to player {}", loadout.getName(), player.getName().getString());
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Failed to apply local loadout for player {}: {}", player.getName().getString(), e.getMessage(), e);
                // Send error message to client
                ServerPlayNetworking.send(player, new OperationResultPayload("apply", false, "Loadout apply failed: " + e.getMessage()));
            }
        });
    }

    /**
     * Handle client request to save current inventory as a loadout
     */
    public static void handleSaveLoadout(SaveLoadoutPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        UUID loadoutId = payload.loadoutId();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                LoadoutManager.LoadoutOperationResult getResult = manager.getLoadout(player.getUuid(), loadoutId);
                
                if (getResult.isSuccess()) {
                    Loadout loadout = capturePlayerInventory(player, getResult.getLoadout());
                    LoadoutManager.LoadoutOperationResult saveResult = manager.updateLoadout(player.getUuid(), loadout);
                    
                    if (saveResult.isSuccess()) {
                        // Clear the player's inventory after saving (bank behavior - prevents duplication)
                        player.getInventory().clear();
                        player.getInventory().markDirty();
                        
                        sendLoadoutsSync(player, manager);
                        LogicalLoadouts.LOGGER.debug("Saved current inventory to loadout '{}' and cleared player inventory for player {}", 
                                                    loadout.getName(), player.getName().getString());
                    }
                    
                    sendOperationResult(player, "save", saveResult);
                } else {
                    sendOperationResult(player, "save", getResult);
                }
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling save loadout packet", e);
                sendOperationResult(player, "save", LoadoutManager.LoadoutOperationResult.error("Failed to save loadout"));
            }
        });
    }
    
    /**
     * Handle client request for all loadouts (sent when joining server or refreshing)
     */
    public static void handleRequestLoadouts(RequestLoadoutsPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                sendLoadoutsSync(player, manager);
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling request loadouts packet", e);
            }
        });
    }
    
    /**
     * Apply a loadout to a player's inventory
     */
    private static void applyLoadoutToPlayer(ServerPlayerEntity player, Loadout loadout) {
        LogicalLoadouts.LOGGER.debug("Applying loadout to player {} in survival mode: {}", player.getUuid(), !player.getAbilities().creativeMode);
        
        // Use the same method that works in single-player
        loadout.applyToPlayer(player);
        
        LogicalLoadouts.LOGGER.debug("Loadout application completed for player {}", player.getUuid());
    }
    
    /**
     * Capture a player's current inventory into a loadout
     */
    private static Loadout capturePlayerInventory(ServerPlayerEntity player, Loadout existingLoadout) {
        // Create a new loadout or update existing one
        Loadout loadout = new Loadout(existingLoadout.getId(), existingLoadout.getName());
        
        // Capture hotbar (slots 0-8)
        for (int i = 0; i < 9; i++) {
            loadout.setHotbarSlot(i, player.getInventory().getStack(i));
        }
        
        // Capture main inventory (slots 9-35)
        for (int i = 0; i < 27; i++) {
            loadout.setMainInventorySlot(i, player.getInventory().getStack(i + 9));
        }
        
        // Capture armor
        for (int i = 0; i < 4; i++) {
            loadout.setArmorSlot(i, player.getInventory().armor.get(i));
        }
        
        // Capture offhand
        loadout.setOffhandSlot(0, player.getInventory().offHand.get(0));
        
        return loadout;
    }
    
    /**
     * Send operation result to client
     */
    private static void sendOperationResult(ServerPlayerEntity player, String operation, LoadoutManager.LoadoutOperationResult result) {
        // Send operation result using modern CustomPayload system
        String message = result.getMessage() != null ? result.getMessage() : "";
        ServerPlayNetworking.send(player, new OperationResultPayload(operation, result.isSuccess(), message));
        
        // Also send chat message for important operations
        if (!result.isSuccess()) {
            player.sendMessage(Text.literal("§cLoadout " + operation + " failed: " + result.getMessage()), false);
        }
    }
    
    /**
     * Send complete loadout list to client
     */
    private static void sendLoadoutsSync(ServerPlayerEntity player, LoadoutManager manager) {
        UUID playerUuid = player.getUuid();
        
        // Get personal loadouts for this player
        List<Loadout> personalLoadouts = manager.getPersonalLoadouts(playerUuid);
        
        // Get server-shared loadouts (available to all players)
        List<Loadout> serverSharedLoadouts = manager.getServerSharedLoadouts();
        
        // Convert to NBT for transmission
        List<NbtCompound> personalLoadoutNbts = personalLoadouts.stream()
            .map(Loadout::toNbt)
            .collect(Collectors.toList());
            
        List<NbtCompound> serverSharedLoadoutNbts = serverSharedLoadouts.stream()
            .map(Loadout::toNbt)
            .collect(Collectors.toList());
        
        // Send using modern CustomPayload system
        ServerPlayNetworking.send(player, new LoadoutsSyncPayload(personalLoadoutNbts, serverSharedLoadoutNbts));
    }
    
    /**
     * Get the server's loadout manager instance
     */
    private static LoadoutManager getLoadoutManager(MinecraftServer server) {
        // This will be implemented when we add the manager to the server
        // For now, return a placeholder
        return LogicalLoadouts.getServerLoadoutManager(server);
    }
    
    /**
     * Ensure player data is loaded in the LoadoutManager for single-player mode
     */
    private static void ensurePlayerDataLoaded(LoadoutManager manager, UUID playerUuid) {
        try {
            // Check if player data is already loaded
            List<Loadout> existingLoadouts = manager.getPlayerLoadouts(playerUuid);
            LogicalLoadouts.LOGGER.debug("ensurePlayerDataLoaded: Player {} has {} existing loadouts", playerUuid, existingLoadouts.size());
            
            if (existingLoadouts.isEmpty()) {
                // Load player data if not already loaded
                LogicalLoadouts.LOGGER.debug("Loading player data for {}", playerUuid);
                manager.loadPlayerData(playerUuid);
                
                // Check again after loading
                List<Loadout> loadoutsAfterLoad = manager.getPlayerLoadouts(playerUuid);
                LogicalLoadouts.LOGGER.debug("After loading: Player {} has {} loadouts", playerUuid, loadoutsAfterLoad.size());
            }
        } catch (Exception e) {
            LogicalLoadouts.LOGGER.error("Failed to ensure player data loaded for " + playerUuid, e);
            // Always try to load, even if checking failed
            manager.loadPlayerData(playerUuid);
        }
    }
}