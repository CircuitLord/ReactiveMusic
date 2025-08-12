package circuitlord.reactivemusic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import circuitlord.reactivemusic.api.eventsys.PluginIdentifier;
import circuitlord.reactivemusic.api.songpack.RuntimeEntry;
import circuitlord.reactivemusic.api.songpack.SongpackZip;
import circuitlord.reactivemusic.api.eventsys.EventRecord;

public final class ReactiveMusicState {
    
    private ReactiveMusicState() {}
    
    public static final Logger LOGGER = LoggerFactory.getLogger("reactive_music");

    public static SongpackZip currentSongpack = null;
    public static Boolean currentDimBlacklisted = false;
    
    public static Map<PluginIdentifier, Boolean> logicFreeze = new HashMap<>();
    public static Map<EventRecord, Boolean> songpackEventMap = new HashMap<>();
    
    public static RuntimeEntry currentEntry = null;
    public static String currentSong = null;

    public static List<RuntimeEntry> validEntries = new ArrayList<>();
    public static List<RuntimeEntry> loadedEntries = new ArrayList<>();
    public static List<RuntimeEntry> previousValidEntries = new ArrayList<>();
    public static List<String>         recentlyPickedSongs = new ArrayList<>();

}

