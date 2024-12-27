package circuitlord.reactivemusic;

import net.minecraft.loot.entry.TagEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class SongpackEntry {


    // expands out into songpack events and biometag events
    public String[] events;

    // deprecated
    public boolean alwaysStop = false;
    // deprecated
    public boolean alwaysPlay = false;

    public boolean allowFallback = true;

    public boolean stopMusicOnEventChanged = false;

    // deprecated for now
    public boolean stackable = false;

    public String[] songs;


}
