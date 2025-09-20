package rocamocha.mochamix.impl.item;

import java.util.Map;

import net.minecraft.item.ItemStack;
import rocamocha.mochamix.api.minecraft.MinecraftComponent.*;
import rocamocha.mochamix.api.minecraft.MinecraftItemStack;
import rocamocha.mochamix.impl.item.adapter.ItemStackIdentityAdapter;
import rocamocha.mochamix.impl.item.adapter.ItemStackComponentAdapter;

/**
 * Socket-backed view wrapper for ItemStack.
 * @see rocamocha.mochamix.api.minecraft.MinecraftItemStack
 */
public class ItemStackSocket implements MinecraftItemStack {
    
    protected final ItemStack itemStack;

    private final ItemStackIdentityAdapter identity;
    private final ItemStackComponentAdapter components;

    public ItemStackSocket(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.identity = new ItemStackIdentityAdapter(itemStack);
        this.components = new ItemStackComponentAdapter(itemStack);
    }
    
    @Override public ItemStack asNative() { return itemStack; }

    @Override public String name() { return itemStack.getName().getString(); }
    @Override public Identity identity() { return identity; }
    @Override public Map<String, EnchantmentAccess> enchantments() { return components.getEnchantments(); }
    @Override public FoodAccess food() { return components.getFood(); }

    // Common item stack properties.
    // Use adapter for more specialized access if needed.
    @Override public int count() { return itemStack.getCount(); }
    @Override public int max() { return itemStack.getMaxCount(); }
    @Override public boolean isEmpty() { return itemStack.isEmpty(); }
}