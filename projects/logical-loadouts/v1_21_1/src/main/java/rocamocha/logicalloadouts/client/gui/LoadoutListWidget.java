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
     * Update the loadout list with new data
     */
    public void updateLoadouts(List<Loadout> loadouts) {
        this.clearEntries();
        for (Loadout loadout : loadouts) {
            if (loadout != null) {  // Extra safety check
                this.addEntry(new LoadoutEntry(loadout));
            }
        }
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
    
    /**
     * Individual loadout entry in the list
     */
    public class LoadoutEntry extends AlwaysSelectedEntryListWidget.Entry<LoadoutEntry> {
        private final Loadout loadout;
        private long lastClickTime = 0;
        
        public LoadoutEntry(Loadout loadout) {
            this.loadout = loadout;
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
            
            // Draw armor section (top row)
            int iconY = y + 12;
            int iconSize = 16;
            int iconSpacing = 18;
            int maxWidth = entryWidth - 10;
            int currentX = x + 5;
            
            context.drawTextWithShadow(client.textRenderer, Text.literal("Armor:").formatted(Formatting.GOLD), currentX, iconY + 4, 0xFFFFFF);
            currentX += 45;
            
            for (net.minecraft.item.ItemStack armorItem : loadout.getArmor()) {
                if (!armorItem.isEmpty()) {
                    if (currentX + iconSize > x + maxWidth) {
                        // Wrap to next row
                        iconY += 20;
                        currentX = x + 50; // Align with items, not label
                    }
                    context.drawItem(armorItem, currentX, iconY);
                    if (armorItem.getCount() > 1) {
                        itemsToCount.add(new ItemCountInfo(armorItem, currentX, iconY));
                    }
                    currentX += iconSpacing;
                }
            }
            
            // Draw hotbar section (middle row)
            iconY += 16; // Extra space after armor
            currentX = x + 5;
            
            context.drawTextWithShadow(client.textRenderer, Text.literal("Hotbar:").formatted(Formatting.GREEN), currentX, iconY + 4, 0xFFFFFF);
            currentX += 55;
            
            for (net.minecraft.item.ItemStack hotbarItem : loadout.getHotbar()) {
                if (!hotbarItem.isEmpty()) {
                    if (currentX + iconSize > x + maxWidth) {
                        // Wrap to next row
                        iconY += 20;
                        currentX = x + 60; // Align with items, not label
                    }
                    context.drawItem(hotbarItem, currentX, iconY);
                    if (hotbarItem.getCount() > 1) {
                        itemsToCount.add(new ItemCountInfo(hotbarItem, currentX, iconY));
                    }
                    currentX += iconSpacing;
                }
            }
            
            // Draw inventory section (bottom section - 3 rows like Minecraft inventory)
            iconY += 16; // Extra space after hotbar
            currentX = x + 5;
            
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
            
            // Draw all item counts on top of everything
            for (ItemCountInfo countInfo : itemsToCount) {
                // Push matrix to ensure count is drawn on top
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 200); // High z-level to ensure it's on top
                drawItemCount(context, countInfo.item, countInfo.x, countInfo.y);
                context.getMatrices().pop();
            }
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            System.out.println("LoadoutEntry clicked: " + loadout.getName());
            LoadoutListWidget.this.setSelected(this);
            
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
            
            return true;
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
}