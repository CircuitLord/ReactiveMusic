package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

import java.util.Map;

public final class BossBarPlugin extends ReactiveMusicPlugin {
    public BossBarPlugin() {
        super("reactivemusic", "bossbar");
    }
    private static EventRecord BOSS;

    @Override public void init() {
        registerSongpackEvents("BOSS");

        BOSS = SongpackEvent.get("BOSS");
    }

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        if (BOSS == null) return;
        boolean active = ReactiveMusicUtils.isBossBarActive();
        eventMap.put(BOSS, active);
    }
}

