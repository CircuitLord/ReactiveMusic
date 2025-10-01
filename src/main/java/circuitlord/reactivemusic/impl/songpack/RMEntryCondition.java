package circuitlord.reactivemusic.impl.songpack;

import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

import circuitlord.reactivemusic.api.eventsys.EventRecord;

public class RMEntryCondition {

    public List<EventRecord> songpackEvents = new ArrayList<>();
    public List<String> biomeTypes = new ArrayList<>();
    public List<String> dimTypes = new ArrayList<>();
    public List<TagKey<Biome>> biomeTags = new ArrayList<>();
    public List<RMEntryBlockCondition> blocks = new ArrayList<>();

}
