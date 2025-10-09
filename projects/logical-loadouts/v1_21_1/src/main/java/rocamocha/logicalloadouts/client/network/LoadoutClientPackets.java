package rocamocha.logicalloadouts.client.network;

import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.client.LoadoutClientManager;
import rocamocha.logicalloadouts.data.Loadout;
import rocamocha.logicalloadouts.network.packets.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles incoming network packets from the server on the client side.
 * Manages loadout synchronization and operation results using CustomPayload system.
 */
public class LoadoutClientPackets {
    
    /**
     * Register client-side packet handlers using modern CustomPayload system
     */
    public static void registerClientPackets() {
        LogicalLoadouts.LOGGER.info("Registering client-side network handlers");
        
        // Register packet handlers for server->client communication
        ClientPlayNetworking.registerGlobalReceiver(LoadoutsSyncPayload.ID, LoadoutClientPackets::handleLoadoutsSync);
        ClientPlayNetworking.registerGlobalReceiver(OperationResultPayload.ID, LoadoutClientPackets::handleOperationResult);
        ClientPlayNetworking.registerGlobalReceiver(LoadoutAppliedPayload.ID, LoadoutClientPackets::handleLoadoutApplied);
        
        LogicalLoadouts.LOGGER.info("Registered all client-side packet handlers");
    }
    
    /**
     * Handle loadouts synchronization from server
     */
    private static void handleLoadoutsSync(LoadoutsSyncPayload payload, ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        List<NbtCompound> loadoutNbts = payload.loadouts();
        
        client.execute(() -> {
            try {
                List<Loadout> loadouts = new ArrayList<>();
                for (NbtCompound nbt : loadoutNbts) {
                    Loadout loadout = Loadout.fromNbt(nbt);
                    loadouts.add(loadout);
                }
                
                // Update client manager with server loadouts
                LoadoutClientManager manager = LoadoutClientManager.getInstance();
                manager.handleServerLoadoutsSync(loadouts);
                
                LogicalLoadouts.LOGGER.debug("Synchronized {} loadouts from server", loadouts.size());
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Failed to handle loadouts sync", e);
            }
        });
    }
    
    /**
     * Handle operation result from server
     */
    private static void handleOperationResult(OperationResultPayload payload, ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        
        client.execute(() -> {
            try {
                String operation = payload.operation();
                boolean success = payload.success();
                String message = payload.message();
                
                // Update client manager with operation result
                LoadoutClientManager manager = LoadoutClientManager.getInstance();
                manager.handleServerOperationResult(operation, success, message);
                
                // Show message to player if it's an error
                if (!success && client.player != null) {
                    client.player.sendMessage(Text.literal("§cLoadout " + operation + " failed: " + message), false);
                }
                
                LogicalLoadouts.LOGGER.debug("Operation {} {}: {}", operation, success ? "succeeded" : "failed", message);
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Failed to handle operation result", e);
            }
        });
    }
    
    /**
     * Handle loadout applied notification from server
     */
    private static void handleLoadoutApplied(LoadoutAppliedPayload payload, ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        String loadoutName = payload.loadoutName();
        
        client.execute(() -> {
            try {
                // Show success message to player
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§aApplied loadout: " + loadoutName), true);
                }
                
                // Update client manager
                LoadoutClientManager manager = LoadoutClientManager.getInstance();
                manager.handleLoadoutApplied(loadoutName);
                
                LogicalLoadouts.LOGGER.debug("Loadout '{}' applied by server", loadoutName);
            } catch (Exception e) {
                LogicalLoadouts.LOGGER.error("Failed to handle loadout applied", e);
            }
        });
    }
}