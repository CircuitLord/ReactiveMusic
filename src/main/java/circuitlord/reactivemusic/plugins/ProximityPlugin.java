package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class ProximityPlugin extends ReactiveMusicPlugin {
    public ProximityPlugin() {
        super("reactivemusic", "proximity");
    }
    private static EventRecord NEARBY_MOBS, VILLAGE;

    @Override public void init() {
        registerSongpackEvents("NEARBY_MOBS", "VILLAGE");
        NEARBY_MOBS = SongpackEvent.get("NEARBY_MOBS");
        VILLAGE     = SongpackEvent.get("VILLAGE");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<EventRecord, Boolean> eventMap) {
        if (player == null || world == null) return;

        // Nearby mobs
        var hostiles = ReactiveMusicUtils.getEntitiesInSphere(HostileEntity.class, player, 12.0, null);
        boolean mobsNearby = !hostiles.isEmpty();
        eventMap.put(NEARBY_MOBS, mobsNearby);

        // Village proximity (simple heuristic using VillageManager distance)
        var villagers = ReactiveMusicUtils.getEntitiesInSphere(VillagerEntity.class, player, 30.0, null);
        boolean inVillage = !villagers.isEmpty();
        eventMap.put(VILLAGE, inVillage);
    }
}