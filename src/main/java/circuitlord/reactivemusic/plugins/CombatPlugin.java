package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

import java.util.Map;

public final class CombatPlugin extends ReactiveMusicPlugin {
    public CombatPlugin() {
        super("reactivemusic", "combat");
    }

    private static EventRecord DYING;
    private static final float THRESHOLD = 0.35f;

    @Override
    public void init() {
        registerSongpackEvents("DYING");

        DYING = SongpackEvent.get("DYING");
    }

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        if (player == null || world == null) return;
        boolean dying = (player.health() / player.maxHealth()) < THRESHOLD;
        eventMap.put(DYING, dying);
    }
}
