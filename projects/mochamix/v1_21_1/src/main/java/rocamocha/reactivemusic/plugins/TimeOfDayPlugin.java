package rocamocha.reactivemusic.plugins;

import rocamocha.reactivemusic.api.*;
import rocamocha.reactivemusic.api.eventsys.EventRecord;
import rocamocha.reactivemusic.api.songpack.SongpackEvent;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

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
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        if (player == null || world == null) return;

        long time = world.time().timeOfDay() % 24000L;
        boolean night   = (time >= 13000L && time < 23000L);
        boolean sunset  = (time >= 12000L && time < 13000L);
        boolean sunrise = (time >= 23000L); // mirrors your SongPicker logic

        eventMap.put(DAY,     !night);
        eventMap.put(NIGHT,    night);
        eventMap.put(SUNSET,   sunset);
        eventMap.put(SUNRISE,  sunrise);
    }
}