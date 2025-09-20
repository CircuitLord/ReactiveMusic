package rocamocha.mochamix.impl.item.adapter;


import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import static java.util.stream.Collectors.toMap;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Rarity;
import rocamocha.mochamix.api.minecraft.MinecraftComponent.*;
import rocamocha.mochamix.impl.component.ComponentAdapters;
import rocamocha.mochamix.impl.component.adapter.FoodAdapter;
import rocamocha.mochamix.impl.component.adapter.RarityAdapter;


public class ItemStackComponentAdapter {
    protected final ItemStack itemStack;
    public ItemStackComponentAdapter(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    // Get all enchantments, preferring the highest level if duplicates exist.
    public Map<String, EnchantmentAccess> getEnchantments() {

        // Get both applied and stored enchantments
        ItemEnchantmentsComponent applied = itemStack.getEnchantments();
        ItemEnchantmentsComponent stored = itemStack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

        return Stream.concat(ComponentAdapters.from(stored), ComponentAdapters.from(applied))
            .collect(toMap(
                EnchantmentAccess::full,
                e -> e,
                (a, b) -> a.level() >= b.level() ? a : b) // Keep the highest level enchantment
            );
    }

    @Nullable
    public FoodAccess getFood() {
        FoodComponent component = itemStack.get(DataComponentTypes.FOOD);
        return component != null ? new FoodAdapter(component) : null;
    }

    public RarityAccess getRarity() {
        Rarity rarity = itemStack.getRarity();
        return rarity != null ? new RarityAdapter(rarity) : null;
    }
}
