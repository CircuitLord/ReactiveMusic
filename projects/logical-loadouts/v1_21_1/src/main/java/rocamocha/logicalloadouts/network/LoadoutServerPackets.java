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
        
        // Validate that the loadout is not empty before creating it
        if (isLoadoutEmpty(loadout)) {
            System.out.println("SERVER: Rejecting empty loadout creation for '" + loadout.getName() + "'");
            System.out.println("  Hotbar items: " + countNonEmptyItems(loadout.getHotbar()));
            System.out.println("  MainInventory items: " + countNonEmptyItems(loadout.getMainInventory()));
            System.out.println("  Armor items: " + countNonEmptyItems(loadout.getArmor()));
            System.out.println("  Offhand items: " + countNonEmptyItems(loadout.getOffhand()));
            server.execute(() -> {
                sendOperationResult(player, "create", LoadoutManager.LoadoutOperationResult.error("Cannot create empty loadout"));
            });
            return;
        }
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                // Ensure player data is loaded for single-player mode
                ensurePlayerDataLoaded(manager, player.getUuid());
                
                // Use the provided loadout data instead of capturing from player
                LoadoutManager.LoadoutOperationResult result = manager.createLoadoutFromData(player, loadout);
                
                // For section loadouts, clear the corresponding section from player's inventory
                clearPlayerSectionAfterLoadoutCreation(player, loadout);
                
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
    
    /**
     * Clear the player's inventory section that was just saved to a loadout
     */
    private static void clearPlayerSectionAfterLoadoutCreation(ServerPlayerEntity player, Loadout loadout) {
        // Check which sections have items in the loadout and clear those sections from player
        boolean hasArmor = false;
        boolean hasHotbar = false;
        boolean hasInventory = false;
        
        // Check armor
        for (net.minecraft.item.ItemStack item : loadout.getArmor()) {
            if (!item.isEmpty()) {
                hasArmor = true;
                break;
            }
        }
        
        // Check hotbar
        for (net.minecraft.item.ItemStack item : loadout.getHotbar()) {
            if (!item.isEmpty()) {
                hasHotbar = true;
                break;
            }
        }
        
        // Check main inventory
        for (net.minecraft.item.ItemStack item : loadout.getMainInventory()) {
            if (!item.isEmpty()) {
                hasInventory = true;
                break;
            }
        }
        
        // Clear the sections that have items
        if (hasArmor) {
            for (int i = 0; i < 4; i++) {
                player.getInventory().armor.set(i, net.minecraft.item.ItemStack.EMPTY);
            }
            LogicalLoadouts.LOGGER.debug("Cleared armor section from player {} after creating loadout", player.getName().getString());
        }
        
        if (hasHotbar) {
            for (int i = 0; i < 9; i++) {
                player.getInventory().setStack(i, net.minecraft.item.ItemStack.EMPTY);
            }
            LogicalLoadouts.LOGGER.debug("Cleared hotbar section from player {} after creating loadout", player.getName().getString());
        }
        
        if (hasInventory) {
            for (int i = 0; i < 27; i++) {
                player.getInventory().setStack(i + 9, net.minecraft.item.ItemStack.EMPTY);
            }
            LogicalLoadouts.LOGGER.debug("Cleared inventory section from player {} after creating loadout", player.getName().getString());
        }
        
        // Mark inventory as dirty to synchronize changes
        if (hasArmor || hasHotbar || hasInventory) {
            player.getInventory().markDirty();
        }
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
        boolean consumeAfterApply = payload.consumeAfterApply();
        
        server.execute(() -> {
            try {
                System.out.println("DEBUG: Server received local loadout: " + loadout.getName() + " for player " + player.getName().getString() + ", consume=" + consumeAfterApply);
                
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
                        
                        // Check if the updated loadout is now empty and delete it if so
                        if (isLoadoutEmpty(currentInventory)) {
                            LoadoutManager.LoadoutOperationResult deleteResult = manager.deleteLoadout(player.getUuid(), loadout.getId());
                            if (deleteResult.isSuccess()) {
                                System.out.println("DEBUG: Deleted empty loadout after swap: " + loadout.getName());
                            } else {
                                LogicalLoadouts.LOGGER.warn("Failed to delete empty loadout after swap: {}", deleteResult.getMessage());
                            }
                        }
                        
                        // Send updated loadout list to client
                        sendLoadoutsSync(player, manager);
                    } else {
                        LogicalLoadouts.LOGGER.warn("Failed to update server loadout after swap: {}", updateResult.getMessage());
                    }
                } else {
                    // Not a server-stored loadout (local/global) - just apply it
                    System.out.println("DEBUG: Local/global loadout, just applying");
                    applyLoadoutToPlayer(player, loadout);
                    
                    // If consumeAfterApply is true, delete the personal loadout after applying
                    if (consumeAfterApply) {
                        System.out.println("DEBUG: Consuming personal loadout after application");
                        // For personal loadouts, we need to notify the client to remove it from their local storage
                        // Since personal loadouts are client-side, we can't delete them server-side
                        // The client has already handled the consumption in the GUI
                    }
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
     * Handle client request to apply a section from a loadout to their inventory
     */
    public static void handleApplySection(ApplySectionPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        UUID loadoutId = payload.loadoutId();
        String sectionName = payload.sectionName();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                // Ensure player data is loaded for single-player mode
                ensurePlayerDataLoaded(manager, player.getUuid());
                LoadoutManager.LoadoutOperationResult getResult = manager.getLoadout(player.getUuid(), loadoutId);
                
                if (getResult.isSuccess()) {
                    Loadout loadout = getResult.getLoadout();
                    
                    // Capture player's original section items BEFORE applying loadout items (for swap behavior)
                    ItemStack[] playerOriginalItems = capturePlayerSectionItems(player, sectionName);
                    
                    // Apply the specific section from loadout to player
                    applySectionFromLoadoutToPlayer(player, loadout, sectionName);
                    
                    // Mark inventory as dirty to synchronize changes to client
                    player.getInventory().markDirty();
                    
                    // Update the loadout with the player's original items (swap behavior)
                    Loadout updatedLoadout = createUpdatedLoadoutWithPlayerItems(loadout, sectionName, playerOriginalItems);
                    LoadoutManager.LoadoutOperationResult updateResult = manager.updateLoadout(player.getUuid(), updatedLoadout);
                    
                    if (updateResult.isSuccess()) {
                        // Check if the updated loadout is now empty and delete it if so
                        if (isLoadoutEmpty(updatedLoadout)) {
                            LoadoutManager.LoadoutOperationResult deleteResult = manager.deleteLoadout(player.getUuid(), loadout.getId());
                            if (deleteResult.isSuccess()) {
                                LogicalLoadouts.LOGGER.debug("Deleted empty loadout after section apply: {}", loadout.getName());
                            } else {
                                LogicalLoadouts.LOGGER.warn("Failed to delete empty loadout after section apply: {}", deleteResult.getMessage());
                            }
                        }
                        
                        // Send updated loadout list to client
                        sendLoadoutsSync(player, manager);
                        LogicalLoadouts.LOGGER.debug("Applied section '{}' from loadout '{}' to player {}", 
                                                    sectionName, loadout.getName(), player.getName().getString());
                    } else {
                        sendOperationResult(player, "apply_section", updateResult);
                    }
                } else {
                    sendOperationResult(player, "apply_section", getResult);
                }
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling apply section packet", e);
                sendOperationResult(player, "apply_section", LoadoutManager.LoadoutOperationResult.error("Failed to apply section"));
            }
        });
    }
    
    /**
     * Handle client request to deposit a section from their inventory into a loadout
     */
    public static void handleDepositSection(DepositSectionPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        UUID loadoutId = payload.loadoutId();
        String sectionName = payload.sectionName();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                // Ensure player data is loaded for single-player mode
                ensurePlayerDataLoaded(manager, player.getUuid());
                LoadoutManager.LoadoutOperationResult getResult = manager.getLoadout(player.getUuid(), loadoutId);
                
                if (getResult.isSuccess()) {
                    Loadout loadout = getResult.getLoadout();
                    
                    // Check if loadout section is empty (deposit requirement)
                    if (isLoadoutSectionEmpty(loadout, sectionName)) {
                        // Deposit the section from player to loadout
                        Loadout updatedLoadout = depositSectionFromPlayerToLoadout(player, loadout, sectionName);
                        
                        // Mark inventory as dirty to synchronize changes to client
                        player.getInventory().markDirty();
                        
                        LoadoutManager.LoadoutOperationResult updateResult = manager.updateLoadout(player.getUuid(), updatedLoadout);
                        
                        if (updateResult.isSuccess()) {
                            // Send updated loadout list to client
                            sendLoadoutsSync(player, manager);
                            LogicalLoadouts.LOGGER.debug("Deposited section '{}' from player {} to loadout '{}'", 
                                                        sectionName, player.getName().getString(), loadout.getName());
                        } else {
                            sendOperationResult(player, "deposit_section", updateResult);
                        }
                    } else {
                        sendOperationResult(player, "deposit_section", 
                                          LoadoutManager.LoadoutOperationResult.error("Loadout section is not empty"));
                    }
                } else {
                    sendOperationResult(player, "deposit_section", getResult);
                }
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling deposit section packet", e);
                sendOperationResult(player, "deposit_section", LoadoutManager.LoadoutOperationResult.error("Failed to deposit section"));
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
     * Send updated loadout list to all connected players (public method for admin commands)
     */
    public static void sendLoadoutsSyncToAllPlayers(MinecraftServer server, LoadoutManager manager) {
        for (ServerPlayerEntity connectedPlayer : server.getPlayerManager().getPlayerList()) {
            sendLoadoutsSync(connectedPlayer, manager);
        }
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
    
    /**
     * Apply a specific section from a loadout to a player's inventory
     */
    private static void applySectionFromLoadoutToPlayer(ServerPlayerEntity player, Loadout loadout, String sectionName) {
        switch (sectionName.toUpperCase()) {
            case "ARMOR":
                for (int i = 0; i < 4; i++) {
                    player.getInventory().armor.set(i, loadout.getArmor()[i].copy());
                }
                break;
            case "HOTBAR":
                for (int i = 0; i < 9; i++) {
                    player.getInventory().setStack(i, loadout.getHotbar()[i].copy());
                }
                break;
            case "INVENTORY":
                for (int i = 0; i < 27; i++) {
                    player.getInventory().setStack(i + 9, loadout.getMainInventory()[i].copy());
                }
                break;
        }
    }
    
    /**
     * Capture a player's section items before they are replaced
     */
    private static ItemStack[] capturePlayerSectionItems(ServerPlayerEntity player, String sectionName) {
        switch (sectionName.toUpperCase()) {
            case "ARMOR":
                ItemStack[] armorItems = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    armorItems[i] = player.getInventory().getArmorStack(i).copy();
                }
                return armorItems;
            case "HOTBAR":
                ItemStack[] hotbarItems = new ItemStack[9];
                for (int i = 0; i < 9; i++) {
                    hotbarItems[i] = player.getInventory().getStack(i).copy();
                }
                return hotbarItems;
            case "INVENTORY":
                ItemStack[] inventoryItems = new ItemStack[27];
                for (int i = 0; i < 27; i++) {
                    inventoryItems[i] = player.getInventory().getStack(i + 9).copy();
                }
                return inventoryItems;
            default:
                return new ItemStack[0];
        }
    }
    
    /**
     * Create an updated loadout with the player's original items in the specified section
     */
    private static Loadout createUpdatedLoadoutWithPlayerItems(Loadout loadout, String sectionName, ItemStack[] playerItems) {
        Loadout updatedLoadout = new Loadout(loadout.getId(), loadout.getName());
        
        // Copy all existing data first
        for (int i = 0; i < 9; i++) updatedLoadout.setHotbarSlot(i, loadout.getHotbar()[i].copy());
        for (int i = 0; i < 27; i++) updatedLoadout.setMainInventorySlot(i, loadout.getMainInventory()[i].copy());
        for (int i = 0; i < 4; i++) updatedLoadout.setArmorSlot(i, loadout.getArmor()[i].copy());
        for (int i = 0; i < 1; i++) updatedLoadout.setOffhandSlot(i, loadout.getOffhand()[i].copy());
        
        // Update the specific section with player's original items
        switch (sectionName.toUpperCase()) {
            case "ARMOR":
                for (int i = 0; i < 4 && i < playerItems.length; i++) {
                    updatedLoadout.setArmorSlot(i, playerItems[i]);
                }
                break;
            case "HOTBAR":
                for (int i = 0; i < 9 && i < playerItems.length; i++) {
                    updatedLoadout.setHotbarSlot(i, playerItems[i]);
                }
                break;
            case "INVENTORY":
                for (int i = 0; i < 27 && i < playerItems.length; i++) {
                    updatedLoadout.setMainInventorySlot(i, playerItems[i]);
                }
                break;
        }
        
        return updatedLoadout;
    }
    
    /**
     * Deposit a specific section from player to loadout
     */
    private static Loadout depositSectionFromPlayerToLoadout(ServerPlayerEntity player, Loadout loadout, String sectionName) {
        Loadout updatedLoadout = new Loadout(loadout.getId(), loadout.getName());
        
        // Copy all existing data first
        for (int i = 0; i < 9; i++) updatedLoadout.setHotbarSlot(i, loadout.getHotbar()[i].copy());
        for (int i = 0; i < 27; i++) updatedLoadout.setMainInventorySlot(i, loadout.getMainInventory()[i].copy());
        for (int i = 0; i < 4; i++) updatedLoadout.setArmorSlot(i, loadout.getArmor()[i].copy());
        for (int i = 0; i < 1; i++) updatedLoadout.setOffhandSlot(i, loadout.getOffhand()[i].copy());
        
        // Update the specific section with player's items and clear player's section
        switch (sectionName.toUpperCase()) {
            case "ARMOR":
                for (int i = 0; i < 4; i++) {
                    updatedLoadout.setArmorSlot(i, player.getInventory().getArmorStack(i).copy());
                    player.getInventory().armor.set(i, net.minecraft.item.ItemStack.EMPTY);
                }
                break;
            case "HOTBAR":
                for (int i = 0; i < 9; i++) {
                    updatedLoadout.setHotbarSlot(i, player.getInventory().getStack(i).copy());
                    player.getInventory().setStack(i, net.minecraft.item.ItemStack.EMPTY);
                }
                break;
            case "INVENTORY":
                for (int i = 0; i < 27; i++) {
                    updatedLoadout.setMainInventorySlot(i, player.getInventory().getStack(i + 9).copy());
                    player.getInventory().setStack(i + 9, net.minecraft.item.ItemStack.EMPTY);
                }
                break;
        }
        
        return updatedLoadout;
    }
    
    /**
     * Check if a specific section in a loadout is empty
     */
    private static boolean isLoadoutSectionEmpty(Loadout loadout, String sectionName) {
        switch (sectionName.toUpperCase()) {
            case "ARMOR":
                for (net.minecraft.item.ItemStack item : loadout.getArmor()) {
                    if (!item.isEmpty()) return false;
                }
                return true;
            case "HOTBAR":
                for (net.minecraft.item.ItemStack item : loadout.getHotbar()) {
                    if (!item.isEmpty()) return false;
                }
                return true;
            case "INVENTORY":
                for (net.minecraft.item.ItemStack item : loadout.getMainInventory()) {
                    if (!item.isEmpty()) return false;
                }
                return true;
            default:
                return true;
        }
    }
    
    /**
     * Handle client request for loadout list
     */
    public static void handleRequestLoadouts(RequestLoadoutsPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                // Ensure player data is loaded for single-player mode
                ensurePlayerDataLoaded(manager, player.getUuid());
                
                // Send loadout list to client
                sendLoadoutsSync(player, manager);
                
                LogicalLoadouts.LOGGER.debug("Sent loadout list to player {}", player.getName().getString());
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling request loadouts packet", e);
                sendOperationResult(player, "request_loadouts", LoadoutManager.LoadoutOperationResult.error("Failed to retrieve loadouts"));
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
     * Check if a loadout is completely empty (no items in any section)
     */
    private static boolean isLoadoutEmpty(Loadout loadout) {
        // Check armor
        for (net.minecraft.item.ItemStack item : loadout.getArmor()) {
            if (!item.isEmpty()) return false;
        }
        
        // Check hotbar
        for (net.minecraft.item.ItemStack item : loadout.getHotbar()) {
            if (!item.isEmpty()) return false;
        }
        
        // Check main inventory
        for (net.minecraft.item.ItemStack item : loadout.getMainInventory()) {
            if (!item.isEmpty()) return false;
        }
        
        // Check offhand
        for (net.minecraft.item.ItemStack item : loadout.getOffhand()) {
            if (!item.isEmpty()) return false;
        }
        
        return true;
    }
    
    /**
     * Handle client request to reload server loadouts
     */
    public static void handleReloadServerLoadouts(ReloadServerLoadoutsPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                
                // Check if player has permission to reload server loadouts (ops only)
                if (!manager.hasPermission(player, "logical-loadouts.admin")) {
                    sendOperationResult(player, "reload_server_loadouts", 
                        LoadoutManager.LoadoutOperationResult.error("You don't have permission to reload server loadouts"));
                    return;
                }
                
                // Reload server loadouts
                manager.reloadServerSharedLoadouts();
                
                // Send updated loadout list to all connected clients
                sendLoadoutsSyncToAllPlayers(server, manager);
                
                sendOperationResult(player, "reload_server_loadouts", 
                    LoadoutManager.LoadoutOperationResult.success(null));
                LogicalLoadouts.LOGGER.info("Player {} reloaded server loadouts", player.getName().getString());
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling reload server loadouts packet", e);
                sendOperationResult(player, "reload_server_loadouts", 
                    LoadoutManager.LoadoutOperationResult.error("Failed to reload server loadouts"));
            }
        });
    }
    
    /**
     * Handle client request to upload a loadout to server
     */
    public static void handleUploadLoadout(UploadLoadoutPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = context.server();
        Loadout loadout = payload.loadout();
        String name = payload.name();
        
        server.execute(() -> {
            try {
                LoadoutManager manager = getLoadoutManager(server);
                
                // Check if player has permission to upload server loadouts (ops only)
                if (!manager.hasPermission(player, "logical-loadouts.admin")) {
                    sendOperationResult(player, "upload_loadout", 
                        LoadoutManager.LoadoutOperationResult.error("You don't have permission to upload server loadouts"));
                    return;
                }
                
                // Create a new loadout with the specified name for server storage
                Loadout serverLoadout = new Loadout(name);
                
                // Copy all data from the uploaded loadout
                for (int i = 0; i < 9; i++) serverLoadout.setHotbarSlot(i, loadout.getHotbar()[i].copy());
                for (int i = 0; i < 27; i++) serverLoadout.setMainInventorySlot(i, loadout.getMainInventory()[i].copy());
                for (int i = 0; i < 4; i++) serverLoadout.setArmorSlot(i, loadout.getArmor()[i].copy());
                for (int i = 0; i < 1; i++) serverLoadout.setOffhandSlot(i, loadout.getOffhand()[i].copy());
                
                // Save as server loadout
                LoadoutManager.LoadoutOperationResult result = manager.createServerLoadout(serverLoadout, name);
                
                if (result.isSuccess()) {
                    // Reload server loadouts to make them available immediately
                    manager.reloadServerSharedLoadouts();
                    
                    // Send updated loadout list to all connected clients
                    sendLoadoutsSyncToAllPlayers(server, manager);
                    
                    sendOperationResult(player, "upload_loadout", result);
                    LogicalLoadouts.LOGGER.info("Player {} uploaded server loadout '{}'", player.getName().getString(), name);
                } else {
                    sendOperationResult(player, "upload_loadout", result);
                }
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Error handling upload loadout packet", e);
                sendOperationResult(player, "upload_loadout", LoadoutManager.LoadoutOperationResult.error("Failed to upload loadout"));
            }
        });
    }
}