package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

import java.util.Map;

public final class DimensionPlugin extends ReactiveMusicPlugin {
    public DimensionPlugin() {
        super("reactivemusic", "dimension");
    }
    private static EventRecord OVERWORLD, NETHER, END;

    @Override
    public void init() {
        registerSongpackEvents("OVERWORLD", "NETHER", "END");

        OVERWORLD = SongpackEvent.get("OVERWORLD");
        NETHER    = SongpackEvent.get("NETHER");
        END       = SongpackEvent.get("END");
    }

    // Moved from SongPicker - now owned by this plugin
    private static String currentDimName = "";
    
    // Public accessor for SongPicker.isEntryValid()
    public static String getCurrentDimName() {
        return currentDimName;
    }

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        if (world == null) return;

        // Update current dimension name for SongPicker validation
        currentDimName = world.dimension().id();

        // Existing specific dimension event logic
        boolean isOverworld = world.dimension().id().equals("minecraft:overworld");
        boolean isNether    = world.dimension().id().equals("minecraft:the_nether");
        boolean isEnd       = world.dimension().id().equals("minecraft:the_end");

        eventMap.put(OVERWORLD, isOverworld);
        eventMap.put(NETHER,    isNether);
        eventMap.put(END,       isEnd);
    }
}
