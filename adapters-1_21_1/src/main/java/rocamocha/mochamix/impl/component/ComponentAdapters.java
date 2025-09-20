package rocamocha.mochamix.impl.component;

import java.util.stream.Stream;

import net.minecraft.component.type.ItemEnchantmentsComponent;
import rocamocha.mochamix.api.minecraft.MinecraftComponent.*;
import rocamocha.mochamix.impl.component.adapter.EnchantmentAdapter;

/**
 * Helpers to convert Minecraft components to their adapter-backed views.
 * @see rocamocha.mochamix.api.minecraft.MinecraftComponent
 */
public class ComponentAdapters {

    /**
     * Convert a ComponentHolder to a stream of Enchantment adapters.
     * Throws IllegalArgumentException if the component type is unsupported.
     * This is converted by the next adapter in the chain.
     * @param holder the component holder, e.g. from an ItemStack
     * @return a stream of Enchantment adapters, possibly empty and never null
     */
    public static Stream<EnchantmentAccess> from(ItemEnchantmentsComponent component) {
        return component.getEnchantments().stream()
        .map(e -> {
            int level = component.getLevel(e);
            return new EnchantmentAdapter(e, level);
        });
    }
}
