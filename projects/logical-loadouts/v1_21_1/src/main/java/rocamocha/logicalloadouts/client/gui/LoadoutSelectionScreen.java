package rocamocha.logicalloadouts.client.gui;

import rocamocha.logicalloadouts.client.LoadoutClientManager;
import rocamocha.logicalloadouts.data.Loadout;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Main GUI screen for managing loadouts.
 * Provides interface for creating, deleting, applying, and saving loadouts.
 */
public class LoadoutSelectionScreen extends Screen implements LoadoutClientManager.LoadoutUpdateListener {
    
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 4;
    
    private final LoadoutClientManager manager;
    private List<Loadout> loadouts;
    
    // UI Components
    private LoadoutListWidget loadoutList;
    private ButtonWidget createButton;
    private ButtonWidget deleteButton;
    private ButtonWidget applyButton;
    private ButtonWidget saveButton;
    private ButtonWidget refreshButton;
    private ButtonWidget closeButton;
    private TextFieldWidget nameField;
    
    // State
    private Loadout selectedLoadout = null;
    private boolean showCreateDialog = false;
    
    public LoadoutSelectionScreen() {
        super(Text.literal("Loadout Management"));
        this.manager = LoadoutClientManager.getInstance();
        this.loadouts = manager.getAllLoadouts();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Register as listener for loadout updates
        manager.addListener(this);
        
        int centerX = this.width / 2;
        int startY = 40;
        
        // Layout: 2/3 left for loadout list, 1/3 right for info box
        int leftPanelWidth = (this.width * 2) / 3 - 20; // 2/3 minus some margin
        int rightPanelX = leftPanelWidth + 20; // Start of right panel
        
        // Create loadout list widget - takes up left 2/3 of screen
        int listHeight = this.height - 140; // More space for buttons
        loadoutList = new LoadoutListWidget(this.client, leftPanelWidth, listHeight, startY, 25, this.loadouts, this);
        loadoutList.setX(10); // Start with small margin from left edge
        this.addSelectableChild(loadoutList);
        
        // Button layout - center buttons in the left panel area
        int buttonStartY = startY + listHeight + 10; // Start buttons after list with padding
        int buttonWidth = 90; // Smaller button width
        int buttonSpacing = 10; // Space between buttons
        
        // Calculate total width needed for 4 buttons
        int totalButtonWidth = (buttonWidth * 4) + (buttonSpacing * 3);
        int buttonStartX = centerX - totalButtonWidth / 2;
        
        // Main action buttons (first row)
        applyButton = ButtonWidget.builder(Text.literal("Apply"), button -> applySelectedLoadout())
            .dimensions(buttonStartX, buttonStartY, buttonWidth, BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(applyButton);
        
        saveButton = ButtonWidget.builder(Text.literal("Save Current"), button -> saveCurrentToLoadout())
            .dimensions(buttonStartX + buttonWidth + buttonSpacing, buttonStartY, buttonWidth, BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(saveButton);
        
        createButton = ButtonWidget.builder(Text.literal("Create New"), button -> toggleCreateDialog())
            .dimensions(buttonStartX + (buttonWidth + buttonSpacing) * 2, buttonStartY, buttonWidth, BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(createButton);
        
        deleteButton = ButtonWidget.builder(Text.literal("Delete"), button -> deleteSelectedLoadout())
            .dimensions(buttonStartX + (buttonWidth + buttonSpacing) * 3, buttonStartY, buttonWidth, BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(deleteButton);
        
        // Utility buttons (second row)
        int secondRowY = buttonStartY + BUTTON_HEIGHT + 8;
        int utilButtonWidth = (buttonWidth * 2) + buttonSpacing; // Wider buttons for second row
        int utilButtonStartX = centerX - utilButtonWidth - buttonSpacing / 2;
        
        refreshButton = ButtonWidget.builder(Text.literal("Refresh"), button -> refreshLoadouts())
            .dimensions(utilButtonStartX, secondRowY, utilButtonWidth, BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(refreshButton);
        
        closeButton = ButtonWidget.builder(Text.literal("Close"), button -> this.close())
            .dimensions(utilButtonStartX + utilButtonWidth + buttonSpacing, secondRowY, utilButtonWidth, BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(closeButton);
        
        // Create name input field (initially hidden)
        nameField = new TextFieldWidget(this.textRenderer, centerX - 75, startY + 20, 150, 20, Text.literal("Loadout Name"));
        nameField.setMaxLength(32);
        nameField.setPlaceholder(Text.literal("Enter loadout name"));
        nameField.setVisible(false);
        this.addSelectableChild(nameField);
        
        updateButtonStates();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Different background rendering based on dialog state
        if (showCreateDialog) {
            // Use a simple solid background when dialog is active to prevent blur
            context.fill(0, 0, this.width, this.height, 0xFF1E1E1E);
        } else {
            // Normal background when no dialog
            this.renderBackground(context, mouseX, mouseY, delta);
        }
        
        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw operation mode status
        String statusText = "Mode: " + manager.getOperationMode();
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(statusText), this.width / 2, 30, 0xCCCCCC);
        
        // Note: List and info will be rendered after buttons to avoid blur
        
        // Control button visibility based on dialog state
        setButtonsVisible(!showCreateDialog);
        
        // Render UI elements (buttons) first
        super.render(context, mouseX, mouseY, delta);
        
        // Render loadout list AFTER buttons to ensure it's on top when not in dialog mode
        if (!showCreateDialog) {
            // Re-render the loadout list on top of buttons to avoid blur
            loadoutList.render(context, mouseX, mouseY, delta);
            
            // Re-render selected loadout info on top
            if (selectedLoadout != null) {
                renderLoadoutInfo(context, selectedLoadout);
            }
        }
        
        // If dialog is showing, render it LAST so it appears on top
        if (showCreateDialog) {
            renderCreateDialog(context, mouseX, mouseY);
        }
    }
    
    private void renderCreateDialog(DrawContext context, int mouseX, int mouseY) {
        int dialogWidth = 240;
        int dialogHeight = 100;
        int dialogX = this.width / 2 - dialogWidth / 2;
        int dialogY = this.height / 2 - dialogHeight / 2; // Center the dialog
        
        // Draw completely opaque dialog background with border
        context.fill(dialogX - 3, dialogY - 3, dialogX + dialogWidth + 3, dialogY + dialogHeight + 3, 0xFF000000); // Black border
        context.fill(dialogX - 2, dialogY - 2, dialogX + dialogWidth + 2, dialogY + dialogHeight + 2, 0xFF606060); // Gray border
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF3C3C3C); // Solid dialog background
        
        // Draw dialog title
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Create New Loadout").formatted(Formatting.BOLD), 
                                         this.width / 2, dialogY + 15, 0xFFFFFF);
        
        // Update name field position to be centered in dialog
        nameField.setX(dialogX + (dialogWidth - 150) / 2);
        nameField.setY(dialogY + 35);
        nameField.setVisible(true);
        nameField.render(context, mouseX, mouseY, 0);
        
        // Instructions
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Press Enter to create").formatted(Formatting.ITALIC), 
                                         this.width / 2, dialogY + 65, 0xCCCCCC);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Press Escape to cancel").formatted(Formatting.ITALIC), 
                                         this.width / 2, dialogY + 78, 0xCCCCCC);
    }
    
    private void renderLoadoutInfo(DrawContext context, Loadout loadout) {
        // Position info box in right 1/3 of screen
        int leftPanelWidth = (this.width * 2) / 3 - 20;
        int rightPanelX = leftPanelWidth + 30; // Start of right panel with margin
        int rightPanelWidth = this.width - rightPanelX - 10; // Width of right panel minus margin
        
        int infoX = rightPanelX;
        int infoY = 60;
        int infoWidth = Math.max(150, rightPanelWidth - 20); // Use available width, minimum 150px
        
        // Draw info background
        context.fill(infoX - 5, infoY - 5, infoX + infoWidth + 5, infoY + 60 + 5, 0x40000000);
        
        // Draw loadout info
        context.drawTextWithShadow(this.textRenderer, Text.literal("Selected Loadout:").formatted(Formatting.BOLD), 
                                 infoX, infoY, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal(loadout.getName()), 
                                 infoX, infoY + 12, 0xFFDD00);
        context.drawTextWithShadow(this.textRenderer, Text.literal("ID: " + loadout.getId().toString().substring(0, 8) + "..."), 
                                 infoX, infoY + 24, 0xAAAAAA);
        
        // Last modified
        long timeDiff = System.currentTimeMillis() - loadout.getLastModified();
        String timeText = formatTimeDifference(timeDiff);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Modified: " + timeText), 
                                 infoX, infoY + 36, 0xAAAAAA);
        
        // Quick actions info
        context.drawTextWithShadow(this.textRenderer, Text.literal("Double-click to apply").formatted(Formatting.ITALIC), 
                                 infoX, infoY + 50, 0x888888);
    }
    
