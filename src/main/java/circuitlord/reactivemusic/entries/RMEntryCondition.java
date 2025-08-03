package circuitlord.reactivemusic.entries;

import circuitlord.reactivemusic.SongpackEventType;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class RMEntryCondition {

    // the way conditions work just means that each condition requires there to be at least one true in each list (or empty list) for the whole condition to be valid
    // This is how we handle ORs

    public List<SongpackEventType> songpackEvents = new ArrayList<>();
    public List<String> biomeTypes = new ArrayList<>();

    public List<String> dimTypes = new ArrayList<>();

    public List<TagKey<Biome>> biomeTags = new ArrayList<>();

    public List<RMEntryBlockCondition> blocks = new ArrayList<>();

}
