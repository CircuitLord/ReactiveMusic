package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
    public void gameTick(PlayerEntity player, World world, Map<EventRecord, Boolean> eventMap) {
        if (player == null || world == null) return;
        BlockPos pos = player.getBlockPos();

        eventMap.put(STORM, ReactiveMusicUtils.isStorm(world));
        eventMap.put(RAIN, ReactiveMusicUtils.isRainingAt(world, pos));
        eventMap.put(SNOW, ReactiveMusicUtils.isSnowingAt(world, pos));
        eventMap.put(UNDERWATER, player.isSubmergedInWater());
        eventMap.put(UNDERGROUND, ReactiveMusicUtils.isUnderground(world, pos, 55));
        eventMap.put(DEEP_UNDERGROUND, ReactiveMusicUtils.isDeepUnderground(world, pos, 15));
        eventMap.put(HIGH_UP, ReactiveMusicUtils.isHighUp(pos, 128));
    }
}

