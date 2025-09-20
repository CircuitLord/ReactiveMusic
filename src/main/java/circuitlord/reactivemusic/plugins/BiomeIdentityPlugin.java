package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

public final class BiomeIdentityPlugin extends ReactiveMusicPlugin {
    public BiomeIdentityPlugin() {
        super("reactivemusic", "biome_id");
    }
    @Override public void init() { /* no-op */ }

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, java.util.Map<EventRecord, Boolean> out) {
        if (player == null || world == null) return;

        MinecraftVector3 pos = player.location().pos();
        SongPicker.currentBiomeName = world.blocks().getBiomeAt(pos); // isEntryValid() uses this
    }
}

