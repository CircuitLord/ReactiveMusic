package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class TimeOfDayPlugin extends ReactiveMusicPlugin {
    public TimeOfDayPlugin() {
        super("reactivemusic", "time_of_day");
    }

    private static EventRecord DAY, NIGHT, SUNSET, SUNRISE;
    
    @Override
    public void init() {
        registerSongpackEvents("DAY","NIGHT","SUNSET","SUNRISE");
        
        DAY = SongpackEvent.get("DAY");
        NIGHT = SongpackEvent.get("NIGHT");
        SUNSET = SongpackEvent.get("SUNSET");
        SUNRISE = SongpackEvent.get("SUNRISE");
    }


    @Override
    public void gameTick(PlayerEntity player, World world, Map<EventRecord, Boolean> eventMap) {
        if (player == null || world == null) return;

        long time = world.getTimeOfDay() % 24000L;
        boolean night   = (time >= 13000L && time < 23000L);
        boolean sunset  = (time >= 12000L && time < 13000L);
        boolean sunrise = (time >= 23000L); // mirrors your SongPicker logic

        eventMap.put(DAY,     !night);
        eventMap.put(NIGHT,    night);
        eventMap.put(SUNSET,   sunset);
        eventMap.put(SUNRISE,  sunrise);
    }
}