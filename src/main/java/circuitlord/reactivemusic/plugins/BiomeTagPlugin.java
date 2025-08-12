package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;

import java.util.List;
import java.util.Map;

public final class BiomeTagPlugin extends ReactiveMusicPlugin {
    public BiomeTagPlugin() {
        super("reactivemusic", "biome_tag");
    }
    @Override public void init() { /* no-op (SongPicker already builds BIOME_TAGS/map) */ }

    @Override public void gameTick(PlayerEntity player, World world, Map<EventRecord, Boolean> out) {
        if (player == null || world == null) return;

        BlockPos pos = player.getBlockPos();
        RegistryEntry<Biome> biome = world.getBiome(pos);

        // Collect current tags once
        List<TagKey<Biome>> currentTags = biome.streamTags().toList();

        // Mirror SongPicker’s original per-tick loop: compare by tag.id() identity
        for (TagKey<Biome> tag : SongPicker.BIOME_TAGS) {
            boolean found = false;
            for (TagKey<Biome> cur : currentTags) {
                if (cur.id() == tag.id()) { // keep the same non-Fabric-safe identity check
                    found = true;
                    break;
                }
            }
            SongPicker.biomeTagEventMap.put(tag, found); // isEntryValid() reads this
        }
    }
}