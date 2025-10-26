package rocamocha.logicalloadouts.client;

import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.client.gui.LoadoutSelectionScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Handles keybindings for loadout operations.
 * Provides quick access to loadout management functions.
 */
public class LoadoutKeybindings {
    
    // Keybinding definitions
    private static KeyBinding openLoadoutMenuKey;
    
    /**
     * Register all keybindings and tick events
     */
    public static void registerKeybindings() {
        LogicalLoadouts.LOGGER.info("Registering loadout keybindings");
        
        // Main loadout menu
        openLoadoutMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.logicalloadouts.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_L, // Default: L key
            "key.categories.logicalloadouts"
        ));
        
        // Register tick event to handle keybinding presses
        ClientTickEvents.END_CLIENT_TICK.register(LoadoutKeybindings::onClientTick);
        
        LogicalLoadouts.LOGGER.info("Registered {} loadout keybinding", 1);
    }
    
    /**
     * Handle client tick events to process keybinding presses
     */
    private static void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        
        // Handle open menu key
        if (openLoadoutMenuKey.wasPressed()) {
            handleOpenLoadoutMenu(client);
        }
    }
    
    /**
     * Handle opening the loadout menu
     */
    private static void handleOpenLoadoutMenu(MinecraftClient client) {
        // Always allow opening the loadout menu (works in both local and server mode)
        client.setScreen(new LoadoutSelectionScreen());
        LogicalLoadouts.LOGGER.debug("Opened loadout menu via keybinding");
    }
}