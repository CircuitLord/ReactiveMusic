package rocamocha.logicalloadouts;

import rocamocha.logicalloadouts.network.LoadoutNetworking;
import rocamocha.logicalloadouts.server.LoadoutManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
        
        // Register commands
        registerCommands();
        
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
    
    /**
     * Register server commands
     */
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Create suggestion provider for loadout names
            SuggestionProvider<ServerCommandSource> loadoutSuggestions = (context, builder) -> {
                ServerCommandSource source = context.getSource();
                MinecraftServer server = source.getServer();
                
                try {
                    LoadoutManager manager = getServerLoadoutManager(server);
                    List<rocamocha.logicalloadouts.data.Loadout> serverLoadouts = manager.getServerSharedLoadouts();
                    
                    // Add all loadout names as suggestions
                    for (rocamocha.logicalloadouts.data.Loadout loadout : serverLoadouts) {
                        builder.suggest(loadout.getName());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error providing loadout suggestions", e);
                }
                
                return builder.buildFuture();
            };
            
            // Register /loadout command
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("loadout")
                    .then(net.minecraft.server.command.CommandManager.argument("name", StringArgumentType.greedyString())
                        .suggests(loadoutSuggestions)
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            
                            if (player == null) {
                                source.sendError(Text.literal("This command can only be used by players"));
                                return 0;
                            }
                            
                            String loadoutName = StringArgumentType.getString(context, "name");
                            MinecraftServer server = source.getServer();
                            
                            try {
                                LoadoutManager manager = getServerLoadoutManager(server);
                                List<rocamocha.logicalloadouts.data.Loadout> serverLoadouts = manager.getServerSharedLoadouts();
                                
                                // Find loadout by name (case-insensitive)
                                rocamocha.logicalloadouts.data.Loadout targetLoadout = null;
                                for (rocamocha.logicalloadouts.data.Loadout loadout : serverLoadouts) {
                                    if (loadout.getName().equalsIgnoreCase(loadoutName)) {
                                        targetLoadout = loadout;
                                        break;
                                    }
                                }
                                
                                final rocamocha.logicalloadouts.data.Loadout finalLoadout = targetLoadout;
                                
                                if (finalLoadout == null) {
                                    source.sendError(Text.literal("Server loadout '" + loadoutName + "' not found"));
                                    return 0;
                                }
                                
                                // Apply the loadout
                                finalLoadout.applyToPlayer(player);
                                
                                // Send success message
                                source.sendFeedback(() -> Text.literal("Applied server loadout '" + finalLoadout.getName() + "'"), false);
                                
                                LOGGER.info("Player {} applied server loadout '{}' via command", player.getName().getString(), finalLoadout.getName());
                                
                            } catch (Exception e) {
                                LOGGER.error("Error executing loadout command", e);
                                source.sendError(Text.literal("An error occurred while applying the loadout"));
                                return 0;
                            }
                            
                            return 1;
                        })
                    )
            );
            
            // Register /loadouts command for server administration
            dispatcher.register(
                net.minecraft.server.command.CommandManager.literal("loadouts")
                    .requires(source -> source.hasPermissionLevel(2)) // Requires OP level 2
                    .then(net.minecraft.server.command.CommandManager.literal("reload")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            MinecraftServer server = source.getServer();
                            
                            try {
                                LoadoutManager manager = getServerLoadoutManager(server);
                                
                                // Reload server loadouts
                                manager.reloadServerSharedLoadouts();
                                
                                // Send updated loadout list to all connected clients
                                rocamocha.logicalloadouts.network.LoadoutServerPackets.sendLoadoutsSyncToAllPlayers(server, manager);
                                
                                // Send success message
                                source.sendFeedback(() -> Text.literal("Successfully reloaded server loadouts and updated all clients"), false);
                                
                                LOGGER.info("Server loadouts reloaded via admin command");
                                
                            } catch (Exception e) {
                                LOGGER.error("Error reloading server loadouts", e);
                                source.sendError(Text.literal("An error occurred while reloading server loadouts"));
                                return 0;
                            }
                            
                            return 1;
                        })
                    )
            );
        });
    }
}