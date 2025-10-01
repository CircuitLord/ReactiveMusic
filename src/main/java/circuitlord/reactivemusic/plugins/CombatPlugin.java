package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

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
    public void gameTick(PlayerEntity player, World world, Map<EventRecord, Boolean> eventMap) {
        if (player == null || world == null) return;
        boolean dying = (player.getHealth() / player.getMaxHealth()) < THRESHOLD;
        eventMap.put(DYING, dying);
    }
}
