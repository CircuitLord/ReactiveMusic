package circuitlord.reactivemusic.api.songpack;

import java.util.Map;

import circuitlord.reactivemusic.impl.eventsys.RMEventRecord;
import circuitlord.reactivemusic.impl.songpack.RMSongpackEvent;

/**
 * This had to be structured as a coupling.
 * This is the core of RM, please be careful if you are going to touch or change this.
 * @see RMSongpackEvent
 */
public interface SongpackEvent {
    // Do not leak impl here
    Map<String, RMEventRecord> getMap();

    // Static API that delegates to the impl
    static RMEventRecord get(String id) { return RMSongpackEvent.get(id); }
    static RMEventRecord register(RMEventRecord eventRecord) { return RMSongpackEvent.register(eventRecord); }
    static RMEventRecord[] values() { return RMSongpackEvent.values(); }

    // Optional: expose predefined constants as interface-typed fields
    RMEventRecord NONE = RMSongpackEvent.NONE;
    RMEventRecord MAIN_MENU = RMSongpackEvent.MAIN_MENU;
    RMEventRecord CREDITS = RMSongpackEvent.CREDITS;
    RMEventRecord GENERIC = RMSongpackEvent.GENERIC;
}
