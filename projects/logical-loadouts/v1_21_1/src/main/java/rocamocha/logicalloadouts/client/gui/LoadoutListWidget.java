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
    public class LoadoutEntry extends Entry<LoadoutEntry> {
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
            
            // Draw loadout name
            context.drawTextWithShadow(client.textRenderer, Text.literal(loadout.getName()).formatted(Formatting.BOLD),
                                     x + 5, y + 2, 0xFFFFFF);
            
            // Draw loadout info
            String info = String.format("Items: %d • Modified: %s", 
                                      countNonEmptySlots(loadout),
                                      formatLastModified(loadout.getLastModified()));
            context.drawTextWithShadow(client.textRenderer, Text.literal(info),
                                     x + 5, y + 12, 0xAAAAAA);
            
            // Draw quick apply hint
            if (index < 5) {
                String quickKey = "Numpad " + (index + 1);
                context.drawTextWithShadow(client.textRenderer, Text.literal(quickKey).formatted(Formatting.ITALIC),
                                         x + entryWidth - 60, y + 8, 0x888888);
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
    }
}