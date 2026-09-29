package circuitlord.reactivemusic.entries;

import circuitlord.reactivemusic.SongpackEventType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

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

    // nearby entities of a given type, regardless of relation to the player
    public List<RMEntryEntityCondition> nearbyEntities = new ArrayList<>();

    // entities that recently attacked/were attacked by the player, matched by type
    public List<RMEntryEntityCondition> attackers = new ArrayList<>();

}