    private String formatTimeDifference(long millis) {
        if (millis < 60000) return "Just now";
        if (millis < 3600000) return (millis / 60000) + "m ago";
        if (millis < 86400000) return (millis / 3600000) + "h ago";
        return (millis / 86400000) + "d ago";
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showCreateDialog) {
            if (keyCode == 256) { // Escape
                toggleCreateDialog();
                return true;
            } else if (keyCode == 257) { // Enter
                createLoadout();
                return true;
            } else if (nameField.isFocused()) {
                return nameField.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (showCreateDialog && nameField.isFocused()) {
            return nameField.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }
    
    private void applySelectedLoadout() {
        if (selectedLoadout != null) {
            System.out.println("Applying loadout: " + selectedLoadout.getName());
            
            // In survival mode, check inventory state before applying
            boolean isInSurvival = this.client != null && this.client.player != null && !this.client.player.getAbilities().creativeMode;
            boolean inventoryEmpty = isPlayerInventoryEmpty();
            
            boolean success = manager.applyLoadout(selectedLoadout.getId());
            if (success) {
                System.out.println("Successfully applied loadout: " + selectedLoadout.getName());
                
                if (isInSurvival) {
                    if (inventoryEmpty) {
                        // Empty inventory - consume the loadout (delete it)
                        System.out.println("Player inventory was empty - consuming loadout: " + selectedLoadout.getName());
                        boolean deleteSuccess = manager.deleteLoadout(selectedLoadout.getId());
                        if (deleteSuccess) {
                            System.out.println("Loadout consumed in survival mode: " + selectedLoadout.getName());
                            selectedLoadout = null;
                            updateButtonStates();
                            refreshLoadouts();
                        } else {
                            System.out.println("Failed to delete loadout after use: " + manager.getLastOperationResult());
                        }
                    } else {
                        // Non-empty inventory - swap contents with loadout
                        System.out.println("Player inventory was not empty - swapping contents with loadout");
                        swapInventoryWithLoadout();
                    }
                }
            } else {
                System.out.println("Failed to apply loadout: " + manager.getLastOperationResult());
            }
        } else {
            System.out.println("No loadout selected for apply!");
        }
    }
    
    private void saveCurrentToLoadout() {
        // Always create a new loadout (same as "Create" button) - this works correctly
        String name;
        if (selectedLoadout != null) {
            // Use the selected loadout's name but create a new loadout (overwrite behavior)
            name = selectedLoadout.getName();
            System.out.println("Creating new loadout with existing name: " + name);
        } else {
            // No selection - create a new loadout with timestamp name
            name = "Loadout_" + System.currentTimeMillis();
            System.out.println("Creating new loadout: " + name);
        }
        
        // Use the same method as "Create" button - this works perfectly
        boolean success = manager.createLoadout(name);
        System.out.println("Create loadout result: " + success);
        if (success) {
            System.out.println("Success! Refreshing loadouts.");
            
            // Server-side loadout application will handle inventory changes properly
            System.out.println("Loadout created - inventory clearing will be handled by server-side sync");
            
            refreshLoadouts();
        } else {
            System.out.println("Failed to create loadout: " + manager.getLastOperationResult());
        }
    }
    
    private void deleteSelectedLoadout() {
        if (selectedLoadout != null) {
            System.out.println("Deleting loadout: " + selectedLoadout.getName());
            boolean success = manager.deleteLoadout(selectedLoadout.getId());
            if (success) {
                System.out.println("Successfully deleted loadout: " + selectedLoadout.getName());
                selectedLoadout = null;
                updateButtonStates();
                // Force a refresh to update the list
                refreshLoadouts();
            } else {
                System.out.println("Failed to delete loadout: " + manager.getLastOperationResult());
            }
        } else {
            System.out.println("No loadout selected for delete!");
        }
    }
    
    private void toggleCreateDialog() {
        showCreateDialog = !showCreateDialog;
        nameField.setVisible(showCreateDialog);
        
        if (showCreateDialog) {
            nameField.setFocused(true);
            nameField.setText("");
        } else {
            nameField.setFocused(false);
        }
    }
    
    private void createLoadout() {
        String name = nameField.getText().trim();
        System.out.println("CreateLoadout called with name: '" + name + "'");
        if (!name.isEmpty()) {
            System.out.println("Name is not empty, calling manager.createLoadout...");
            boolean success = manager.createLoadout(name);
            System.out.println("Create loadout result: " + success);
            if (success) {
                System.out.println("Success! Closing dialog and refreshing.");
                
                // Server-side loadout application will handle inventory changes properly
                System.out.println("Loadout created - inventory clearing will be handled by server-side sync");
                
                toggleCreateDialog();
                // Force a refresh to show the new loadout
                refreshLoadouts();
            } else {
                // Show error message - for now just log it
                System.out.println("Failed to create loadout: " + manager.getLastOperationResult());
            }
        } else {
            System.out.println("Name is empty!");
        }
    }
    
    private void refreshLoadouts() {
        // Refresh the loadout list
        onLoadoutsUpdated();
    }
    
    private void updateButtonStates() {
        boolean hasSelection = selectedLoadout != null;
        boolean isConnected = manager.isConnectedToServer();
        
        System.out.println("UpdateButtonStates: hasSelection=" + hasSelection + 
                         ", selectedLoadout=" + (selectedLoadout != null ? selectedLoadout.getName() : "null") +
                         ", totalLoadouts=" + loadouts.size());
        
        // In hybrid mode, allow local operations even when not connected to server
        applyButton.active = hasSelection; // Can apply both local and server loadouts
        saveButton.active = true; // Can always save current inventory (create new or overwrite selected)
        deleteButton.active = hasSelection; // Can delete both local and server loadouts
        createButton.active = true; // Can always create new loadouts (local when offline)
        refreshButton.active = true; // Can always refresh the list
    }
    
    public void setSelectedLoadout(Loadout loadout) {
        System.out.println("LoadoutSelectionScreen: setSelectedLoadout called with: " + 
                         (loadout != null ? loadout.getName() : "null"));
        this.selectedLoadout = loadout;
        updateButtonStates();
    }
    
    /**
     * Public method to apply selected loadout (for use by LoadoutListWidget double-click)
     */
    public void applySelectedLoadoutPublic() {
        applySelectedLoadout();
    }
    
    private void setButtonsVisible(boolean visible) {
        applyButton.visible = visible;
        saveButton.visible = visible;
        createButton.visible = visible;
        deleteButton.visible = visible;
        refreshButton.visible = visible;
        closeButton.visible = visible;
    }
    
    @Override
    public void close() {
        manager.removeListener(this);
        super.close();
    }
    
    /**
     * Checks if the player's inventory is completely empty
     */
    private boolean isPlayerInventoryEmpty() {
        if (this.client == null || this.client.player == null) {
            return true;
        }
        
        // Check hotbar
        for (int i = 0; i < 9; i++) {
            if (!this.client.player.getInventory().getStack(i).isEmpty()) {
                return false;
            }
        }
        
        // Check main inventory
        for (int i = 9; i < 36; i++) {
            if (!this.client.player.getInventory().getStack(i).isEmpty()) {
                return false;
            }
        }
        
        // Check armor slots
        for (int i = 0; i < 4; i++) {
            if (!this.client.player.getInventory().armor.get(i).isEmpty()) {
                return false;
            }
        }
        
        // Check offhand
        if (!this.client.player.getInventory().offHand.get(0).isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Swaps the current player inventory with the selected loadout (used in survival mode)
     */
    private void swapInventoryWithLoadout() {
        if (selectedLoadout != null && this.client != null && this.client.player != null) {
            System.out.println("Swapping inventory contents with loadout: " + selectedLoadout.getName());
            
            // Update the existing loadout with current inventory contents (no copy, same name/ID)
            boolean success = manager.updateLocalLoadout(selectedLoadout.getId());
            if (success) {
                System.out.println("Successfully updated loadout with current inventory contents");
                // The loadout now contains what was in the player's inventory
                // The player's inventory already has the loadout contents from the apply operation
                refreshLoadouts();
            } else {
                System.out.println("Failed to swap - could not update loadout: " + manager.getLastOperationResult());
            }
        }
    }
    
    // LoadoutUpdateListener implementation
    @Override
    public void onLoadoutsUpdated() {
        this.loadouts = manager.getAllLoadouts();
        if (loadoutList != null) {
            loadoutList.updateLoadouts(loadouts);
        }
        updateButtonStates();
    }
}