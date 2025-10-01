package circuitlord.reactivemusic.impl.eventsys;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import net.minecraft.entity.player.PlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/** Internal cache of last-known conditions per player.
 * This is not implemented into any feature as of yet.
 * It is here for possible server-side functionality using
 * a bridging plugin for client-server communication.
 */
public final class RMEventState {
    private RMEventState() {}
    private static final Map<UUID, Map<SongpackEvent, Boolean>> LAST = new ConcurrentHashMap<>();

    public static void updateForPlayer(PlayerEntity player, Map<SongpackEvent, Boolean> conditions) {
        if (player == null || conditions == null) return;
        LAST.put(player.getUuid(), Collections.unmodifiableMap(new HashMap<>(conditions)));
    }

    public static Map<SongpackEvent, Boolean> snapshot(UUID playerId) {
        Map<SongpackEvent, Boolean> m = LAST.get(playerId);
        return (m != null) ? m : Collections.emptyMap();
    }

    public static void clear(UUID playerId) { LAST.remove(playerId); }
    public static void clearAll() { LAST.clear(); }
}
