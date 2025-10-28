package rocamocha.logicalloadouts.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import rocamocha.logicalloadouts.network.packets.ApplySectionPayload;
import rocamocha.logicalloadouts.network.packets.ApplyLocalLoadoutPayload;
import rocamocha.logicalloadouts.network.packets.DepositSectionPayload;
import rocamocha.logicalloadouts.network.packets.ReloadServerLoadoutsPayload;
import rocamocha.logicalloadouts.network.packets.UploadLoadoutPayload;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import rocamocha.logicalloadouts.client.LoadoutClientManager;
import rocamocha.logicalloadouts.data.Loadout;

import java.util.List;
import java.util.ArrayList;

/**
 * Main GUI screen for managing loadouts.
 * Provides interface for creating, deleting, applying, and saving loadouts.
 */
public class LoadoutSelectionScreen extends Screen implements LoadoutClientManager.LoadoutUpdateListener {
    
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 4;
    
    // Tab system
    public enum LoadoutTab {
        PERSONAL("Loadouts", 0xFF4A90E2),
        INVENTORY("Inventory", 0xFF8A2BE2),
        HOTBAR("Hotbar", 0xFFFF6B35),
        ARMOR("Armor", 0xFFFFD700),
        SERVER("Server", 0xFF50C878);
        
        private final String displayName;
        private final int color;
        
        LoadoutTab(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
        
        public boolean isSectionTab() {
            return this == ARMOR || this == HOTBAR || this == INVENTORY;
        }
    }
    
    private final LoadoutClientManager manager;
    private List<Loadout> personalLoadouts;
    private List<Loadout> serverLoadouts;
    
    // UI Components
    private LoadoutListWidget personalLoadoutList;
    private LoadoutListWidget serverLoadoutList;
    private ButtonWidget createButton;
    private ButtonWidget deleteButton;
    private ButtonWidget applyButton;
    private ButtonWidget saveButton;
    private ButtonWidget refreshButton;
    private ButtonWidget closeButton;
    private TextFieldWidget nameField;
    private CheckboxWidget includeArmorCheckbox;
    private CheckboxWidget includeOffhandCheckbox;
    private CheckboxWidget includeInventoryCheckbox;
    private CheckboxWidget includeHotbarCheckbox;
    
    // State
    private Loadout selectedLoadout = null;
    private LoadoutTab activeTab = LoadoutTab.PERSONAL;
    private boolean showCreateDialog = false;
    private String validationError = null;
    
    public LoadoutSelectionScreen() {
        super(Text.literal("Loadout Management"));
        this.manager = LoadoutClientManager.getInstance();
        this.personalLoadouts = manager.getPersonalLoadouts();
        this.serverLoadouts = new ArrayList<>();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Register as listener for loadout updates
        manager.addListener(this);
        
        int centerX = this.width / 2;
        int startY = 50; // Leave space for tabs above
        
        // Layout: 2/3 left for loadout list, 1/3 right for info box
        int leftPanelWidth = (this.width * 2) / 3 - 20; // 2/3 minus some margin
        int rightPanelX = leftPanelWidth + 20; // Start of right panel
        
        // Calculate dynamic item height based on active tab
        int dynamicItemHeight = getDynamicItemHeight(activeTab);
        
        // Create personal loadout list widget
        int listHeight = this.height - 110; // More space for taller loadout entries and tabs
        personalLoadoutList = new LoadoutListWidget(this.client, leftPanelWidth, listHeight, startY, dynamicItemHeight, this.personalLoadouts, this);
        personalLoadoutList.setX(10); // Start with small margin from left edge
        this.addSelectableChild(personalLoadoutList);
        
        // Create server loadout list widget (only if connected to server)
        if (manager.isConnectedToServer()) {
            serverLoadoutList = new LoadoutListWidget(this.client, leftPanelWidth, listHeight, startY, dynamicItemHeight, this.serverLoadouts, this);
            serverLoadoutList.setX(10);
            this.addSelectableChild(serverLoadoutList);
        }
        
        // Button layout - center buttons in the left panel area
        int buttonStartY = startY + listHeight + 10; // Start buttons after list with padding
        int buttonWidth = 90; // Smaller button width
        int buttonSpacing = 10; // Space between buttons
        
        // Calculate total width needed for 4 buttons
        int totalButtonWidth = (buttonWidth * 4) + (buttonSpacing * 3);
        int buttonStartX = centerX - totalButtonWidth / 2;
        
        // Main action buttons (first row)
        applyButton = ButtonWidget.builder(Text.literal("Apply"), button -> {
            applySelectedLoadout();
            this.close();
        })
            .dimensions(buttonStartX, buttonStartY, buttonWidth, BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(applyButton);
        
        saveButton = ButtonWidget.builder(Text.literal("Deposit"), button -> {
            saveCurrentToLoadout();
            this.close();
        })
            .dimensions(buttonStartX + buttonWidth + buttonSpacing, buttonStartY, buttonWidth, BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(saveButton);
        
        createButton = ButtonWidget.builder(Text.literal("Export"), button -> toggleCreateDialog())
            .dimensions(buttonStartX + (buttonWidth + buttonSpacing) * 2, buttonStartY, buttonWidth, BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(createButton);
        
        deleteButton = ButtonWidget.builder(Text.literal("Delete"), button -> deleteSelectedLoadout())
            .dimensions(buttonStartX + (buttonWidth + buttonSpacing) * 3, buttonStartY, buttonWidth, BUTTON_HEIGHT)
            .build();
        this.addDrawableChild(deleteButton);

        // Inclusion checkboxes
        includeArmorCheckbox = CheckboxWidget.builder(Text.literal("Armor"), this.textRenderer)
            .pos(buttonStartX, buttonStartY)
            .checked(true)
            .build();
        // this.addDrawableChild(includeArmorCheckbox);

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
        
        nameField = new TextFieldWidget(this.textRenderer, centerX - 75, startY + 20, 150, 20, Text.literal("Export Filename"));
        nameField.setMaxLength(32);
        nameField.setPlaceholder(Text.literal("Enter filename (optional)"));
        nameField.setVisible(false);
        this.addSelectableChild(nameField);
        
        // Initialize loadout lists with current data (after buttons are created)
        onLoadoutsUpdated();
        
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
        
        // Draw title and status text AFTER background so they're visible
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        String statusText = "Mode: " + manager.getOperationMode();
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(statusText), this.width / 2, 18, 0xCCCCCC);
        
        // Control button visibility based on dialog state
        setButtonsVisible(!showCreateDialog);
        
        // Render modern-style buttons manually
        if (!showCreateDialog) {
            renderModernButtons(context, mouseX, mouseY);
        }
        
        // Render UI elements (buttons) first - but we'll handle button rendering manually
        // super.render(context, mouseX, mouseY, delta);
        
        // Draw tabs AFTER background and buttons so they appear on top
        if (!showCreateDialog) {
            renderTabs(context, mouseX, mouseY);
        }
        
        // Render active loadout list AFTER buttons to ensure it's on top when not in dialog mode
        if (!showCreateDialog) {
            LoadoutListWidget activeList = getActiveLoadoutList();
            if (activeList != null) {
                activeList.render(context, mouseX, mouseY, delta);
            }
            
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
    
    private void renderTabs(DrawContext context, int mouseX, int mouseY) {
        int startY = 48; // Loadout list start position
        int tabHeight = 18;
        int tabSpacing = 0; // Add spacing between tabs to prevent accidental clicks
        int tabY = startY - tabHeight; // Position tabs so bottom edge meets top of list
        
        // Left-align tabs with the loadout list (starts at x=10)
        int tabStartX = 10;
        int currentTabX = tabStartX;
        
        // Personal tab
        int personalTabWidth = 60;
        boolean personalHovered = mouseX >= currentTabX && mouseX <= currentTabX + personalTabWidth && 
                                mouseY >= tabY && mouseY <= tabY + tabHeight;
        
        int personalColor = activeTab == LoadoutTab.PERSONAL ? LoadoutTab.PERSONAL.getColor() : 
                          (personalHovered ? 0xFF6A90E2 : 0xFF2A2A2A);
        
        context.fill(currentTabX, tabY, currentTabX + personalTabWidth, tabY + tabHeight, personalColor);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Personal"), 
                                         currentTabX + personalTabWidth / 2, tabY + 6, 0xFFFFFF);
        currentTabX += personalTabWidth + tabSpacing;
        
        // Inventory tab
        int inventoryTabWidth = 70;
        boolean inventoryHovered = mouseX >= currentTabX && mouseX <= currentTabX + inventoryTabWidth && 
                                 mouseY >= tabY && mouseY <= tabY + tabHeight;
        
        int inventoryColor = activeTab == LoadoutTab.INVENTORY ? LoadoutTab.INVENTORY.getColor() : 
                           (inventoryHovered ? 0xFFA855E8 : 0xFF2A2A2A);
        
        context.fill(currentTabX, tabY, currentTabX + inventoryTabWidth, tabY + tabHeight, inventoryColor);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Inventory"), 
                                         currentTabX + inventoryTabWidth / 2, tabY + 6, 0xFFFFFF);
        currentTabX += inventoryTabWidth + tabSpacing;
        
        // Hotbar tab
        int hotbarTabWidth = 60;
        boolean hotbarHovered = mouseX >= currentTabX && mouseX <= currentTabX + hotbarTabWidth && 
                              mouseY >= tabY && mouseY <= tabY + tabHeight;
        
        int hotbarColor = activeTab == LoadoutTab.HOTBAR ? LoadoutTab.HOTBAR.getColor() : 
                         (hotbarHovered ? 0xFFFF8B5A : 0xFF2A2A2A);
        
        context.fill(currentTabX, tabY, currentTabX + hotbarTabWidth, tabY + tabHeight, hotbarColor);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Hotbar"), 
                                         currentTabX + hotbarTabWidth / 2, tabY + 6, 0xFFFFFF);
        currentTabX += hotbarTabWidth + tabSpacing;
        
        // Armor tab
        int armorTabWidth = 50;
        boolean armorHovered = mouseX >= currentTabX && mouseX <= currentTabX + armorTabWidth && 
                             mouseY >= tabY && mouseY <= tabY + tabHeight;
        
        int armorColor = activeTab == LoadoutTab.ARMOR ? LoadoutTab.ARMOR.getColor() : 
                        (armorHovered ? 0xFFFFE55C : 0xFF2A2A2A);
        
        context.fill(currentTabX, tabY, currentTabX + armorTabWidth, tabY + tabHeight, armorColor);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Armor"), 
                                         currentTabX + armorTabWidth / 2, tabY + 6, 0xFFFFFF);
        currentTabX += armorTabWidth + tabSpacing;
        
        // Server tab (only if connected to server)
        if (manager.isConnectedToServer()) {
            int serverTabWidth = 60;
            
            boolean serverHovered = mouseX >= currentTabX && mouseX <= currentTabX + serverTabWidth && 
                                  mouseY >= tabY && mouseY <= tabY + tabHeight;
            
            int serverColor = activeTab == LoadoutTab.SERVER ? LoadoutTab.SERVER.getColor() : 
                            (serverHovered ? 0xFF70C888 : 0xFF2A2A2A);
            
            context.fill(currentTabX, tabY, currentTabX + serverTabWidth, tabY + tabHeight, serverColor);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Server"), 
                                             currentTabX + serverTabWidth / 2, tabY + 6, 0xFFFFFF);
            currentTabX += serverTabWidth + tabSpacing;
        }
    }
    
