package circuitlord.reactivemusic.fabric;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongPicker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.stream.Collectors;

public class RMStructureSyncClient {

    // generous margin over the server's own worst-case ~1s delay (throttled to once per 20
    // ticks, plus it always sends at least once for a brand new player) before concluding the
    // connected server doesn't support structure sync at all
    private static final int MISSING_SUPPORT_TIMEOUT_TICKS = 100;

    private static boolean connected = false;
    private static boolean receivedAnySync = false;
    private static boolean loggedMissingServerSupport = false;
    private static int ticksSinceJoin = 0;

    public static void init() {
        // Fabric API runs payload receivers on the client thread already, no manual client.execute() needed
        ClientPlayNetworking.registerGlobalReceiver(RMStructureSyncPayload.TYPE, (payload, context) -> {
            receivedAnySync = true;
            SongPicker.onStructureSyncReceived(toStrings(payload.structures()), toStrings(payload.tags()));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            connected = true;
            receivedAnySync = false;
            loggedMissingServerSupport = false;
            ticksSinceJoin = 0;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> connected = false);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!connected || receivedAnySync || loggedMissingServerSupport) return;

            if (++ticksSinceJoin > MISSING_SUPPORT_TIMEOUT_TICKS) {
                loggedMissingServerSupport = true;
                ReactiveMusic.LOGGER.error("[ReactiveMusic]: This server doesn't appear to have ReactiveMusic's "
                        + "structure sync (either the mod isn't installed server-side, or the "
                        + "reactivemusicStructureSync gamerule is disabled). STRUCT=/STRUCTTAG= and the structure "
                        + "shorthand tags will not work this session.");
            }
        });
    }

    private static Set<String> toStrings(Set<Identifier> ids) {
        return ids.stream().map(Identifier::toString).collect(Collectors.toUnmodifiableSet());
    }
}
