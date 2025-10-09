package rocamocha.mochamix.api.minecraft;

import rocamocha.mochamix.impl.NativeAccess;

/**
 * A marker interface for Minecraft components.
 * This interface is used to mark classes that represent various components in Minecraft,
 * such as enchantments, food items, etc.
 */
public interface MinecraftComponent extends NativeAccess {

    interface EnchantmentAccess extends MinecraftComponent {
        String namespace(); // e.g. "minecraft"
        String path(); // e.g. "sharpness"
        String full(); // e.g. "minecraft:sharpness"
        int level();
    }

    interface FoodAccess extends MinecraftComponent {
        int nutrition();
        float saturation();
        boolean alwaysEdible();
        int ticksToEat();
    }

    interface RarityAccess extends MinecraftComponent {
        String name(); // e.g. "common", "uncommon", "rare", "epic"
        int value(); // e.g. 0, 1, 2, 3
    }
}
