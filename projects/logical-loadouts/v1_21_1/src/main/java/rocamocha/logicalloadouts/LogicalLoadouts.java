package rocamocha.logicalloadouts;

import rocamocha.logicalloadouts.network.LoadoutNetworking;
import rocamocha.logicalloadouts.server.LoadoutManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LogicalLoadouts implements ModInitializer {
    public static final String MOD_ID = "logical-loadouts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Server-side managers
    private static final Map<MinecraftServer, LoadoutManager> serverManagers = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Logical Loadouts");
        System.out.println("MAIN MOD INITIALIZATION STARTING");
        
        // Register networking (payload types and server handlers)
        System.out.println("About to register networking...");
        LoadoutNetworking.registerNetworking();
        System.out.println("About to register server packets...");
        LoadoutNetworking.registerServerPackets();
        System.out.println("Networking registration completed");
        
        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        
        System.out.println("MAIN MOD INITIALIZATION COMPLETE");
        LOGGER.info("Logical Loadouts initialization complete");
    }
    
    private void onServerStarting(MinecraftServer server) {
        LOGGER.info("Starting loadout manager for server");
        LoadoutManager manager = new LoadoutManager(server);
        serverManagers.put(server, manager);
        
        // Register player join/leave events for this server
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            if (s == server) {
                registerPlayerEvents(server, manager);
            }
        });
    }
    
    private void onServerStopping(MinecraftServer server) {
        LOGGER.info("Stopping loadout manager for server");
        LoadoutManager manager = serverManagers.remove(server);
        if (manager != null) {
            manager.saveAll();
        }
    }
    
    private void registerPlayerEvents(MinecraftServer server, LoadoutManager manager) {
        // Register player join/leave events using Fabric's ServerPlayConnectionEvents
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server1) -> {
            if (server1 == server) { // Only handle events for our server
                UUID playerUuid = handler.getPlayer().getUuid();
                LOGGER.debug("Player {} joined, loading their loadout data", playerUuid);
                manager.loadPlayerData(playerUuid);
            }
        });
        
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server1) -> {
            if (server1 == server) { // Only handle events for our server
                UUID playerUuid = handler.getPlayer().getUuid();
                LOGGER.debug("Player {} left, unloading their loadout data", playerUuid);
                manager.unloadPlayerData(playerUuid);
            }
        });
        
        LOGGER.debug("Player events registered for server");
    }
    
    /**
     * Get the loadout manager for a server
     */
    public static LoadoutManager getServerLoadoutManager(MinecraftServer server) {
        LoadoutManager manager = serverManagers.get(server);
        if (manager == null) {
            throw new IllegalStateException("LoadoutManager not initialized for server");
        }
        return manager;
    }
}