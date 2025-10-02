package rocamocha.mochamix.plugins;

import circuitlord.reactivemusic.ReactiveMusicDebug;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import net.fabricmc.loader.api.FabricLoader;
import rocamocha.mochamix.api.minecraft.MinecraftEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

import java.util.Map;

public class ImmersiveAircraftPlugin extends ReactiveMusicPlugin {
    public ImmersiveAircraftPlugin() {
        super("mochamix", "immersive_aircraft");
    }

    private static final boolean IA_LOADED = FabricLoader.getInstance().isModLoaded("immersive_aircraft");

    private static EventRecord AIRSHIP; 

    @Override public void init() {
        if (!IA_LOADED) {
            ReactiveMusicDebug.LOGGER.warn("Immersive Aircraft mod not detected; ImmersiveAircraftPlugin disabled.");
            return;
        }
        
        registerSongpackEvents("AIRSHIP");
        AIRSHIP = SongpackEvent.get("AIRSHIP");
    }

    @Override public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        // Keep this safe even if IA isn't installed
        if (player == null || world == null || !IA_LOADED) return;
        eventMap.put(AIRSHIP, inAirship(player.vehicle()));
    }

    /** Detect IA airship by entity type registry id: immersive_aircraft:<something_with_airship> */
    private static boolean inAirship(MinecraftEntity vehicle) {
        // climb to the root vehicle (the actual aircraft)
        if (vehicle == null) return false;

        // strict namespace match
        if (!"immersive_aircraft".equals(vehicle.identity().namespace())) return false;

        // be flexible on the path; tighten when you know exact IDs
        String path = vehicle.identity().path().toLowerCase(java.util.Locale.ROOT);
        return path.contains("airship"); // e.g., "airship", "cargo_airship", etc.
    }
}
