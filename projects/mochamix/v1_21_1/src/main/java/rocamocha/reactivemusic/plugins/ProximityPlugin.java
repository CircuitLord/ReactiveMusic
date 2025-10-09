package rocamocha.reactivemusic.plugins;

import rocamocha.reactivemusic.api.*;
import rocamocha.reactivemusic.api.eventsys.EventRecord;
import rocamocha.reactivemusic.api.songpack.SongpackEvent;
import net.minecraft.client.MinecraftClient;

// TODO: find a way to remove these leaks?
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

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
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        if (player == null || world == null) return;

        PlayerEntity mcplayer = MinecraftClient.getInstance().player;

        // Nearby mobs
        var hostiles = ReactiveMusicUtils.getEntitiesInSphere(HostileEntity.class, mcplayer, 12.0, null);
        boolean mobsNearby = !hostiles.isEmpty();
        eventMap.put(NEARBY_MOBS, mobsNearby);

        // Village proximity (simple heuristic using VillageManager distance)
        var villagers = ReactiveMusicUtils.getEntitiesInSphere(VillagerEntity.class, mcplayer, 30.0, null);
        boolean inVillage = !villagers.isEmpty();
        eventMap.put(VILLAGE, inVillage);
    }
}