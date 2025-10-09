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
                LoadoutManager.LoadoutOperationResult result = manager.createLoadout(player.getUuid(), loadoutName);
                
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
                    
                    // Notify client that loadout was applied
                    ServerPlayNetworking.send(player, new LoadoutAppliedPayload(loadout.getName()));
                    
                    LogicalLoadouts.LOGGER.debug("Applied loadout '{}' to player {}", loadout.getName(), player.getName().getString());
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
                
                applyLoadoutToPlayer(player, loadout);
                
                // Notify client that loadout was applied
                ServerPlayNetworking.send(player, new LoadoutAppliedPayload(loadout.getName()));
                
                LogicalLoadouts.LOGGER.debug("Applied local loadout '{}' to player {}", loadout.getName(), player.getName().getString());
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
                    
                    sendOperationResult(player, "save", saveResult);
                    
                    if (saveResult.isSuccess()) {
                        sendLoadoutsSync(player, manager);
                        LogicalLoadouts.LOGGER.debug("Saved current inventory to loadout '{}' for player {}", 
                                                    loadout.getName(), player.getName().getString());
                    }
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
        
        // Clear inventory first to prevent conflicts
        player.getInventory().clear();
        
        // Apply hotbar using proper inventory methods
        ItemStack[] hotbar = loadout.getHotbar();
        for (int i = 0; i < Math.min(hotbar.length, 9); i++) {
            if (!hotbar[i].isEmpty()) {
                player.getInventory().setStack(i, hotbar[i].copy());
            }
        }
        
        // Apply main inventory using proper inventory methods  
        ItemStack[] mainInventory = loadout.getMainInventory();
        for (int i = 0; i < Math.min(mainInventory.length, 27); i++) {
            if (!mainInventory[i].isEmpty()) {
                player.getInventory().setStack(i + 9, mainInventory[i].copy());
            }
        }
        
        // Apply armor using proper armor slot methods
        ItemStack[] armor = loadout.getArmor();
        for (int i = 0; i < Math.min(armor.length, 4); i++) {
            if (!armor[i].isEmpty()) {
                player.getInventory().armor.set(i, armor[i].copy());
            }
        }
        
        // Apply offhand using proper offhand methods
        ItemStack[] offhand = loadout.getOffhand();
        if (offhand.length > 0 && !offhand[0].isEmpty()) {
            player.getInventory().offHand.set(0, offhand[0].copy());
        }
        
        // Critical: Mark inventory as dirty first
        player.getInventory().markDirty();
        
        // Force full inventory synchronization - this is crucial for survival mode
        // Step 1: Reset all tracked slots to force updates
        for (int i = 0; i < player.getInventory().size(); i++) {
            player.playerScreenHandler.setPreviousTrackedSlot(i, ItemStack.EMPTY);
        }
        
        // Step 2: Sync the player screen handler
        player.playerScreenHandler.syncState();
        player.playerScreenHandler.sendContentUpdates();
        
        // Step 3: Force additional synchronization for survival mode
        if (!player.getAbilities().creativeMode) {
            LogicalLoadouts.LOGGER.debug("Applying survival mode synchronization for player {}", player.getUuid());
            
            // Force the current screen handler to sync (this handles the player inventory screen)
            player.currentScreenHandler.syncState();
            player.currentScreenHandler.sendContentUpdates();
            
            // Reset tracking for current screen handler too
            for (int i = 0; i < player.currentScreenHandler.slots.size(); i++) {
                if (i < player.getInventory().size()) {
                    player.currentScreenHandler.setPreviousTrackedSlot(i, ItemStack.EMPTY);
                }
            }
            
            // Send final content update
            player.currentScreenHandler.sendContentUpdates();
            
            // Force player abilities sync to ensure inventory is properly updated
            player.sendAbilitiesUpdate();
        }
        
        LogicalLoadouts.LOGGER.debug("Completed inventory synchronization for player {}", player.getUuid());
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
        List<Loadout> loadouts = manager.getPlayerLoadouts(player.getUuid());
        
        // Convert loadouts to NBT compound list for transmission
        List<NbtCompound> loadoutNbts = loadouts.stream()
            .map(Loadout::toNbt)
            .collect(Collectors.toList());
        
        // Send using modern CustomPayload system
        ServerPlayNetworking.send(player, new LoadoutsSyncPayload(loadoutNbts));
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