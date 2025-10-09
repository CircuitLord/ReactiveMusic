package rocamocha.reactivemusic.plugins;

import rocamocha.reactivemusic.api.*;
import rocamocha.reactivemusic.api.eventsys.EventRecord;
import rocamocha.reactivemusic.api.songpack.SongpackEvent;
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

