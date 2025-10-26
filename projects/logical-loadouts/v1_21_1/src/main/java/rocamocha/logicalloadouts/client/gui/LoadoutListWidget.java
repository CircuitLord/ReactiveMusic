package rocamocha.logicalloadouts.client.gui;

import rocamocha.logicalloadouts.data.Loadout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * List widget for displaying and selecting loadouts in the GUI.
 */
public class LoadoutListWidget extends AlwaysSelectedEntryListWidget<LoadoutListWidget.LoadoutEntry> {
    
    private final LoadoutSelectionScreen parent;
    private ContextMenu activeContextMenu = null;
    private LoadoutSelectionScreen.LoadoutTab currentActiveTab = LoadoutSelectionScreen.LoadoutTab.PERSONAL;
    
    public LoadoutListWidget(MinecraftClient client, int width, int height, int top, int itemHeight, List<Loadout> loadouts) {
        super(client, width, height, top, itemHeight);
        this.parent = null; // Will be set by parent screen
        updateLoadouts(loadouts);
    }
    
    public LoadoutListWidget(MinecraftClient client, int width, int height, int top, int itemHeight, 
                           List<Loadout> loadouts, LoadoutSelectionScreen parent) {
        super(client, width, height, top, itemHeight);
        this.parent = parent;
        updateLoadouts(loadouts);
    }
    
    /**
     * Update the loadout list with new data, filtering based on active tab
     */
    public void updateLoadouts(List<Loadout> loadouts, LoadoutSelectionScreen.LoadoutTab activeTab) {
        this.currentActiveTab = activeTab;
        this.clearEntries();
        for (Loadout loadout : loadouts) {
            if (loadout != null && shouldShowLoadout(loadout, activeTab)) {
                this.addEntry(new LoadoutEntry(loadout, activeTab));
            }
        }
    }
    
    /**
     * Update the loadout list with new data (legacy method for backward compatibility)
     */
    public void updateLoadouts(List<Loadout> loadouts) {
        updateLoadouts(loadouts, LoadoutSelectionScreen.LoadoutTab.PERSONAL); // Default to personal tab
    }
    
    /**
     * Check if a loadout should be shown in the current tab
     */
    private boolean shouldShowLoadout(Loadout loadout, LoadoutSelectionScreen.LoadoutTab activeTab) {
        if (activeTab == null || !activeTab.isSectionTab()) {
            return true; // Show all loadouts in non-section tabs
        }
        
        switch (activeTab) {
            case ARMOR:
                return hasArmorItems(loadout);
            case HOTBAR:
                return hasHotbarItems(loadout);
            case INVENTORY:
                return hasInventoryItems(loadout);
            default:
                return true;
        }
    }
    
    private boolean hasArmorItems(Loadout loadout) {
        for (net.minecraft.item.ItemStack item : loadout.getArmor()) {
            if (!item.isEmpty()) return true;
        }
        return false;
    }
    
    private boolean hasHotbarItems(Loadout loadout) {
        for (net.minecraft.item.ItemStack item : loadout.getHotbar()) {
            if (!item.isEmpty()) return true;
        }
        return false;
    }
    
    private boolean hasInventoryItems(Loadout loadout) {
        for (net.minecraft.item.ItemStack item : loadout.getMainInventory()) {
            if (!item.isEmpty()) return true;
        }
        return false;
    }
    
    @Override
    public void setSelected(LoadoutEntry entry) {
        super.setSelected(entry);
        if (parent != null && entry != null) {
            System.out.println("LoadoutListWidget: Setting selected loadout: " + entry.loadout.getName());
            parent.setSelectedLoadout(entry.loadout);
        } else {
            System.out.println("LoadoutListWidget: Clearing selection (parent=" + (parent != null) + ", entry=" + (entry != null) + ")");
        }
    }
    
    @Override
    public int getRowWidth() {
        return this.width - 20;
    }
    
    protected int getScrollbarPositionX() {
        return this.getRight() - 6;
    }
    
