package rocamocha.voidloom;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

/**
 * Holds registry keys and lifecycle hooks for the Voidloom dimension.
 * The dimension data itself is supplied by datapack JSON (see codex/generated/data).
 */
public final class VoidloomDimensionBootstrap {
    public static final Identifier VOIDLOOM_DIMENSION_ID = Identifier.of("voidloom", "voidloom");
    public static final Identifier VOIDLOOM_DIMENSION_TYPE_ID = Identifier.of("voidloom", "voidloom_void");

    public static final RegistryKey<World> VOIDLOOM_WORLD_KEY = RegistryKey.of(RegistryKeys.WORLD, VOIDLOOM_DIMENSION_ID);
    public static final RegistryKey<DimensionType> VOIDLOOM_DIMENSION_TYPE_KEY =
            RegistryKey.of(RegistryKeys.DIMENSION_TYPE, VOIDLOOM_DIMENSION_TYPE_ID);

    private VoidloomDimensionBootstrap() {
    }

    public static void registerLifecycleCallbacks() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey().equals(VOIDLOOM_WORLD_KEY)) {
                Voidloom.LOGGER.debug("Voidloom world attached at {}", world.getRegistryKey().getValue());
            }
        });
    }
}
