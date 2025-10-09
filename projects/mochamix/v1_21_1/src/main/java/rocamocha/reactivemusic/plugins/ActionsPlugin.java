package rocamocha.reactivemusic.plugins;

import rocamocha.reactivemusic.api.*;
import rocamocha.reactivemusic.api.eventsys.EventRecord;
import rocamocha.reactivemusic.api.songpack.SongpackEvent;

// TODO: find a way to remove these leaks
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import rocamocha.mochamix.api.minecraft.MinecraftEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

import java.util.Map;

public final class ActionsPlugin extends ReactiveMusicPlugin {
    
    public ActionsPlugin() {
        super("reactivemusic", "actions");
    }

    private static EventRecord FISHING, MINECART, BOAT, HORSE, PIG;

    @Override
    public void init() {
        registerSongpackEvents("FISHING","MINECART","BOAT","HORSE","PIG");
        
        FISHING = SongpackEvent.get("FISHING");
        MINECART = SongpackEvent.get("MINECART");
        BOAT = SongpackEvent.get("BOAT");
        HORSE = SongpackEvent.get("HORSE");
        PIG = SongpackEvent.get("PIG");
    }
    

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        if (player == null) return;

        eventMap.put(FISHING, player.fishing());

        MinecraftEntity v = player.vehicle();
        eventMap.put(MINECART, v instanceof MinecartEntity);
        eventMap.put(BOAT,     v instanceof BoatEntity);
        eventMap.put(HORSE,    v instanceof HorseEntity);
        eventMap.put(PIG,      v instanceof PigEntity);
    }
}