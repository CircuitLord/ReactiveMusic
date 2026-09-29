package circuitlord.reactivemusic.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Runs on both physical sides (client and dedicated server), including
 * singleplayer's integrated server - unlike a "server"-only entrypoint,
 * which never fires in singleplayer.
 */
public class ReactiveMusicFabricCommon implements ModInitializer {

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(RMStructureSyncPayload.TYPE, RMStructureSyncPayload.CODEC);

        RMStructureSyncServer.init();
    }
}
