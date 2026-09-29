package circuitlord.reactivemusic.entries.predicate;

import circuitlord.reactivemusic.SongPicker;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public class RMBiomeTagPredicate implements RMPredicate {

    private final TagKey<Biome> tag;

    public RMBiomeTagPredicate(TagKey<Biome> tag) {
        this.tag = tag;
        SongPicker.registerBiomeTag(tag);
    }

    @Override
    public boolean test() {
        return SongPicker.biomeTagEventMap.containsKey(tag) && SongPicker.biomeTagEventMap.get(tag);
    }
}
