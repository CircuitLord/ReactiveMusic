package rocamocha.logicalloadouts;

import rocamocha.logicalloadouts.client.LoadoutClientManager;
import rocamocha.logicalloadouts.client.LoadoutKeybindings;
import rocamocha.logicalloadouts.client.network.LoadoutClientPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalLoadoutsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(LogicalLoadouts.MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Logical Loadouts Client");
        System.out.println("CLIENT MOD INITIALIZATION STARTING");
        
        // Register client-side networking
        System.out.println("About to register client packets...");
        LoadoutClientPackets.registerClientPackets();
        System.out.println("Client packets registered");
        
        // Register connection events
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LoadoutClientManager.getInstance().setConnectedToServer(true);
        });
        
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LoadoutClientManager.getInstance().setConnectedToServer(false);
        });
        
        // Register keybindings
        LoadoutKeybindings.registerKeybindings();
        
        // Register client tick event for keybinding handling
        // Note: LoadoutKeybindings already registers its own tick event
        
        LOGGER.info("Logical Loadouts Client initialization complete");
    }
}