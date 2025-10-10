package rocamocha.logicalloadouts.data;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Represents a complete equipment loadout that can be applied to a player's inventory.
 * Supports both client and server-side usage with proper NBT serialization.
 */
public class Loadout {
    public static final int MAX_LOADOUT_NAME_LENGTH = 32;
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_INVENTORY_SIZE = 27;
    public static final int ARMOR_SIZE = 4;
    public static final int OFFHAND_SIZE = 1;
    
    private final UUID id;
    private String name;
    private long lastModified;
    
    // Inventory slots - using arrays for efficient access
    private final ItemStack[] hotbar = new ItemStack[HOTBAR_SIZE];
    private final ItemStack[] mainInventory = new ItemStack[MAIN_INVENTORY_SIZE]; 
    private final ItemStack[] armor = new ItemStack[ARMOR_SIZE]; // boots, leggings, chestplate, helmet
    private final ItemStack[] offhand = new ItemStack[OFFHAND_SIZE];
    
    // Metadata
    private final Map<String, String> metadata = new HashMap<>();
    
    /**
     * Creates a new empty loadout with a random UUID
     */
    public Loadout(String name) {
        this(UUID.randomUUID(), name);
    }
    
    /**
     * Creates a loadout with a specific UUID (used for deserialization)
     */
    public Loadout(UUID id, String name) {
        this.id = id;
        this.name = validateName(name);
        this.lastModified = System.currentTimeMillis();
        
        // Initialize all slots with empty stacks
        Arrays.fill(hotbar, ItemStack.EMPTY);
        Arrays.fill(mainInventory, ItemStack.EMPTY);
        Arrays.fill(armor, ItemStack.EMPTY);
        Arrays.fill(offhand, ItemStack.EMPTY);
    }
    
    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Loadout name cannot be empty");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_LOADOUT_NAME_LENGTH) {
            throw new IllegalArgumentException("Loadout name too long (max " + MAX_LOADOUT_NAME_LENGTH + " characters)");
        }
        return trimmed;
    }
    
    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public long getLastModified() { return lastModified; }
    
    public ItemStack[] getHotbar() { return hotbar.clone(); }
    public ItemStack[] getMainInventory() { return mainInventory.clone(); }
    public ItemStack[] getArmor() { return armor.clone(); }
    public ItemStack[] getOffhand() { return offhand.clone(); }
    
    public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
    
    // Setters
    public void setName(String name) {
        this.name = validateName(name);
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setHotbarSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= HOTBAR_SIZE) {
            throw new IllegalArgumentException("Invalid hotbar slot: " + slot);
        }
        hotbar[slot] = stack == null ? ItemStack.EMPTY : stack.copy();
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setMainInventorySlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= MAIN_INVENTORY_SIZE) {
            throw new IllegalArgumentException("Invalid main inventory slot: " + slot);
        }
        mainInventory[slot] = stack == null ? ItemStack.EMPTY : stack.copy();
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setArmorSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= ARMOR_SIZE) {
            throw new IllegalArgumentException("Invalid armor slot: " + slot);
        }
        armor[slot] = stack == null ? ItemStack.EMPTY : stack.copy();
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setOffhandSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= OFFHAND_SIZE) {
            throw new IllegalArgumentException("Invalid offhand slot: " + slot);
        }
        offhand[slot] = stack == null ? ItemStack.EMPTY : stack.copy();
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * NBT Serialization for persistence and networking
     */
    public NbtCompound toNbt() {
    NbtCompound nbt = new NbtCompound();
    // Basic data
    nbt.putUuid("id", id);
    nbt.putString("name", name);
    nbt.putLong("lastModified", lastModified);

    // Debug: count non-empty items before saving
    System.out.println("[Loadout.toNbt] Saving loadout '" + name + "' (" + id + ")");
    System.out.println("  Hotbar: " + countNonEmptyItems(hotbar) + " items");
    System.out.println("  MainInventory: " + countNonEmptyItems(mainInventory) + " items");
    System.out.println("  Armor: " + countNonEmptyItems(armor) + " items");
    System.out.println("  Offhand: " + countNonEmptyItems(offhand) + " items");

    // Serialize inventory slots
    nbt.put("hotbar", serializeItemArray(hotbar));
    nbt.put("mainInventory", serializeItemArray(mainInventory));
    nbt.put("armor", serializeItemArray(armor));
    nbt.put("offhand", serializeItemArray(offhand));

    // Serialize metadata
    NbtCompound metaNbt = new NbtCompound();
    metadata.forEach(metaNbt::putString);
    nbt.put("metadata", metaNbt);

    return nbt;
    }
    
    /**
     * NBT Deserialization
     */
    public static Loadout fromNbt(NbtCompound nbt) {
        UUID id = nbt.getUuid("id");
        String name = nbt.getString("name");

        Loadout loadout = new Loadout(id, name);
        loadout.lastModified = nbt.getLong("lastModified");

        // Deserialize inventory slots
        deserializeItemArray(nbt.getList("hotbar", 10), loadout.hotbar);
        deserializeItemArray(nbt.getList("mainInventory", 10), loadout.mainInventory);
        deserializeItemArray(nbt.getList("armor", 10), loadout.armor);
        deserializeItemArray(nbt.getList("offhand", 10), loadout.offhand);

        // Debug: count non-empty items after loading
        System.out.println("[Loadout.fromNbt] Loaded loadout '" + name + "' (" + id + ")");
        System.out.println("  Hotbar: " + countNonEmptyItems(loadout.hotbar) + " items");
        System.out.println("  MainInventory: " + countNonEmptyItems(loadout.mainInventory) + " items");
        System.out.println("  Armor: " + countNonEmptyItems(loadout.armor) + " items");
        System.out.println("  Offhand: " + countNonEmptyItems(loadout.offhand) + " items");

        // Deserialize metadata
        if (nbt.contains("metadata")) {
            NbtCompound metaNbt = nbt.getCompound("metadata");
            for (String key : metaNbt.getKeys()) {
                loadout.metadata.put(key, metaNbt.getString(key));
            }
        }

        return loadout;
    }
    
    // Helper for debug output
    private static int countNonEmptyItems(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty()) count++;
        }
        return count;
    }
    
    private NbtList serializeItemArray(ItemStack[] items) {
        NbtList list = new NbtList();
        System.out.println("  [serializeItemArray] Processing " + items.length + " slots");
        for (int i = 0; i < items.length; i++) {
            NbtCompound itemNbt = new NbtCompound();
            if (!items[i].isEmpty()) {
                // Use encode method with proper registry manager
                NbtCompound itemData = (NbtCompound) items[i].encode(net.minecraft.registry.DynamicRegistryManager.EMPTY);
                // Copy all keys from itemData to itemNbt except for the Slot key we'll add
                for (String key : itemData.getKeys()) {
                    itemNbt.put(key, itemData.get(key));
                }
                System.out.println("    Slot " + i + ": " + items[i].toString() + " -> " + itemNbt.getKeys());
            } else {
                System.out.println("    Slot " + i + ": EMPTY");
            }
            itemNbt.putInt("Slot", i);
            list.add(itemNbt);
        }
        System.out.println("  [serializeItemArray] Created NBT list with " + list.size() + " entries");
        return list;
    }
    
    private static void deserializeItemArray(NbtList list, ItemStack[] items) {
        Arrays.fill(items, ItemStack.EMPTY);
        System.out.println("  [deserializeItemArray] Processing " + list.size() + " entries");
        for (int i = 0; i < list.size(); i++) {
            NbtCompound itemNbt = list.getCompound(i);
            int slot = itemNbt.getInt("Slot");
            System.out.println("    Entry " + i + ": slot=" + slot + ", nbtKeys=" + itemNbt.getKeys());
            if (slot >= 0 && slot < items.length) {
                // Only try to deserialize if there's actual item data (not just a Slot entry)
                if (itemNbt.getKeys().size() > 1) { // More than just "Slot" key
                    // Create a copy without the Slot key for deserialization
                    NbtCompound itemData = new NbtCompound();
                    for (String key : itemNbt.getKeys()) {
                        if (!"Slot".equals(key)) {
                            itemData.put(key, itemNbt.get(key));
                        }
                    }
                    ItemStack stack = ItemStack.fromNbt(net.minecraft.registry.DynamicRegistryManager.EMPTY, itemData).orElse(ItemStack.EMPTY);
                    items[slot] = stack;
                    System.out.println("      -> Loaded: " + (stack.isEmpty() ? "EMPTY" : stack.toString()));
                } else {
                    items[slot] = ItemStack.EMPTY;
                    System.out.println("      -> Empty slot (no item data)");
                }
            }
        }
    }
    
    /**
     * Validates that the loadout contains only valid items and doesn't exceed limits
     */
    public boolean isValid() {
        // Check for invalid items or NBT exploits
        return validateItemArray(hotbar) && 
               validateItemArray(mainInventory) && 
               validateItemArray(armor) && 
               validateItemArray(offhand);
    }
    
    private boolean validateItemArray(ItemStack[] items) {
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                // Validate item exists and stack size is reasonable
                if (item.getItem() == null || item.getCount() < 1 || item.getCount() > item.getMaxCount()) {
                    return false;
                }
                // Additional validation could be added here (banned items, etc.)
            }
        }
        return true;
    }
    
    /**
     * Calculates a simple hash for detecting changes
     */
    public int getContentHash() {
        int hash = name.hashCode();
        hash = 31 * hash + Arrays.hashCode(hotbar);
        hash = 31 * hash + Arrays.hashCode(mainInventory);
        hash = 31 * hash + Arrays.hashCode(armor);
        hash = 31 * hash + Arrays.hashCode(offhand);
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Loadout other)) return false;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    /**
     * Create a loadout from a player's current inventory
     */
    public static Loadout fromPlayer(net.minecraft.entity.player.PlayerEntity player, String name) {
        Loadout loadout = new Loadout(name);
        
        // Copy hotbar
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            loadout.setHotbarSlot(i, stack);
        }
        
        // Copy main inventory (slots 9-35 in player inventory)
        for (int i = 0; i < MAIN_INVENTORY_SIZE; i++) {
            ItemStack stack = player.getInventory().getStack(i + HOTBAR_SIZE);
            loadout.setMainInventorySlot(i, stack);
        }
        
        // Copy armor (armor slots in player inventory)
        for (int i = 0; i < ARMOR_SIZE; i++) {
            ItemStack stack = player.getInventory().getArmorStack(i);
            loadout.setArmorSlot(i, stack);
        }
        
        // Copy offhand
        ItemStack offhandStack = player.getOffHandStack();
        loadout.setOffhandSlot(0, offhandStack);
        
        return loadout;
    }
    
    /**
     * Copy inventory contents from a player into this loadout
     */
    public void copyFromPlayer(net.minecraft.entity.player.PlayerEntity player) {
        // Copy hotbar
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            setHotbarSlot(i, stack);
        }
        
        // Copy main inventory (slots 9-35 in player inventory)
        for (int i = 0; i < MAIN_INVENTORY_SIZE; i++) {
            ItemStack stack = player.getInventory().getStack(i + HOTBAR_SIZE);
            setMainInventorySlot(i, stack);
        }
        
        // Copy armor (armor slots in player inventory)
        for (int i = 0; i < ARMOR_SIZE; i++) {
            ItemStack stack = player.getInventory().getArmorStack(i);
            setArmorSlot(i, stack);
        }
        
        // Copy offhand
        ItemStack offhandStack = player.getInventory().getStack(40); // Offhand slot is 40
        setOffhandSlot(0, offhandStack);
        
        // Update timestamp
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * Apply this loadout to a player's inventory
     */
    public void applyToPlayer(net.minecraft.entity.player.PlayerEntity player) {
        // Clear player inventory first
        player.getInventory().clear();
        
        // Set hotbar
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            player.getInventory().setStack(i, hotbar[i].copy());
        }
        
        // Set main inventory (slots 9-35 in player inventory)
        for (int i = 0; i < MAIN_INVENTORY_SIZE; i++) {
            player.getInventory().setStack(i + HOTBAR_SIZE, mainInventory[i].copy());
        }
        
        // Set armor
        for (int i = 0; i < ARMOR_SIZE; i++) {
            player.getInventory().armor.set(i, armor[i].copy());
        }
        
        // Set offhand
        player.getInventory().offHand.set(0, offhand[0].copy());
        
        // Force synchronization
        player.getInventory().markDirty();
    }
    
    /**
     * Create a copy of this loadout
     */
    public Loadout copy() {
        Loadout copy = new Loadout(this.name + " (Copy)");
        
        // Copy all inventory slots
        System.arraycopy(this.hotbar, 0, copy.hotbar, 0, HOTBAR_SIZE);
        System.arraycopy(this.mainInventory, 0, copy.mainInventory, 0, MAIN_INVENTORY_SIZE);
        System.arraycopy(this.armor, 0, copy.armor, 0, ARMOR_SIZE);
        System.arraycopy(this.offhand, 0, copy.offhand, 0, OFFHAND_SIZE);
        
        // Copy metadata
        copy.metadata.putAll(this.metadata);
        
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("Loadout{id=%s, name='%s', lastModified=%d}", 
                           id, name, lastModified);
    }
}