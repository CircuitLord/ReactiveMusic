package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.registry.entry.RegistryEntry;

public final class BiomeIdentityPlugin extends ReactiveMusicPlugin {
    public BiomeIdentityPlugin() {
        super("reactivemusic", "biome_id");
    }
    @Override public void init() { /* no-op */ }

    @Override
    public void gameTick(PlayerEntity player, World world, java.util.Map<EventRecord, Boolean> out) {
        if (player == null || world == null) return;

        BlockPos pos = player.getBlockPos();
        RegistryEntry<Biome> entry = world.getBiome(pos);

        // Mirror SongPicker’s original assignment of currentBiomeName
        String name = entry.getKey()
                .map(k -> k.getValue().toString())
                .orElse("[unregistered]");
        SongPicker.currentBiomeName = name; // isEntryValid() uses this
    }
}

