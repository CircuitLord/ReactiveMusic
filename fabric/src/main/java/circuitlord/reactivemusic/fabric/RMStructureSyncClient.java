package circuitlord.reactivemusic.fabric;

import circuitlord.reactivemusic.SongPicker;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.stream.Collectors;

public class RMStructureSyncClient {

    public static void init() {
        // Fabric API runs payload receivers on the client thread already, no manual client.execute() needed
        ClientPlayNetworking.registerGlobalReceiver(RMStructureSyncPayload.TYPE, (payload, context) -> {
            SongPicker.onStructureSyncReceived(toStrings(payload.structures()), toStrings(payload.tags()));
        });
    }

    private static Set<String> toStrings(Set<Identifier> ids) {
        return ids.stream().map(Identifier::toString).collect(Collectors.toUnmodifiableSet());
    }
}
