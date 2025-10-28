package rocamocha.logicalloadouts.network;

import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.network.packets.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Network packet registration for client-server communication using CustomPayload system.
 * All loadout operations go through the server for security and persistence.
 */
public class LoadoutNetworking {
    
    /**
     * Register all CustomPayload types and server-side packet handlers
     */
    public static void registerNetworking() {
        LogicalLoadouts.LOGGER.info("Registering modern CustomPayload networking system");
        System.out.println("LoadoutNetworking.registerNetworking() called");
        
        PayloadTypeRegistry.playC2S().register(CreateLoadoutPayload.ID, CreateLoadoutPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CreateLoadoutFromDataPayload.ID, CreateLoadoutFromDataPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteLoadoutPayload.ID, DeleteLoadoutPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateLoadoutPayload.ID, UpdateLoadoutPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ApplyLoadoutPayload.ID, ApplyLoadoutPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ApplyLocalLoadoutPayload.ID, ApplyLocalLoadoutPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveLoadoutPayload.ID, SaveLoadoutPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestLoadoutsPayload.ID, RequestLoadoutsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ApplySectionPayload.ID, ApplySectionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DepositSectionPayload.ID, DepositSectionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ReloadServerLoadoutsPayload.ID, ReloadServerLoadoutsPayload.CODEC);
        
        // Register payload types for server -> client packets
        PayloadTypeRegistry.playS2C().register(OperationResultPayload.ID, OperationResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LoadoutsSyncPayload.ID, LoadoutsSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LoadoutAppliedPayload.ID, LoadoutAppliedPayload.CODEC);
        
        LogicalLoadouts.LOGGER.info("Registered all CustomPayload types");
    }
    
    /**
     * Register server-side packet handlers
     */
    public static void registerServerPackets() {
        LogicalLoadouts.LOGGER.info("Registering server-side network handlers");
        System.out.println("LoadoutNetworking.registerServerPackets() called");
        
        ServerPlayNetworking.registerGlobalReceiver(CreateLoadoutPayload.ID, LoadoutServerPackets::handleCreateLoadout);
        ServerPlayNetworking.registerGlobalReceiver(CreateLoadoutFromDataPayload.ID, LoadoutServerPackets::handleCreateLoadoutFromData);
        ServerPlayNetworking.registerGlobalReceiver(DeleteLoadoutPayload.ID, LoadoutServerPackets::handleDeleteLoadout);
        ServerPlayNetworking.registerGlobalReceiver(UpdateLoadoutPayload.ID, LoadoutServerPackets::handleUpdateLoadout);
        ServerPlayNetworking.registerGlobalReceiver(ApplyLoadoutPayload.ID, LoadoutServerPackets::handleApplyLoadout);
        ServerPlayNetworking.registerGlobalReceiver(ApplyLocalLoadoutPayload.ID, LoadoutServerPackets::handleApplyLocalLoadout);
        ServerPlayNetworking.registerGlobalReceiver(SaveLoadoutPayload.ID, LoadoutServerPackets::handleSaveLoadout);
        ServerPlayNetworking.registerGlobalReceiver(RequestLoadoutsPayload.ID, LoadoutServerPackets::handleRequestLoadouts);
        ServerPlayNetworking.registerGlobalReceiver(ApplySectionPayload.ID, LoadoutServerPackets::handleApplySection);
        ServerPlayNetworking.registerGlobalReceiver(DepositSectionPayload.ID, LoadoutServerPackets::handleDepositSection);
        ServerPlayNetworking.registerGlobalReceiver(ReloadServerLoadoutsPayload.ID, LoadoutServerPackets::handleReloadServerLoadouts);
        
        LogicalLoadouts.LOGGER.info("Registered all server-side packet handlers");
    }
}