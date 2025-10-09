package rocamocha.logicalloadouts.client;

import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.client.gui.LoadoutSelectionScreen;
import rocamocha.logicalloadouts.data.Loadout;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Handles keybindings for loadout operations.
 * Provides quick access to loadout management functions.
 */
public class LoadoutKeybindings {
    
    // Keybinding definitions
    private static KeyBinding openLoadoutMenuKey;
    private static KeyBinding quickApplyLoadout1Key;
    private static KeyBinding quickApplyLoadout2Key;
    private static KeyBinding quickApplyLoadout3Key;
    private static KeyBinding quickApplyLoadout4Key;
    private static KeyBinding quickApplyLoadout5Key;
    private static KeyBinding quickSaveToLoadout1Key;
    
    // State tracking
    private static boolean[] keyPressed = new boolean[10]; // Track key states to prevent repeats
    
    /**
     * Register all keybindings and tick events
     */
    public static void registerKeybindings() {
        LogicalLoadouts.LOGGER.info("Registering loadout keybindings");
        
        // Main loadout menu
        openLoadoutMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.logical-loadouts.open_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_L, // Default: L key
            "category.logical-loadouts"
        ));
        
        // Quick apply loadouts (1-5)
        quickApplyLoadout1Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.logical-loadouts.quick_apply_1",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_1, // Default: Numpad 1
            "category.logical-loadouts"
        ));
        
        quickApplyLoadout2Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.logical-loadouts.quick_apply_2", 
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_2, // Default: Numpad 2
            "category.logical-loadouts"
        ));
        
        quickApplyLoadout3Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.logical-loadouts.quick_apply_3",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_3, // Default: Numpad 3
            "category.logical-loadouts"
        ));
        
        quickApplyLoadout4Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.logical-loadouts.quick_apply_4",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_4, // Default: Numpad 4
            "category.logical-loadouts"
        ));
        
        quickApplyLoadout5Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.logical-loadouts.quick_apply_5",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_5, // Default: Numpad 5
            "category.logical-loadouts"
        ));
        
        // Quick save to loadout 1 (Ctrl + Numpad 1)
        quickSaveToLoadout1Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.logical-loadouts.quick_save_1",
            InputUtil.Type.KEYSYM,
            InputUtil.UNKNOWN_KEY.getCode(), // No default key - must be set by user
            "category.logical-loadouts"
        ));
        
        // Register tick event to handle keybinding presses
        ClientTickEvents.END_CLIENT_TICK.register(LoadoutKeybindings::onClientTick);
        
        LogicalLoadouts.LOGGER.info("Registered {} loadout keybindings", 6);
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
        
        // Handle quick apply keys
        if (quickApplyLoadout1Key.wasPressed()) {
            handleQuickApplyLoadout(client, 0);
        }
        if (quickApplyLoadout2Key.wasPressed()) {
            handleQuickApplyLoadout(client, 1);
        }
        if (quickApplyLoadout3Key.wasPressed()) {
            handleQuickApplyLoadout(client, 2);
        }
        if (quickApplyLoadout4Key.wasPressed()) {
            handleQuickApplyLoadout(client, 3);
        }
        if (quickApplyLoadout5Key.wasPressed()) {
            handleQuickApplyLoadout(client, 4);
        }
        
        // Handle quick save key (with Ctrl modifier)
        if (quickSaveToLoadout1Key.wasPressed() && (client.options.sprintKey.isPressed() || isCtrlPressed())) {
            handleQuickSaveToLoadout(client, 0);
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
    
    /**
     * Handle quick apply loadout (now uses global slots for first 3, then regular loadouts)
     */
    private static void handleQuickApplyLoadout(MinecraftClient client, int index) {
        LoadoutClientManager manager = LoadoutClientManager.getInstance();
        
        // First 3 slots are global loadouts (always available)
        if (index < 3) {
            boolean success = manager.applyGlobalLoadout(index);
            if (!success && client.player != null) {
                client.player.sendMessage(net.minecraft.text.Text.literal("§cGlobal Slot " + (index + 1) + " is empty"), true);
            }
            return;
        }
        
        // Remaining slots use regular loadout list
        List<Loadout> loadouts = manager.getAllLoadouts();
        int adjustedIndex = index - 3; // Adjust for global slots
        
        if (adjustedIndex >= 0 && adjustedIndex < loadouts.size()) {
            Loadout loadout = loadouts.get(adjustedIndex);
            manager.applyLoadout(loadout.getId());
            LogicalLoadouts.LOGGER.debug("Quick applied loadout {} via keybinding: {}", index + 1, loadout.getName());
        } else if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§cLoadout slot " + (index + 1) + " is empty"), true);
        }
    }
    
    /**
     * Handle quick save to loadout (always saves to global slot 1 for simplicity)
     */
    private static void handleQuickSaveToLoadout(MinecraftClient client, int index) {
        LoadoutClientManager manager = LoadoutClientManager.getInstance();
        
        // Always save to global slot 1 when using quick save
        manager.saveToGlobalSlot(1, "Quick Save");
        LogicalLoadouts.LOGGER.debug("Quick saved to Global Slot 1 via keybinding");
    }
    
    /**
     * Check if Ctrl key is pressed
     */
    private static boolean isCtrlPressed() {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) ||
               InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}