    @Override
    protected void renderList(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderList(context, mouseX, mouseY, delta);
        
        // Render context menu if active
        if (activeContextMenu != null && activeContextMenu.isVisible()) {
            activeContextMenu.render(context, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle context menu first
        if (activeContextMenu != null && activeContextMenu.isVisible()) {
            if (activeContextMenu.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        // Handle right-click for context menu on Personal tab
        if (button == 1 && currentActiveTab == LoadoutSelectionScreen.LoadoutTab.PERSONAL) {
            // Check if clicking on a loadout entry
            for (LoadoutEntry entry : this.children()) {
                if (entry.isMouseOver(mouseX, mouseY)) {
                    System.out.println("Right-click detected on loadout entry: " + entry.loadout.getName());
                    entry.showContextMenu(mouseX, mouseY);
                    return true;
                }
            }
        }
        
        // Handle regular list interaction
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle context menu key presses
        if (activeContextMenu != null && activeContextMenu.isVisible()) {
            if (activeContextMenu.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * Individual loadout entry in the list
     */
    public class LoadoutEntry extends AlwaysSelectedEntryListWidget.Entry<LoadoutEntry> {
        private final Loadout loadout;
        private final LoadoutSelectionScreen.LoadoutTab activeTab;
        private long lastClickTime = 0;
        
        public LoadoutEntry(Loadout loadout) {
            this(loadout, LoadoutSelectionScreen.LoadoutTab.PERSONAL);
        }
        
        public LoadoutEntry(Loadout loadout, LoadoutSelectionScreen.LoadoutTab activeTab) {
            this.loadout = loadout;
            this.activeTab = activeTab;
        }
        
        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, 
                          int mouseX, int mouseY, boolean hovered, float tickDelta) {
            
            // Safety check for null loadout
            if (loadout == null) {
                context.drawTextWithShadow(client.textRenderer, Text.literal("(Empty Slot)").formatted(Formatting.ITALIC),
                                         x + 5, y + 8, 0x888888);
                return;
            }
            
            // Draw background for hovered/selected entries
            if (hovered || this == getSelectedOrNull()) {
                context.fill(x - 2, y - 2, x + entryWidth + 2, y + entryHeight + 2, 0x40FFFFFF);
            }
            
            // Draw loadout info
            String info = String.format("Items: %d • Modified: %s",
                countNonEmptySlots(loadout),
                formatLastModified(loadout.getLastModified())
            );
            context.drawTextWithShadow(client.textRenderer, Text.literal(info), x + 5, y + 2, 0xAAAAAA);
            
            // Draw quick apply hint
            if (index < 5) {
                String quickKey = "Numpad " + (index + 1);
                context.drawTextWithShadow(client.textRenderer, Text.literal(quickKey).formatted(Formatting.ITALIC),
                                         x + entryWidth - 60, y + 2, 0x888888);
            }
            
            // Collect all items to draw counts for
            java.util.List<ItemCountInfo> itemsToCount = new java.util.ArrayList<>();
            
            // Draw sections based on active tab
            
            if (activeTab.isSectionTab()) {
                // In section tabs, only show the relevant section
                switch (activeTab) {
                    case ARMOR:
                        renderArmorSection(context, x, y, entryWidth, itemsToCount);
                        break;
                    case HOTBAR:
                        renderHotbarSection(context, x, y, entryWidth, itemsToCount);
                        break;
                    case INVENTORY:
                        renderInventorySection(context, x, y, entryWidth, itemsToCount);
                        break;
                    default:
                        // Should not happen
                        break;
                }
            } else {
                // In full tabs (Personal/Server), show all sections
                renderArmorSection(context, x, y, entryWidth, itemsToCount);
                renderHotbarSection(context, x, y + 20, entryWidth, itemsToCount);
                renderInventorySection(context, x, y + 40, entryWidth, itemsToCount);
            }
            
            // Draw all item counts on top of everything
            for (ItemCountInfo countInfo : itemsToCount) {
                // Push matrix to ensure count is drawn on top
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 200); // High z-level to ensure it's on top
                drawItemCount(context, countInfo.item, countInfo.x, countInfo.y);
                context.getMatrices().pop();
            }
        }
        
        private void renderArmorSection(DrawContext context, int x, int y, int entryWidth, java.util.List<ItemCountInfo> itemsToCount) {
            int iconY = y + 12;
            int iconSpacing = 18;
            int currentX = x + 5;
            
            context.drawTextWithShadow(client.textRenderer, Text.literal("Armor:").formatted(Formatting.GOLD), currentX, iconY + 4, 0xFFFFFF);
            currentX += 45;
            
            for (net.minecraft.item.ItemStack armorItem : loadout.getArmor()) {
                if (!armorItem.isEmpty()) {
                    context.drawItem(armorItem, currentX, iconY);
                    if (armorItem.getCount() > 1) {
                        itemsToCount.add(new ItemCountInfo(armorItem, currentX, iconY));
                    }
                    currentX += iconSpacing;
                }
            }
        }
        
        private void renderHotbarSection(DrawContext context, int x, int y, int entryWidth, java.util.List<ItemCountInfo> itemsToCount) {
            int iconY = y + 12;
            int iconSpacing = 18;
            int currentX = x + 5;
            
            context.drawTextWithShadow(client.textRenderer, Text.literal("Hotbar:").formatted(Formatting.GREEN), currentX, iconY + 4, 0xFFFFFF);
            currentX += 55;
            
            for (net.minecraft.item.ItemStack hotbarItem : loadout.getHotbar()) {
                if (!hotbarItem.isEmpty()) {
                    context.drawItem(hotbarItem, currentX, iconY);
                    if (hotbarItem.getCount() > 1) {
                        itemsToCount.add(new ItemCountInfo(hotbarItem, currentX, iconY));
                    }
                    currentX += iconSpacing;
                }
            }
        }
        
        private void renderInventorySection(DrawContext context, int x, int y, int entryWidth, java.util.List<ItemCountInfo> itemsToCount) {
            int iconY = y + 12;
            int iconSpacing = 18;
            int currentX = x + 5;
            
            context.drawTextWithShadow(client.textRenderer, Text.literal("Inventory:").formatted(Formatting.BLUE), currentX, iconY + 4, 0xFFFFFF);
            
            // Start inventory icons below the label
            int inventoryStartY = iconY + 20; // Start icons 20 pixels below the label
            int inventoryStartX = x + 5; // Align with other sections
            
            // Render inventory in 3 rows of 9 columns (like Minecraft inventory)
            net.minecraft.item.ItemStack[] inventoryItems = loadout.getMainInventory();
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotIndex = row * 9 + col;
                    if (slotIndex < inventoryItems.length) {
                        net.minecraft.item.ItemStack inventoryItem = inventoryItems[slotIndex];
                        if (!inventoryItem.isEmpty()) {
                            int itemX = inventoryStartX + (col * iconSpacing);
                            int itemY = inventoryStartY + (row * 20);
                            context.drawItem(inventoryItem, itemX, itemY);
                            if (inventoryItem.getCount() > 1) {
                                itemsToCount.add(new ItemCountInfo(inventoryItem, itemX, itemY));
                            }
                        }
                    }
                }
            }
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            System.out.println("LoadoutEntry clicked: " + loadout.getName() + ", button: " + button + ", activeTab: " + activeTab);
            LoadoutListWidget.this.setSelected(this);
            
            // Handle right-click for context menu (only on Personal tab)
            if (button == 1 && activeTab == LoadoutSelectionScreen.LoadoutTab.PERSONAL) {
                showContextMenu(mouseX, mouseY);
                return true;
            }
            
            // Handle left-click and double-click
            if (button == 0) {
                // Handle double-click to apply loadout
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < 300) { // 300ms double-click threshold
                    if (parent != null) {
                        // Apply loadout on double-click using the screen's method (includes survival mode logic)
                        LoadoutSelectionScreen screen = parent;
                        screen.setSelectedLoadout(this.loadout);
                        // Call the screen's applySelectedLoadout method to ensure survival mode mechanics are applied
                        screen.applySelectedLoadoutPublic();
                    }
                }
                lastClickTime = currentTime;
            }
            
            return true;
        }
        
        /**
         * Show context menu for right-click on Personal tab
         */
        private void showContextMenu(double mouseX, double mouseY) {
            if (parent == null) return;
            
            // Create and show context menu
            activeContextMenu = new ContextMenu((int)mouseX, (int)mouseY, this);
        }
        
        @Override
        public Text getNarration() {
            return Text.literal("Loadout: " + loadout.getName() + 
                               " with " + countNonEmptySlots(loadout) + " items");
        }
        
        private int countNonEmptySlots(Loadout loadout) {
            int count = 0;
            
            // Count hotbar items
            for (net.minecraft.item.ItemStack item : loadout.getHotbar()) {
                if (!item.isEmpty()) count++;
            }
            
            // Count main inventory items
            for (net.minecraft.item.ItemStack item : loadout.getMainInventory()) {
                if (!item.isEmpty()) count++;
            }
            
            // Count armor items
            for (net.minecraft.item.ItemStack item : loadout.getArmor()) {
                if (!item.isEmpty()) count++;
            }
            
            // Count offhand items
            for (net.minecraft.item.ItemStack item : loadout.getOffhand()) {
                if (!item.isEmpty()) count++;
            }
            
            return count;
        }
        
        private String formatLastModified(long lastModified) {
            long diff = System.currentTimeMillis() - lastModified;
            if (diff < 60000) return "now";
            if (diff < 3600000) return (diff / 60000) + "m";
            if (diff < 86400000) return (diff / 3600000) + "h";
            return (diff / 86400000) + "d";
        }
        
        /**
         * Draws item count overlay like Minecraft inventory (bottom-right corner)
         */
        private void drawItemCount(DrawContext context, net.minecraft.item.ItemStack item, int x, int y) {
            if (item.getCount() > 1) {
                String countText = String.valueOf(item.getCount());
                int textX = x + 16 - client.textRenderer.getWidth(countText); // Right-align in 16px icon
                int textY = y + 8; // Bottom of 16px icon
                
                // Use drawTextWithShadow for better visibility (like Minecraft inventory)
                context.drawTextWithShadow(client.textRenderer, Text.literal(countText), textX, textY, 0xFFFFFF);
            }
        }
        
        /**
         * Helper class to store item count rendering information
         */
        private static class ItemCountInfo {
            final net.minecraft.item.ItemStack item;
            final int x, y;
            
            ItemCountInfo(net.minecraft.item.ItemStack item, int x, int y) {
                this.item = item;
                this.x = x;
                this.y = y;
            }
        }
    }
    
    /**
     * Simple context menu for right-click actions
     */
    private class ContextMenu {
        private int x, y;
        private final LoadoutEntry entry;
        private final java.util.List<MenuItem> items;
        private final int width = 120;
        private final int height = 60;
        private boolean visible = true;
        
        public ContextMenu(int x, int y, LoadoutEntry entry) {
            this.x = x;
            this.y = y;
            this.entry = entry;
            
            // Create menu items
            items = new java.util.ArrayList<>();
            items.add(new MenuItem("Take Armor", () -> applySection(LoadoutSelectionScreen.LoadoutTab.ARMOR)));
            items.add(new MenuItem("Take Hotbar", () -> applySection(LoadoutSelectionScreen.LoadoutTab.HOTBAR)));
            items.add(new MenuItem("Take Inventory", () -> applySection(LoadoutSelectionScreen.LoadoutTab.INVENTORY)));
            
            // Adjust position to fit on screen
            adjustPosition();
        }
        
        private void adjustPosition() {
            // Ensure menu doesn't go off screen and stays within the list widget bounds
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            
            // Get list widget bounds
            int listX = getX();
            int listY = getY();
            int listWidth = getWidth();
            int listHeight = getHeight();
            
            // First, try to position relative to the list widget
            if (x + width > listX + listWidth) {
                x = listX + listWidth - width;
            }
            if (x < listX) {
                x = listX;
            }
            if (y + height > listY + listHeight) {
                y = listY + listHeight - height;
            }
            if (y < listY) {
                y = listY;
            }
            
            // Then ensure it doesn't go off screen (fallback)
            if (x + width > screenWidth) {
                x = screenWidth - width;
            }
            if (x < 0) {
                x = 0;
            }
            if (y + height > screenHeight) {
                y = screenHeight - height;
            }
            if (y < 0) {
                y = 0;
            }
        }
        
        private void applySection(LoadoutSelectionScreen.LoadoutTab section) {
            if (parent != null) {
                parent.applySectionFromLoadout(entry.loadout, section);
            }
            closeMenu();
        }
        
        private void closeMenu() {
            visible = false;
            activeContextMenu = null;
        }
        
        void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (!visible) return;
            
            // Push matrix to ensure menu appears above all other elements
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 300); // Higher z-level than item icons (200)
            
            // Draw background
            context.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xFF000000);
            context.fill(x, y, x + width, y + height, 0xFF2D2D2D);
            
            // Draw border
            context.drawBorder(x, y, width, height, 0xFFFFFFFF);
            
            // Draw menu items
            int itemY = y + 4;
            for (int i = 0; i < items.size(); i++) {
                MenuItem item = items.get(i);
                boolean hovered = mouseX >= x && mouseX <= x + width && 
                                mouseY >= itemY && mouseY <= itemY + 16;
                
                // Draw item background if hovered
                if (hovered) {
                    context.fill(x + 2, itemY, x + width - 2, itemY + 16, 0xFF404040);
                }
                
                // Draw item text
                context.drawTextWithShadow(client.textRenderer, Text.literal(item.text), x + 6, itemY + 4, 
                                        hovered ? 0xFFFFFF : 0xCCCCCC);
                
                itemY += 18;
            }
            
            context.getMatrices().pop();
        }
        
        boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible) return false;
            
            if (button == 0) { // Left click
                int itemY = y + 4;
                for (int i = 0; i < items.size(); i++) {
                    if (mouseX >= x && mouseX <= x + width && 
                        mouseY >= itemY && mouseY <= itemY + 16) {
                        items.get(i).action.run();
                        return true;
                    }
                    itemY += 18;
                }
            }
            
            // Close menu if clicked outside
            closeMenu();
            return true;
        }
        
        boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 256) { // ESC key
                closeMenu();
                return true;
            }
            return false;
        }
        
        boolean isVisible() {
            return visible;
        }
        
        private class MenuItem {
            final String text;
            final Runnable action;
            
            MenuItem(String text, Runnable action) {
                this.text = text;
                this.action = action;
            }
        }
    }
}