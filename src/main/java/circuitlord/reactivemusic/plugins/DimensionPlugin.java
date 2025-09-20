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

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        if (world == null) return;

        boolean isOverworld = world.dimension().id() == "minecraft:overworld";
        boolean isNether    = world.dimension().id() == "minecraft:the_nether" ;
        boolean isEnd       = world.dimension().id() == "minecraft:the_end";

        eventMap.put(OVERWORLD, isOverworld);
        eventMap.put(NETHER,    isNether);
        eventMap.put(END,       isEnd);
    }
}