    private void renderModernButtons(DrawContext context, int mouseX, int mouseY) {
        // Button layout - center buttons in the left panel area
        int startY = 50; // Loadout list start position
        int listHeight = this.height - 110;
        int buttonStartY = startY + listHeight + 10; // Start buttons after list with padding
        int buttonWidth = 90; // Smaller button width
        int buttonSpacing = 10; // Space between buttons
        
        // Calculate total width needed for 4 buttons
        int totalButtonWidth = (buttonWidth * 4) + (buttonSpacing * 3);
        int buttonStartX = this.width / 2 - totalButtonWidth / 2;
        
        // Main action buttons (first row)
        renderModernButton(context, applyButton, buttonStartX, buttonStartY, buttonWidth, BUTTON_HEIGHT, mouseX, mouseY);
        renderModernButton(context, saveButton, buttonStartX + buttonWidth + buttonSpacing, buttonStartY, buttonWidth, BUTTON_HEIGHT, mouseX, mouseY);
        renderModernButton(context, createButton, buttonStartX + (buttonWidth + buttonSpacing) * 2, buttonStartY, buttonWidth, BUTTON_HEIGHT, mouseX, mouseY);
        renderModernButton(context, deleteButton, buttonStartX + (buttonWidth + buttonSpacing) * 3, buttonStartY, buttonWidth, BUTTON_HEIGHT, mouseX, mouseY);
        
        // Utility buttons (second row)
        int secondRowY = buttonStartY + BUTTON_HEIGHT + 8;
        int utilButtonWidth = (buttonWidth * 2) + buttonSpacing; // Wider buttons for second row
        int utilButtonStartX = this.width / 2 - utilButtonWidth - buttonSpacing / 2;
        
        renderModernButton(context, refreshButton, utilButtonStartX, secondRowY, utilButtonWidth, BUTTON_HEIGHT, mouseX, mouseY);
        renderModernButton(context, closeButton, utilButtonStartX + utilButtonWidth + buttonSpacing, secondRowY, utilButtonWidth, BUTTON_HEIGHT, mouseX, mouseY);
    }
    
    private void renderModernButton(DrawContext context, ButtonWidget button, int x, int y, int width, int height, int mouseX, int mouseY) {
        if (!button.visible) return;
        
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        boolean active = button.active;
        
        // Choose colors based on state - similar to tabs but with different colors
        int backgroundColor;
        int textColor = 0xFFFFFF;
        
        if (!active) {
            backgroundColor = 0xFF333333; // Dark gray for inactive
            textColor = 0xFF666666; // Dimmed text
        } else if (hovered) {
            backgroundColor = 0xFF5A5A5A; // Lighter gray for hover
        } else {
            backgroundColor = 0xFF404040; // Medium gray for normal
        }
        
        // Draw button background
        context.fill(x, y, x + width, y + height, backgroundColor);
        
        // Draw subtle border
        context.fill(x, y, x + width, y + 1, 0xFF606060); // Top border
        context.fill(x, y + height - 1, x + width, y + height, 0xFF202020); // Bottom border
        context.fill(x, y, x + 1, y + height, 0xFF606060); // Left border
        context.fill(x + width - 1, y, x + width, y + height, 0xFF202020); // Right border
        
        // Draw button text centered
        int textY = y + (height - 8) / 2; // Center text vertically (8 is approximate text height)
        context.drawCenteredTextWithShadow(this.textRenderer, button.getMessage(), x + width / 2, textY, textColor);
    }
    
