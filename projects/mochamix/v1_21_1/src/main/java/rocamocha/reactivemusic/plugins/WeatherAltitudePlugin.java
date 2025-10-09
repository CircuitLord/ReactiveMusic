package rocamocha.reactivemusic.plugins;

import rocamocha.reactivemusic.api.*;
import rocamocha.reactivemusic.api.eventsys.EventRecord;
import rocamocha.reactivemusic.api.songpack.SongpackEvent;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

import java.util.Map;

public final class WeatherAltitudePlugin extends ReactiveMusicPlugin {
    public WeatherAltitudePlugin() {
        super("reactivemusic", "weather_and_altitude");
    }
    private static EventRecord RAIN, SNOW, STORM, UNDERWATER, UNDERGROUND, DEEP_UNDERGROUND, HIGH_UP;

    @Override
    public void init() {
        registerSongpackEvents("RAIN", "SNOW", "STORM", "UNDERWATER", "UNDERGROUND", "DEEP", "DEEP_UNDERGROUND", "HIGH_UP");

        RAIN = SongpackEvent.get("RAIN");
        SNOW = SongpackEvent.get("SNOW");
        STORM = SongpackEvent.get("STORM");
        UNDERWATER = SongpackEvent.get("UNDERWATER");
        UNDERGROUND = SongpackEvent.get("UNDERGROUND");
        DEEP_UNDERGROUND = SongpackEvent.get("DEEP_UNDERGROUND");
        HIGH_UP = SongpackEvent.get("HIGH_UP");
    }

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        if (player == null || world == null) return;
        MinecraftVector3 pos = player.location().pos();

        eventMap.put(STORM, world.weather().isThunderingAt(pos));
        eventMap.put(RAIN, world.weather().isRainingAt(pos));
        eventMap.put(SNOW, world.weather().isSnowingAt(pos));
        eventMap.put(UNDERWATER, player.underwater());
        eventMap.put(UNDERGROUND, ReactiveMusicUtils.isUnderground(world, pos, 55));
        eventMap.put(DEEP_UNDERGROUND, ReactiveMusicUtils.isDeepUnderground(world, pos, 15));
        eventMap.put(HIGH_UP, ReactiveMusicUtils.isHighUp(pos, 128));
    }
}

