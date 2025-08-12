package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

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
        HORSE = SongpackEvent.get("HORSEING");
        PIG = SongpackEvent.get("PIG");
    }
    

    @Override
    public void gameTick(PlayerEntity player, World world, Map<EventRecord, Boolean> eventMap) {
        if (player == null) return;

        eventMap.put(FISHING, player.fishHook != null);

        Entity v = player.getVehicle();
        eventMap.put(MINECART, v instanceof MinecartEntity);
        eventMap.put(BOAT,     v instanceof BoatEntity);
        eventMap.put(HORSE,    v instanceof HorseEntity);
        eventMap.put(PIG,      v instanceof PigEntity);
    }
}