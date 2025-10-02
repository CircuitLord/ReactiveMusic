package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import circuitlord.reactivemusic.config.ModConfig;

// TODO: find a way to remove these leaks
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

// import net.minecraft.client.MinecraftClient;
// import net.minecraft.client.network.ServerInfo;
import java.util.Map;

public final class AtHomePlugin extends ReactiveMusicPlugin {
    public AtHomePlugin() {
        super("reactivemusic", "at_home");
    }

    private static final float RADIUS = 45.0f;

    // Plugin-local state; no more SongPicker.wasSleeping
    private static boolean wasSleeping = false;

    // Event handles
    private static EventRecord HOME, HOME_OVERWORLD, HOME_NETHER, HOME_END;

    @Override
    public void init() {
        registerSongpackEvents("HOME", "HOME_OVERWORLD", "HOME_NETHER", "HOME_END");
        
        HOME = SongpackEvent.get("HOME");
        HOME_OVERWORLD = SongpackEvent.get("HOME_OVERWORLD");
        HOME_NETHER = SongpackEvent.get("HOME_NETHER");
        HOME_END = SongpackEvent.get("HOME_END");
    }

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        if (player == null || world == null) return;

        // Keys: base (per save/server), and per-dimension
        String baseKey = computeBaseWorldKey();
        String dimPath = world.dimension().path(); // overworld | the_nether | the_end | ...
        String dimKey = baseKey + "_" + dimPath;

        // On sleep edge, save both base and dimension-specific homes
        if (!wasSleeping && player.sleeping()) {
            var pos = player.location().pos();
            ReactiveMusic.modConfig.savedHomePositions.put(baseKey, pos);
            ReactiveMusic.modConfig.savedHomePositions.put(dimKey, pos);
            // TODO: There is definitely a better way to serialize the positions that is built into the fabric mappings
            // ???: Is it Persistent State? Is it possible to make it part of the stable API?
            ModConfig.saveConfig();
        }
        wasSleeping = player.sleeping();

        // Emit base HOME (per save/server, regardless of dimension)
        eventMap.put(HOME, isWithinHome(world, player, baseKey));

        // Emit one of the three dimension-specific events (only for vanilla dims)
        String dimId = world.dimension().path();
        if (dimId == "overworld") {
            eventMap.put(HOME_OVERWORLD, isWithinHome(world, player, dimKey));
            eventMap.put(HOME_NETHER, false);
            eventMap.put(HOME_END, false);
        } else if (dimId == "the_nether") {
            eventMap.put(HOME_OVERWORLD, false);
            eventMap.put(HOME_NETHER, isWithinHome(world, player, dimKey));
            eventMap.put(HOME_END, false);
        } else if (dimId == "the_end") {
            eventMap.put(HOME_OVERWORLD, false);
            eventMap.put(HOME_NETHER, false);
            eventMap.put(HOME_END, isWithinHome(world, player, dimKey));
        } else {
            // Non-vanilla dimension: keep the three vanilla-specific flags false
            eventMap.put(HOME_OVERWORLD, false);
            eventMap.put(HOME_NETHER, false);
            eventMap.put(HOME_END, false);
        }
    }

    // --- helpers ---

    private static boolean isWithinHome(MinecraftWorld world, MinecraftPlayer player, String key) {
        var map = ReactiveMusic.modConfig.savedHomePositions;
        if (!map.containsKey(key)) return false;
        MinecraftVector3 dist = player.location().pos().subtract(map.get(key));
        return dist.length() < RADIUS;
    }

    /** Per-save (singleplayer) or per-server (multiplayer) identifier — no dimension. */
    private static String computeBaseWorldKey() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            if (mc.isInSingleplayer() && mc.getServer() != null && mc.getServer().getSaveProperties() != null) {
                // Singleplayer: user-facing save name (from level.dat)
                String pretty = mc.getServer().getSaveProperties().getLevelName();
                if (pretty != null && !pretty.isBlank()) return pretty;
            } else {
                // Multiplayer: server list entry (client-side safe)
                ServerInfo entry = mc.getCurrentServerEntry();
                if (entry != null) {
                    if (entry.name != null && !entry.name.isBlank()) return entry.name;
                    if (entry.address != null && !entry.address.isBlank()) return entry.address;
                }
            }
        }
        return "unknown_world";
    }
}