    private LoadoutListWidget getActiveLoadoutList() {
        switch (activeTab) {
            case PERSONAL:
                return personalLoadoutList;
            case SERVER:
                return serverLoadoutList;
            case ARMOR:
            case HOTBAR:
            case INVENTORY:
                // Section tabs use the personal loadout list but with filtering
                return personalLoadoutList;
            default:
                return personalLoadoutList;
        }
    }
    
    private void renderCreateDialog(DrawContext context, int mouseX, int mouseY) {
        int dialogWidth = 240;
        int dialogHeight = 140; // Increased height to accommodate button and text
        int dialogX = this.width / 2 - dialogWidth / 2;
        int dialogY = this.height / 2 - dialogHeight / 2; // Center the dialog
        
        // Draw completely opaque dialog background with border
        context.fill(dialogX - 3, dialogY - 3, dialogX + dialogWidth + 3, dialogY + dialogHeight + 3, 0xFF000000); // Black border
        context.fill(dialogX - 2, dialogY - 2, dialogX + dialogWidth + 2, dialogY + dialogHeight + 2, 0xFF606060); // Gray border
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF3C3C3C); // Solid dialog background
        
        // Draw dialog title
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Export Loadout").formatted(Formatting.BOLD), 
                                         this.width / 2, dialogY + 15, 0xFFFFFF);
        
        // Update name field position to be centered in dialog
        nameField.setX(dialogX + (dialogWidth - 150) / 2);
        nameField.setY(dialogY + 35);
        nameField.setVisible(true);
        nameField.render(context, mouseX, mouseY, 0);
        
        // Upload button (centered, only if connected to server)
        if (manager.isConnectedToServer()) {
            int buttonWidth = 80;
            int buttonHeight = 20;
            int buttonX = dialogX + (dialogWidth - buttonWidth) / 2; // Center the button
            int buttonY = dialogY + 70; // Position button below name field with space
            
            // Draw Upload button
            boolean uploadHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth && 
                                   mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
            boolean isValid = validationError == null;
            int uploadButtonColor = !isValid ? 0xFF444444 : (uploadHovered ? 0xFF888888 : 0xFF666666);
            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, uploadButtonColor);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Upload").formatted(Formatting.WHITE), 
                                             buttonX + buttonWidth / 2, buttonY + 6, 0xFFFFFF);
        }
        
        // Validation error text (displayed above instructions if there's an error)
        if (validationError != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(validationError).formatted(Formatting.RED), 
                                             this.width / 2, dialogY + 95, 0xFF5555);
        }
        
        // Instructions (moved down to avoid overlap)
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Press Enter to export").formatted(Formatting.ITALIC), 
                                         this.width / 2, dialogY + 110, 0xCCCCCC);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Press Escape to cancel").formatted(Formatting.ITALIC), 
                                         this.width / 2, dialogY + 123, 0xCCCCCC);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check if clicking on tabs first
            int startY = 48; // Loadout list start position
            int tabHeight = 18;
            int tabSpacing = 0; // Add spacing between tabs to prevent accidental clicks
            int tabY = startY - tabHeight; // Position tabs so bottom edge meets top of list
            
            // Left-align tabs with the loadout list (starts at x=10)
            int tabStartX = 10;
            int currentTabX = tabStartX;
            
            // Personal tab
            int personalTabWidth = 60;
            if (mouseX >= currentTabX && mouseX <= currentTabX + personalTabWidth && 
                mouseY >= tabY && mouseY <= tabY + tabHeight) {
                switchToTab(LoadoutTab.PERSONAL);
                return true;
            }
            currentTabX += personalTabWidth + tabSpacing;
            
            // Inventory tab
            int inventoryTabWidth = 70;
            if (mouseX >= currentTabX && mouseX <= currentTabX + inventoryTabWidth && 
                mouseY >= tabY && mouseY <= tabY + tabHeight) {
                switchToTab(LoadoutTab.INVENTORY);
                return true;
            }
            currentTabX += inventoryTabWidth + tabSpacing;
            
            // Hotbar tab
            int hotbarTabWidth = 60;
            if (mouseX >= currentTabX && mouseX <= currentTabX + hotbarTabWidth && 
                mouseY >= tabY && mouseY <= tabY + tabHeight) {
                switchToTab(LoadoutTab.HOTBAR);
                return true;
            }
            currentTabX += hotbarTabWidth + tabSpacing;
            
            // Armor tab
            int armorTabWidth = 50;
            if (mouseX >= currentTabX && mouseX <= currentTabX + armorTabWidth && 
                mouseY >= tabY && mouseY <= tabY + tabHeight) {
                switchToTab(LoadoutTab.ARMOR);
                return true;
            }
            currentTabX += armorTabWidth + tabSpacing;
            
            // Server tab (only if connected to server)
            if (manager.isConnectedToServer()) {
                int serverTabWidth = 60;
                if (mouseX >= currentTabX && mouseX <= currentTabX + serverTabWidth && 
                    mouseY >= tabY && mouseY <= tabY + tabHeight) {
                    switchToTab(LoadoutTab.SERVER);
                    return true;
                }
                currentTabX += serverTabWidth + tabSpacing;
            }
            
            // Check if clicking in the create dialog (when visible)
            if (showCreateDialog) {
                int dialogX = this.width / 2 - 120; // 240/2 = 120
                int dialogY = this.height / 2 - 70; // 140/2 = 70
                int dialogWidth = 240;
                int dialogHeight = 140;
                
                // Check if click is within dialog bounds
                if (mouseX >= dialogX && mouseX <= dialogX + dialogWidth &&
                    mouseY >= dialogY && mouseY <= dialogY + dialogHeight) {
                    
                    // Upload button (centered, only if connected to server)
                    if (manager.isConnectedToServer()) {
                        int buttonWidth = 80;
                        int buttonHeight = 20;
                        int buttonX = dialogX + (dialogWidth - buttonWidth) / 2; // Center the button
                        int buttonY = dialogY + 70; // Position button below name field with space
                        
                        if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                            mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                            // Only allow upload if name is valid
                            if (validationError == null) {
                                uploadLoadout();
                            }
                            return true;
                        }
                    }
                    
                    // Consume the click if it's in the dialog but not on a button
                    return true;
                }
            }
            
            // Check if clicking in the loadout list area - only allow if on active tab
            int listStartY = 50;
            int listHeight = this.height - 110;
            int listX = 10;
            int listWidth = (this.width * 2) / 3 - 20;
            
            if (mouseX >= listX && mouseX <= listX + listWidth && 
                mouseY >= listStartY && mouseY <= listStartY + listHeight) {
                // Click is in the list area - only pass to active widget
                LoadoutListWidget activeList = getActiveLoadoutList();
                if (activeList != null) {
                    return activeList.mouseClicked(mouseX, mouseY, button);
                }
                return true; // Consume the click even if no active list
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void switchToTab(LoadoutTab newTab) {
        if (activeTab != newTab) {
            activeTab = newTab;
            selectedLoadout = null; // Clear selection when switching tabs
            updateButtonStates();
            
            // Recreate the loadout list widget with appropriate height for the new tab
            recreateLoadoutListWidget();
            
            // Refresh loadouts to apply filtering for the new tab
            onLoadoutsUpdated();
        }
    }
    
    /**
     * Recreate the loadout list widget with dynamic height based on active tab
     */
    private void recreateLoadoutListWidget() {
        // Remove existing widgets
        if (personalLoadoutList != null) {
            this.remove(personalLoadoutList);
        }
        if (serverLoadoutList != null) {
            this.remove(serverLoadoutList);
        }
        
        int startY = 50; // Leave space for tabs above
        
        // Layout: 2/3 left for loadout list, 1/3 right for info box
        int leftPanelWidth = (this.width * 2) / 3 - 20; // 2/3 minus some margin
        
        // Calculate dynamic item height based on active tab
        int dynamicItemHeight = getDynamicItemHeight(activeTab);
        
        // Create personal loadout list widget with dynamic height
        int listHeight = this.height - 110; // More space for taller loadout entries and tabs
        personalLoadoutList = new LoadoutListWidget(this.client, leftPanelWidth, listHeight, startY, dynamicItemHeight, this.personalLoadouts, this);
        personalLoadoutList.setX(10); // Start with small margin from left edge
        this.addSelectableChild(personalLoadoutList);
        
        // Create server loadout list widget (only if connected to server)
        if (manager.isConnectedToServer()) {
            serverLoadoutList = new LoadoutListWidget(this.client, leftPanelWidth, listHeight, startY, dynamicItemHeight, this.serverLoadouts, this);
            serverLoadoutList.setX(10);
            this.addSelectableChild(serverLoadoutList);
        }
    }
    
    /**
     * Get the appropriate item height for a given tab
     */
    private int getDynamicItemHeight(LoadoutTab tab) {
        if (tab.isSectionTab()) {
            // Section tabs only need space for one section
            switch (tab) {
                case ARMOR:
                case HOTBAR:
                    return 32; // Single row of items
                case INVENTORY:
                    return 96; // 3 rows of inventory items
                default:
                    return 32;
            }
        } else {
            // Full tabs (Personal/Server) need space for all sections
            return 134;
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showCreateDialog) {
            if (keyCode == 257) { // Enter key
                // Only allow upload if name is valid
                if (validationError == null) {
                    uploadLoadout();
                }
                return true;
            } else if (keyCode == 256) { // Escape key
                toggleCreateDialog();
                return true;
            } else if (nameField.isFocused()) {
                boolean result = nameField.keyPressed(keyCode, scanCode, modifiers);
                // Re-validate after any key press that might modify text (backspace, delete, etc.)
                validateUploadName();
                return result;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (showCreateDialog && nameField.isFocused()) {
            boolean result = nameField.charTyped(chr, modifiers);
            validateUploadName(); // Validate after each character
            return result;
        }
        return super.charTyped(chr, modifiers);
    }
    
    private void applySelectedLoadout() {
        if (selectedLoadout == null) {
            System.out.println("No loadout selected for apply!");
            return;
        }
        
        if (this.client == null || this.client.player == null) {
            System.out.println("Client or player is null!");
            return;
        }
        
        // Check if we're in a section tab - use section-specific application
        if (activeTab.isSectionTab()) {
            applySectionLoadout();
            return;
        }
        
        // Full loadout application logic
        System.out.println("Applying full loadout: " + selectedLoadout.getName());
        
        boolean success = false;
        
        // Check if we're in multiplayer - if so, always send to server for proper application
        if (manager.isConnectedToServer()) {
            System.out.println("In multiplayer - sending loadout to server for application");
            // Personal loadouts should be consumed when applied to empty inventory, just like in single player
            boolean inventoryEmpty = isPlayerInventoryEmpty();
            ApplyLocalLoadoutPayload payload = new ApplyLocalLoadoutPayload(selectedLoadout, inventoryEmpty);
            ClientPlayNetworking.send(payload);
            success = true; // Assume success, server will handle the actual application
            
            // If inventory was empty, the loadout will be consumed on the server side
            if (inventoryEmpty) {
                selectedLoadout = null;
                updateButtonStates();
                refreshLoadouts();
            }
        } else {
            // Single player - handle locally
            boolean inventoryEmpty = isPlayerInventoryEmpty();
            
            if (inventoryEmpty) {
                // Empty inventory - apply loadout items directly to inventory
                System.out.println("Player inventory was empty - applying loadout directly");
                success = applyLoadoutToInventory(selectedLoadout);
                if (success) {
                    // Delete the loadout after consuming it
                    boolean deleteSuccess = manager.deleteLoadout(selectedLoadout.getId());
                    if (deleteSuccess) {
                        System.out.println("Loadout consumed: " + selectedLoadout.getName());
                        selectedLoadout = null;
                        updateButtonStates();
                        refreshLoadouts();
                    } else {
                        System.out.println("Failed to delete loadout after use: " + manager.getLastOperationResult());
                    }
                }
            } else {
                // Non-empty inventory - swap contents with loadout
                System.out.println("Player inventory was not empty - swapping contents with loadout");
                success = swapInventoryWithLoadout();
            }
        }
        
        if (!success) {
            System.out.println("Failed to apply loadout: " + manager.getLastOperationResult());
        }
    }
    
    private void applySectionLoadout() {
        if (selectedLoadout == null || this.client == null || this.client.player == null) {
            System.out.println("No loadout selected or client/player is null!");
            return;
        }

        System.out.println("Applying " + activeTab.getDisplayName() + " section from loadout: " + selectedLoadout.getName());

        // Check if connected to server (multiplayer)
        boolean isConnectedToServer = manager.isConnectedToServer();

        if (isConnectedToServer) {
            // In multiplayer: send to server only, let server handle and sync back
            System.out.println("Connected to server - sending section apply request to server");
            String sectionName = activeTab.name().toLowerCase();
            ApplySectionPayload payload = new ApplySectionPayload(selectedLoadout.getId(), sectionName);
            ClientPlayNetworking.send(payload);
        } else {
            // In single-player: perform immediate local update
            System.out.println("Single-player mode - performing local section apply");
            swapSectionWithLoadoutProperly(selectedLoadout, activeTab);
        }

        this.close(); // Close the GUI after applying
    }
    
    /**
     * Applies items from the loadout section to the player's inventory
     */
    private void applySectionItems(Loadout loadout, LoadoutTab section) {
        switch (section) {
            case ARMOR:
                // Apply armor items
                for (int i = 0; i < 4 && i < loadout.getArmor().length; i++) {
                    net.minecraft.item.ItemStack armorItem = loadout.getArmor()[i];
                    if (!armorItem.isEmpty()) {
                        this.client.player.getInventory().armor.set(i, armorItem.copy());
                    }
                }
                System.out.println("Applied armor section from loadout");
                break;
                
            case HOTBAR:
                // Apply hotbar items
                for (int i = 0; i < 9 && i < loadout.getHotbar().length; i++) {
                    net.minecraft.item.ItemStack hotbarItem = loadout.getHotbar()[i];
                    if (!hotbarItem.isEmpty()) {
                        this.client.player.getInventory().setStack(i, hotbarItem.copy());
                    }
                }
                System.out.println("Applied hotbar section from loadout");
                break;
                
            case INVENTORY:
                // Apply inventory items (3x9 grid)
                net.minecraft.item.ItemStack[] inventoryItems = loadout.getMainInventory();
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 9; col++) {
                        int slotIndex = row * 9 + col;
                        int inventorySlot = 9 + slotIndex; // Main inventory starts at slot 9
                        if (slotIndex < inventoryItems.length && inventorySlot < 36) {
                            net.minecraft.item.ItemStack inventoryItem = inventoryItems[slotIndex];
                            if (!inventoryItem.isEmpty()) {
                                this.client.player.getInventory().setStack(inventorySlot, inventoryItem.copy());
                            }
                        }
                    }
                }
                System.out.println("Applied inventory section from loadout");
                break;
                
            default:
                // Should not happen
                System.out.println("ERROR: applySectionItems called with non-section tab: " + section);
                break;
        }
    }
    
    /**
     * Properly swaps a section between the player and loadout (preserves both sets of items)
     */
    private void swapSectionWithLoadoutProperly(Loadout loadout, LoadoutTab section) {
        if (loadout == null || this.client == null || this.client.player == null) {
            return;
        }

        System.out.println("Swapping " + section.getDisplayName() + " section with loadout: " + loadout.getName());

        // Store the player's current items in this section
        ItemStack[] playerItems = new ItemStack[0];
        switch (section) {
            case ARMOR:
                playerItems = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    playerItems[i] = this.client.player.getInventory().getArmorStack(i).copy();
                }
                break;
            case HOTBAR:
                playerItems = new ItemStack[9];
                for (int i = 0; i < 9; i++) {
                    playerItems[i] = this.client.player.getInventory().getStack(i).copy();
                }
                break;
            case INVENTORY:
                playerItems = new ItemStack[27];
                for (int i = 0; i < 27; i++) {
                    playerItems[i] = this.client.player.getInventory().getStack(i + 9).copy();
                }
                break;
        }

        // Apply loadout items to player
        switch (section) {
            case ARMOR:
                for (int i = 0; i < 4; i++) {
                    this.client.player.getInventory().armor.set(i, loadout.getArmor()[i].copy());
                }
                break;
            case HOTBAR:
                for (int i = 0; i < 9; i++) {
                    this.client.player.getInventory().setStack(i, loadout.getHotbar()[i].copy());
                }
                break;
            case INVENTORY:
                for (int i = 0; i < 27; i++) {
                    this.client.player.getInventory().setStack(i + 9, loadout.getMainInventory()[i].copy());
                }
                break;
        }

        // Update loadout with player's original items
        switch (section) {
            case ARMOR:
                for (int i = 0; i < 4; i++) {
                    loadout.setArmorSlot(i, playerItems[i]);
                }
                break;
            case HOTBAR:
                for (int i = 0; i < 9; i++) {
                    loadout.setHotbarSlot(i, playerItems[i]);
                }
                break;
            case INVENTORY:
                for (int i = 0; i < 27; i++) {
                    loadout.setMainInventorySlot(i, playerItems[i]);
                }
                break;
        }

        // Update the loadout in local storage
        boolean updateSuccess = manager.updateLoadout(loadout);
        if (updateSuccess) {
            System.out.println("Successfully swapped " + section.getDisplayName() + " section with loadout");
            
            // Check if the loadout is now empty and delete it if so
            if (isLoadoutEmpty(loadout)) {
                boolean deleteSuccess = manager.deleteLoadout(loadout.getId());
                if (deleteSuccess) {
                    System.out.println("Loadout became empty after section swap, successfully deleted: " + loadout.getName());
                } else {
                    System.out.println("Failed to delete empty loadout after section swap: " + manager.getLastOperationResult());
                }
            }
        } else {
            System.out.println("Failed to update loadout during section swap: " + manager.getLastOperationResult());
        }

        refreshLoadouts();
    }
    
    private void saveCurrentToLoadout() {
        if (this.client == null || this.client.player == null) {
            System.out.println("Client or player is null!");
            return;
        }
        
        // Check if we're in a section tab - use section-specific saving
        if (activeTab.isSectionTab()) {
            saveSectionToLoadout();
            return;
        }
        
        // Personal tab logic
        if (selectedLoadout != null) {
            // Update existing loadout by depositing into empty sections
            updateLoadoutWithEmptySections(selectedLoadout);
        } else {
            // No selection - create a new loadout with timestamp name
            String name = "Loadout_" + System.currentTimeMillis();
            System.out.println("Creating new loadout: " + name);
            
            // Create a new loadout
            boolean success = manager.createLoadout(name);
            System.out.println("Create personal loadout result: " + success);
            
            if (success) {
                System.out.println("Success! Created loadout: " + name);
                
                // Clear the player's entire inventory after successfully creating the loadout
                this.client.player.getInventory().clear();
                for (int i = 0; i < 4; i++) {
                    this.client.player.getInventory().armor.set(i, net.minecraft.item.ItemStack.EMPTY);
                }
                this.client.player.getInventory().offHand.set(0, net.minecraft.item.ItemStack.EMPTY);
                
                refreshLoadouts();
            } else {
                System.out.println("Failed to create loadout: " + manager.getLastOperationResult());
            }
        }
    }
    
    /**
     * Update a loadout by depositing player items into its empty sections
     */
    private void updateLoadoutWithEmptySections(Loadout loadout) {
        if (this.client == null || this.client.player == null) {
            return;
        }
        
        System.out.println("Updating loadout '" + loadout.getName() + "' by depositing into empty sections");
        
        boolean updated = false;
        
        // Check and update armor section if empty
        if (checkLoadoutSectionEmpty(loadout, LoadoutTab.ARMOR)) {
            System.out.println("Depositing armor into loadout");
            for (int i = 0; i < 4; i++) {
                net.minecraft.item.ItemStack playerArmor = this.client.player.getInventory().armor.get(i);
                if (!playerArmor.isEmpty()) {
                    loadout.setArmorSlot(i, playerArmor.copy());
                    this.client.player.getInventory().armor.set(i, net.minecraft.item.ItemStack.EMPTY);
                    updated = true;
                }
            }
        }
        
        // Check and update hotbar section if empty
        if (checkLoadoutSectionEmpty(loadout, LoadoutTab.HOTBAR)) {
            System.out.println("Depositing hotbar into loadout");
            for (int i = 0; i < 9; i++) {
                net.minecraft.item.ItemStack playerHotbar = this.client.player.getInventory().getStack(i);
                if (!playerHotbar.isEmpty()) {
                    loadout.setHotbarSlot(i, playerHotbar.copy());
                    this.client.player.getInventory().setStack(i, net.minecraft.item.ItemStack.EMPTY);
                    updated = true;
                }
            }
        }
        
        // Check and update inventory section if empty
        if (checkLoadoutSectionEmpty(loadout, LoadoutTab.INVENTORY)) {
            System.out.println("Depositing inventory into loadout");
            for (int i = 0; i < 27; i++) {
                net.minecraft.item.ItemStack playerInventory = this.client.player.getInventory().getStack(i + 9);
                if (!playerInventory.isEmpty()) {
                    loadout.setMainInventorySlot(i, playerInventory.copy());
                    this.client.player.getInventory().setStack(i + 9, net.minecraft.item.ItemStack.EMPTY);
                    updated = true;
                }
            }
        }
        
        if (updated) {
            // Update the loadout in storage
            boolean success = manager.updateLoadout(loadout);
            if (success) {
                System.out.println("Successfully updated loadout with deposited items");
                refreshLoadouts();
            } else {
                System.out.println("Failed to update loadout: " + manager.getLastOperationResult());
            }
        } else {
            System.out.println("No empty sections found in loadout to deposit into");
        }
    }
    
    /**
     * Public method to check if a loadout section is empty (for context menu use)
     */
    public boolean checkLoadoutSectionEmpty(Loadout loadout, LoadoutTab section) {
        switch (section) {
            case ARMOR:
                for (int i = 0; i < 4; i++) {
                    if (!loadout.getArmor()[i].isEmpty()) {
                        return false;
                    }
                }
                return true;
                
            case HOTBAR:
                for (int i = 0; i < 9; i++) {
                    if (!loadout.getHotbar()[i].isEmpty()) {
                        return false;
                    }
                }
                return true;
                
            case INVENTORY:
                for (int i = 0; i < 27; i++) {
                    if (!loadout.getMainInventory()[i].isEmpty()) {
                        return false;
                    }
                }
                return true;
                
            default:
                return true;
        }
    }
    
    /**
     * Public method to check if player has items in a section (for context menu use)
     */
    public boolean checkPlayerHasSectionItems(LoadoutTab section) {
        if (this.client == null || this.client.player == null) {
            return false;
        }
        
        switch (section) {
            case ARMOR:
                for (int i = 0; i < 4; i++) {
                    if (!this.client.player.getInventory().getArmorStack(i).isEmpty()) {
                        return true;
                    }
                }
                return false;
                
            case HOTBAR:
                for (int i = 0; i < 9; i++) {
                    if (!this.client.player.getInventory().getStack(i).isEmpty()) {
                        return true;
                    }
                }
                return false;
                
            case INVENTORY:
                for (int i = 9; i < 36; i++) {
                    if (!this.client.player.getInventory().getStack(i).isEmpty()) {
                        return true;
                    }
                }
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Check if a loadout is completely empty (all sections are empty)
     */
    private boolean isLoadoutEmpty(Loadout loadout) {
        return checkLoadoutSectionEmpty(loadout, LoadoutTab.ARMOR) &&
               checkLoadoutSectionEmpty(loadout, LoadoutTab.HOTBAR) &&
               checkLoadoutSectionEmpty(loadout, LoadoutTab.INVENTORY) &&
               (loadout.getOffhand().length == 0 || loadout.getOffhand()[0].isEmpty());
    }
    
    private void saveSectionToLoadout() {
        if (this.client == null || this.client.player == null) {
            System.out.println("Client or player is null!");
            return;
        }
        
        // Check if the section has any items before creating the loadout
        if (!checkPlayerHasSectionItems(activeTab)) {
            System.out.println("Cannot create loadout from empty " + activeTab.getDisplayName() + " section");
            System.out.println("CLIENT: Section check failed for " + activeTab.getDisplayName());
            return;
        }
        
        // Determine the name for the loadout
        String name = activeTab.getDisplayName() + "_Loadout_" + System.currentTimeMillis();
        System.out.println("Creating new " + activeTab.getDisplayName() + " loadout: " + name);
        
        // Create a Loadout object with only the section items
        Loadout sectionLoadout = new Loadout(name);
        
        // Check if connected to server (multiplayer)
        boolean isConnectedToServer = manager.isConnectedToServer();
        
        switch (activeTab) {
            case ARMOR:
                // Copy armor items to the loadout
                for (int i = 0; i < 4; i++) {
                    net.minecraft.item.ItemStack armorItem = this.client.player.getInventory().armor.get(i);
                    if (!armorItem.isEmpty()) {
                        sectionLoadout.setArmorSlot(i, armorItem.copy());
                    }
                }
                // Clear armor slots after depositing (only in single-player)
                if (!isConnectedToServer) {
                    for (int i = 0; i < 4; i++) {
                        this.client.player.getInventory().armor.set(i, net.minecraft.item.ItemStack.EMPTY);
                    }
                }
                break;
                
            case HOTBAR:
                // Copy hotbar items to the loadout
                for (int i = 0; i < 9; i++) {
                    net.minecraft.item.ItemStack hotbarItem = this.client.player.getInventory().getStack(i);
                    if (!hotbarItem.isEmpty()) {
                        sectionLoadout.setHotbarSlot(i, hotbarItem.copy());
                    }
                }
                // Clear hotbar slots after depositing (only in single-player)
                if (!isConnectedToServer) {
                    for (int i = 0; i < 9; i++) {
                        this.client.player.getInventory().setStack(i, net.minecraft.item.ItemStack.EMPTY);
                    }
                }
                break;
                
            case INVENTORY:
                // Copy main inventory items to the loadout
                for (int i = 0; i < 27; i++) {
                    net.minecraft.item.ItemStack inventoryItem = this.client.player.getInventory().getStack(i + 9);
                    if (!inventoryItem.isEmpty()) {
                        sectionLoadout.setMainInventorySlot(i, inventoryItem.copy());
                    }
                }
                // Clear inventory slots after depositing (only in single-player)
                if (!isConnectedToServer) {
                    for (int i = 0; i < 27; i++) {
                        this.client.player.getInventory().setStack(i + 9, net.minecraft.item.ItemStack.EMPTY);
                    }
                }
                break;
                
            default:
                // This should never happen since this method is only called for section tabs
                System.out.println("ERROR: saveSectionToLoadout called with non-section tab: " + activeTab);
                return;
        }
        
        System.out.println("DEBUG: Created loadout with ID: " + sectionLoadout.getId());
        
        // Create the loadout with section-only data
        boolean success = manager.createLoadoutFromData(sectionLoadout);
        if (!success) {
            System.out.println("Failed to create section loadout: " + manager.getLastOperationResult());
            return;
        }
        
        refreshLoadouts();
    }
    
    /**
     * Clear a section in a loadout (set all slots to empty)
     */
    private void clearLoadoutSection(Loadout loadout, LoadoutTab section) {
        switch (section) {
            case ARMOR:
                for (int i = 0; i < 4; i++) {
                    loadout.setArmorSlot(i, net.minecraft.item.ItemStack.EMPTY);
                }
                break;
                
            case HOTBAR:
                for (int i = 0; i < 9; i++) {
                    loadout.setHotbarSlot(i, net.minecraft.item.ItemStack.EMPTY);
                }
                break;
                
            case INVENTORY:
                for (int i = 0; i < 27; i++) {
                    loadout.setMainInventorySlot(i, net.minecraft.item.ItemStack.EMPTY);
                }
                break;
                
            default:
                System.out.println("WARNING: clearLoadoutSection called with non-section tab: " + section);
                break;
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
            validationError = null; // Reset validation when opening dialog
        } else {
            nameField.setFocused(false);
            validationError = null; // Clear validation when closing
        }
    }
    
    private void exportLoadout() {
        if (selectedLoadout == null) {
            System.out.println("No loadout selected for export!");
            return;
        }
        
        String filename = nameField.getText().trim();
        if (filename.isEmpty()) {
            filename = selectedLoadout.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
        }
        
        System.out.println("Exporting loadout: " + selectedLoadout.getName() + " to file: " + filename + ".nbt");
        
        // Export the loadout to the exported directory
        boolean success = manager.exportLoadout(selectedLoadout.getId(), filename);
        if (success) {
            System.out.println("Successfully exported loadout: " + selectedLoadout.getName());
            toggleCreateDialog();
        } else {
            System.out.println("Failed to export loadout: " + manager.getLastOperationResult());
        }
    }
    
    private void validateUploadName() {
        if (nameField == null || nameField.getText().isEmpty()) {
            validationError = "Loadout name cannot be empty";
            return;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            validationError = "Loadout name cannot be empty";
            return;
        }

        // Check for invalid characters
        if (!name.matches("[a-zA-Z0-9_\\- ]+")) {
            validationError = "Loadout name can only contain letters, numbers, spaces, underscores, and hyphens";
            return;
        }

        // Check length
        if (name.length() > 50) {
            validationError = "Loadout name cannot be longer than 50 characters";
            return;
        }

        // Check for duplicate names with server loadouts
        List<Loadout> serverLoadouts = LoadoutClientManager.getInstance().getLoadouts(LoadoutClientManager.LoadoutType.SERVER);
        for (Loadout existingServerLoadout : serverLoadouts) {
            if (existingServerLoadout.getName().equalsIgnoreCase(name)) {
                validationError = "A server loadout with this name already exists";
                return;
            }
        }

        validationError = null; // Valid
    }
    
    private void uploadLoadout() {
        if (selectedLoadout == null) {
            System.out.println("No loadout selected for upload!");
            return;
        }
        
        if (!manager.isConnectedToServer()) {
            System.out.println("Cannot upload loadout - not connected to server!");
            return;
        }
        
        String loadoutName = nameField.getText().trim();
        if (loadoutName.isEmpty()) {
            loadoutName = selectedLoadout.getName();
        }
        
        // Check for duplicate names in existing server loadouts
        for (Loadout existingServerLoadout : serverLoadouts) {
            if (existingServerLoadout.getName().equalsIgnoreCase(loadoutName)) {
                System.out.println("Cannot upload loadout - a server loadout with name '" + loadoutName + "' already exists!");
                // Could show a message to the user here, but for now just log and return
                return;
            }
        }
        
        System.out.println("Uploading loadout: " + selectedLoadout.getName() + " as: " + loadoutName);
        
        // Send upload request to server
        UploadLoadoutPayload payload = new UploadLoadoutPayload(selectedLoadout, loadoutName);
        ClientPlayNetworking.send(payload);
        
        // Close the dialog after sending the upload request
        toggleCreateDialog();
    }
    
    private void refreshLoadouts() {
        // If connected to server, request server to reload server loadouts
        if (manager.isConnectedToServer()) {
            System.out.println("Connected to server - requesting server to reload loadouts");
            ReloadServerLoadoutsPayload payload = new ReloadServerLoadoutsPayload();
            ClientPlayNetworking.send(payload);
        } else {
            System.out.println("Single-player mode - refreshing local loadouts");
        }
        
        // Refresh the loadout list (will be updated when server responds or immediately in single-player)
        onLoadoutsUpdated();
    }
    
    private void updateButtonStates() {
        boolean hasSelection = selectedLoadout != null;
        List<Loadout> activeLoadouts = activeTab == LoadoutTab.PERSONAL ? personalLoadouts : serverLoadouts;
        
        System.out.println("UpdateButtonStates: hasSelection=" + hasSelection + 
                         ", selectedLoadout=" + (selectedLoadout != null ? selectedLoadout.getName() : "null") +
                         ", activeTab=" + activeTab + ", totalLoadouts=" + activeLoadouts.size());
        
        // In hybrid mode, allow local operations even when not connected to server
        applyButton.active = hasSelection; // Can apply both local and server loadouts
        saveButton.active = activeTab == LoadoutTab.PERSONAL || activeTab.isSectionTab(); // Can save to personal loadouts and create section loadouts
        deleteButton.active = hasSelection && activeTab == LoadoutTab.PERSONAL && !activeTab.isSectionTab(); // Can only delete personal loadouts, not in section tabs
        createButton.active = hasSelection && activeTab == LoadoutTab.PERSONAL && !activeTab.isSectionTab(); // Can only export personal loadouts, not in section tabs
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
    
    /**
     * Public method to apply a specific section from a loadout (for context menu use)
     */
    public void applySectionFromLoadout(Loadout loadout, LoadoutTab section) {
        if (loadout == null || this.client == null || this.client.player == null) {
            System.out.println("Loadout is null or client/player is null!");
            return;
        }

        System.out.println("Applying " + section.getDisplayName() + " section from loadout: " + loadout.getName());

        // Check if connected to server (multiplayer)
        boolean isConnectedToServer = manager.isConnectedToServer();

        if (isConnectedToServer) {
            // In multiplayer: send to server only, let server handle and sync back
            System.out.println("Connected to server - sending section apply request to server");
            String sectionName = section.name().toLowerCase();
            ApplySectionPayload payload = new ApplySectionPayload(loadout.getId(), sectionName);
            ClientPlayNetworking.send(payload);
        } else {
            // In single-player: perform immediate local update
            System.out.println("Single-player mode - performing local section apply");
            swapSectionWithLoadoutProperly(loadout, section);
        }
    }
    
    /**
     * Locally deposit a specific section from player inventory into a loadout (for immediate visual feedback)
     */
    private void depositSectionIntoLoadoutLocally(Loadout loadout, LoadoutTab section) {
        if (loadout == null || this.client == null || this.client.player == null) {
            return;
        }

        boolean deposited = false;

        switch (section) {
            case ARMOR:
                for (int i = 0; i < 4; i++) {
                    net.minecraft.item.ItemStack playerArmor = this.client.player.getInventory().getArmorStack(i);
                    if (!playerArmor.isEmpty()) {
                        loadout.setArmorSlot(i, playerArmor.copy());
                        this.client.player.getInventory().armor.set(i, net.minecraft.item.ItemStack.EMPTY);
                        deposited = true;
                    }
                }
                break;

            case HOTBAR:
                for (int i = 0; i < 9; i++) {
                    net.minecraft.item.ItemStack playerHotbar = this.client.player.getInventory().getStack(i);
                    if (!playerHotbar.isEmpty()) {
                        loadout.setHotbarSlot(i, playerHotbar.copy());
                        this.client.player.getInventory().setStack(i, net.minecraft.item.ItemStack.EMPTY);
                        deposited = true;
                    }
                }
                break;

            case INVENTORY:
                for (int i = 0; i < 27; i++) {
                    net.minecraft.item.ItemStack playerInventory = this.client.player.getInventory().getStack(i + 9);
                    if (!playerInventory.isEmpty()) {
                        loadout.setMainInventorySlot(i, playerInventory.copy());
                        this.client.player.getInventory().setStack(i + 9, net.minecraft.item.ItemStack.EMPTY);
                        deposited = true;
                    }
                }
                break;

            default:
                System.out.println("ERROR: depositSectionIntoLoadoutLocally called with non-section tab: " + section);
                return;
        }

        if (deposited) {
            boolean success = manager.updateLoadout(loadout);
            if (success) {
                System.out.println("Successfully deposited " + section.getDisplayName() + " section into loadout locally");
                refreshLoadouts();
            } else {
                System.out.println("Failed to update loadout after local deposit: " + manager.getLastOperationResult());
            }
        } else {
            System.out.println("No items to deposit in " + section.getDisplayName() + " section");
        }
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
     * Checks if the player's entire inventory (hotbar + main inventory + armor + offhand) is completely empty
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
        
        // Check armor
        for (int i = 0; i < 4; i++) {
            if (!this.client.player.getInventory().getArmorStack(i).isEmpty()) {
                return false;
            }
        }
        
        // Check offhand
        if (!this.client.player.getInventory().getStack(40).isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Applies a loadout directly to the player's inventory (puts real items in slots)
     */
    private boolean applyLoadoutToInventory(Loadout loadout) {
        if (loadout == null || this.client == null || this.client.player == null) {
            return false;
        }
        
        System.out.println("Applying loadout directly to inventory: " + loadout.getName());
        
        // Apply loadout items to player inventory slots (making them "real" items)
        for (int i = 0; i < Loadout.HOTBAR_SIZE; i++) {
            ItemStack stack = loadout.getHotbar()[i];
            if (stack == null) stack = ItemStack.EMPTY;
            this.client.player.getInventory().setStack(i, stack.copy());
        }
        for (int i = 0; i < Loadout.MAIN_INVENTORY_SIZE; i++) {
            ItemStack stack = loadout.getMainInventory()[i];
            if (stack == null) stack = ItemStack.EMPTY;
            this.client.player.getInventory().setStack(i + Loadout.HOTBAR_SIZE, stack.copy());
        }
        for (int i = 0; i < Loadout.ARMOR_SIZE; i++) {
            ItemStack stack = loadout.getArmor()[i];
            if (stack == null) stack = ItemStack.EMPTY;
            this.client.player.getInventory().armor.set(i, stack.copy());
        }
        for (int i = 0; i < Loadout.OFFHAND_SIZE; i++) {
            ItemStack stack = loadout.getOffhand()[i];
            if (stack == null) stack = ItemStack.EMPTY;
            this.client.player.getInventory().offHand.set(i, stack.copy());
        }
        
        // Mark inventory as dirty to trigger synchronization
        this.client.player.getInventory().markDirty();
        
        System.out.println("Successfully applied loadout to inventory");
        return true;
    }
    
    /**
     * Swaps the current player inventory with the selected loadout (used in survival mode)
     */
    private boolean swapInventoryWithLoadout() {
        if (selectedLoadout != null && this.client != null && this.client.player != null) {
            System.out.println("Swapping inventory contents with loadout: " + selectedLoadout.getName());
            
            // Store the player's current inventory sections
            ItemStack[] playerHotbar = new ItemStack[Loadout.HOTBAR_SIZE];
            ItemStack[] playerMainInventory = new ItemStack[Loadout.MAIN_INVENTORY_SIZE];
            ItemStack[] playerArmor = new ItemStack[Loadout.ARMOR_SIZE];
            ItemStack[] playerOffhand = new ItemStack[Loadout.OFFHAND_SIZE];
            
            // Copy player's current items (mirroring hotbar logic that works)
            for (int i = 0; i < Loadout.HOTBAR_SIZE; i++) {
                playerHotbar[i] = this.client.player.getInventory().getStack(i).copy();
            }
            for (int i = 0; i < Loadout.MAIN_INVENTORY_SIZE; i++) {
                playerMainInventory[i] = this.client.player.getInventory().getStack(i + Loadout.HOTBAR_SIZE).copy();
            }
            for (int i = 0; i < Loadout.ARMOR_SIZE; i++) {
                playerArmor[i] = this.client.player.getInventory().getArmorStack(i).copy();
            }
            for (int i = 0; i < Loadout.OFFHAND_SIZE; i++) {
                playerOffhand[i] = this.client.player.getInventory().getStack(40).copy(); // Offhand slot
            }
            
            // Apply loadout items to player (mirroring hotbar logic)
            for (int i = 0; i < Loadout.HOTBAR_SIZE; i++) {
                this.client.player.getInventory().setStack(i, selectedLoadout.getHotbar()[i].copy());
            }
            for (int i = 0; i < Loadout.MAIN_INVENTORY_SIZE; i++) {
                this.client.player.getInventory().setStack(i + Loadout.HOTBAR_SIZE, selectedLoadout.getMainInventory()[i].copy());
            }
            for (int i = 0; i < Loadout.ARMOR_SIZE; i++) {
                this.client.player.getInventory().armor.set(i, selectedLoadout.getArmor()[i].copy());
            }
            for (int i = 0; i < Loadout.OFFHAND_SIZE; i++) {
                this.client.player.getInventory().offHand.set(i, selectedLoadout.getOffhand()[i].copy());
            }
            
            // Update loadout with player's original items
            for (int i = 0; i < Loadout.HOTBAR_SIZE; i++) {
                selectedLoadout.setHotbarSlot(i, playerHotbar[i]);
            }
            for (int i = 0; i < Loadout.MAIN_INVENTORY_SIZE; i++) {
                selectedLoadout.setMainInventorySlot(i, playerMainInventory[i]);
            }
            for (int i = 0; i < Loadout.ARMOR_SIZE; i++) {
                selectedLoadout.setArmorSlot(i, playerArmor[i]);
            }
            for (int i = 0; i < Loadout.OFFHAND_SIZE; i++) {
                selectedLoadout.setOffhandSlot(i, playerOffhand[i]);
            }
            
            // Update the loadout in storage
            boolean updateSuccess = manager.updateLoadout(selectedLoadout);
            if (!updateSuccess) {
                System.out.println("Failed to update loadout during swap: " + manager.getLastOperationResult());
                return false;
            }
            
            System.out.println("Successfully swapped inventory contents with loadout");
            
            // Refresh loadouts to get updated data
            refreshLoadouts();
            
            // Find the updated loadout and check if it's empty
            Loadout updatedLoadout = null;
            for (Loadout loadout : personalLoadouts) {
                if (loadout.getId().equals(selectedLoadout.getId())) {
                    updatedLoadout = loadout;
                    break;
                }
            }
            
            if (updatedLoadout != null && isLoadoutEmpty(updatedLoadout)) {
                // Delete the empty loadout
                boolean deleteSuccess = manager.deleteLoadout(updatedLoadout.getId());
                if (deleteSuccess) {
                    System.out.println("Loadout became empty after swap, successfully deleted: " + updatedLoadout.getName());
                    selectedLoadout = null;
                } else {
                    System.out.println("Failed to delete empty loadout after swap: " + manager.getLastOperationResult());
                }
                refreshLoadouts(); // Refresh again after deletion
            }
            
            return true;
        }
        return false;
    }
    
    // LoadoutUpdateListener implementation
    @Override
    public void onLoadoutsUpdated() {
        this.personalLoadouts = manager.getPersonalLoadouts();
        if (personalLoadoutList != null) {
            personalLoadoutList.updateLoadouts(personalLoadouts, activeTab);
        }
        
        // Load server loadouts if connected
        if (manager.isConnectedToServer()) {
            this.serverLoadouts = manager.getLoadouts(LoadoutClientManager.LoadoutType.SERVER);
            if (serverLoadoutList != null) {
                serverLoadoutList.updateLoadouts(serverLoadouts, activeTab);
            }
        }
        
        updateButtonStates();
    }
    
    /**
     * Check if the player has items in a specific section
     */
    private boolean hasPlayerSectionItems(LoadoutTab section) {
        if (this.client == null || this.client.player == null) {
            return false;
        }
        
        switch (section) {
            case ARMOR:
                for (int i = 0; i < 4; i++) {
                    if (!this.client.player.getInventory().getArmorStack(i).isEmpty()) {
                        return true;
                    }
                }
                return false;
                
            case HOTBAR:
                for (int i = 0; i < 9; i++) {
                    if (!this.client.player.getInventory().getStack(i).isEmpty()) {
                        return true;
                    }
                }
                return false;
                
            case INVENTORY:
                for (int i = 9; i < 36; i++) {
                    if (!this.client.player.getInventory().getStack(i).isEmpty()) {
                        return true;
                    }
                }
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Deposit a specific section from player inventory into an existing loadout
     */
    public void depositSectionIntoLoadout(Loadout loadout, LoadoutTab section) {
        if (loadout == null || this.client == null || this.client.player == null) {
            System.out.println("Loadout is null or client/player is null!");
            return;
        }

        if (!checkLoadoutSectionEmpty(loadout, section)) {
            System.out.println("Cannot deposit into " + section.getDisplayName() + " section - loadout section is not empty");
            return;
        }

        System.out.println("Depositing " + section.getDisplayName() + " section into loadout: " + loadout.getName());

        // Check if connected to server (multiplayer)
        boolean isConnectedToServer = manager.isConnectedToServer();

        if (isConnectedToServer) {
            // In multiplayer: send to server only, let server handle and sync back
            System.out.println("Connected to server - sending section deposit request to server");
            String sectionName = section.name().toLowerCase();
            DepositSectionPayload payload = new DepositSectionPayload(loadout.getId(), sectionName);
            ClientPlayNetworking.send(payload);
        } else {
            // In single-player: perform immediate local update
            System.out.println("Single-player mode - performing local section deposit");
            depositSectionIntoLoadoutLocally(loadout, section);
        }
    }
}