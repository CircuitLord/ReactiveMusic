/**
 * Helper methods to convert Minecraft objects to Mochamix API views.
 * This is not intended to be implemented by API consumers, unless required
 * for interoperability with other mods or native Minecraft code outside of the Mochamix scope.
 */

package rocamocha.mochamix.api.io;

import java.util.Objects;
import java.util.ServiceLoader;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import rocamocha.mochamix.api.minecraft.MinecraftEntity;
import rocamocha.mochamix.api.minecraft.MinecraftItemStack;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;
import rocamocha.mochamix.impl.ViewFactory;

/**
 * Central conversion helpers that delegate to a version-specific factory.
 * The factory is resolved via {@link ServiceLoader} and can be replaced at runtime.
 * @see ViewFactory
 */
public final class MinecraftView {
    private static volatile ViewFactory factory;

    static {
        reloadFactory();
    }

    private MinecraftView() {}

    /**
     * Replace the active factory at runtime. Intended for tests or custom bootstrapping.
     */
    public static void setFactory(ViewFactory factory) {
        MinecraftView.factory = Objects.requireNonNull(factory, "factory");
    }

    /** Attempt to resolve the factory via {@link ServiceLoader}. */
    public static void reloadFactory() {
        factory = lookupFactory();
    }

    private static ViewFactory lookupFactory() {
        ServiceLoader<ViewFactory> loader = ServiceLoader.load(ViewFactory.class, ViewFactory.class.getClassLoader());
        for (ViewFactory candidate : loader) {
            return candidate;
        }
        return null;
    }

    private static ViewFactory requireFactory() {
        ViewFactory current = factory;
        if (current == null) {
            current = lookupFactory();
            factory = current;
        }
        if (current == null) {
            throw new IllegalStateException("No MinecraftViewFactory implementation found on the classpath.");
        }
        return current;
    }

    public static MinecraftPlayer of(PlayerEntity player) {
        return player == null ? null : requireFactory().createPlayer(player);
    }

    public static MinecraftEntity of(Entity entity) {
        return entity == null ? null : requireFactory().createEntity(entity);
    }

    public static MinecraftWorld of(World world) {
        return world == null ? null : requireFactory().createWorld(world);
    }

    public static MinecraftVector3 of(BlockPos position) {
        return position == null ? null : requireFactory().createPosition(position);
    }

    public static MinecraftVector3 of(Vec3d vector) {
        return vector == null ? null : requireFactory().createPosition(vector);
    }

    public static MinecraftItemStack of(ItemStack stack) {
        return stack == null ? null : requireFactory().createItemStack(stack);
    }
}
