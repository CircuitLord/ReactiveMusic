package circuitlord.reactivemusic.impl.songpack;

import java.util.*;

import circuitlord.reactivemusic.ReactiveMusicDebug;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import circuitlord.reactivemusic.impl.eventsys.RMEventRecord;
import circuitlord.reactivemusic.impl.eventsys.RMPluginIdentifier;

/**
 * This is coupled to the API's matching interface.
 * @see SongpackEvent
 */
public final class RMSongpackEvent implements SongpackEvent {

    private static final Map<String, RMEventRecord> REGISTRY = new HashMap<>();
    
    /** If for some reason we need to get the event map outside of where it is provided... */
    @Override public Map<String, RMEventRecord> getMap() { return REGISTRY; }


    public static RMEventRecord register(RMEventRecord eventRecord) {
        ReactiveMusicDebug.LOGGER.info("Registering [" + eventRecord.getPluginId().getId() + "] event: " + eventRecord.getEventId());
        return REGISTRY.computeIfAbsent(eventRecord.getEventId(), k -> {return eventRecord;});
    }
    
    private static RMEventRecord builtIn(String eventId) {
        RMPluginIdentifier pluginId = new RMPluginIdentifier("reactivemusic", "standard_events");
        RMEventRecord eventRecord = new RMEventRecord(eventId, pluginId);
        return register(eventRecord);
    }
    
    public static RMEventRecord[] values() {
        return REGISTRY.values().toArray(new RMEventRecord[0]);
    }
    
    public static RMEventRecord get(String id) { return REGISTRY.get(id); }


    public static final RMEventRecord NONE = builtIn("NONE");
    public static final RMEventRecord MAIN_MENU = builtIn("MAIN_MENU");
    public static final RMEventRecord CREDITS = builtIn("CREDITS");
    public static final RMEventRecord GENERIC = builtIn("GENERIC");
}