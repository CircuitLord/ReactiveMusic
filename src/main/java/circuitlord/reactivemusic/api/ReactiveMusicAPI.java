package circuitlord.reactivemusic.api;

import java.util.List;

import circuitlord.reactivemusic.*;
import circuitlord.reactivemusic.api.audio.ReactivePlayerManager;
import circuitlord.reactivemusic.api.songpack.RuntimeEntry;
import circuitlord.reactivemusic.api.songpack.SongpackZip;
import circuitlord.reactivemusic.impl.songpack.RMSongpackLoader;

public interface ReactiveMusicAPI {
    public interface ModConfig {
        static boolean debugModeEnabled() { return ReactiveMusic.modConfig.debugModeEnabled; }
    }

    /**
     * API view for the anything related to the EVENT system should go here.
     * This allows for expandability in the future past the event system, and better
     * modularity.
     */
    public interface EventSys { 
    }
    
    /**
     * API view for anything related to SONGPACKS should go here.
     * 
     * TODO: Add a method to inject a code-defined songpack object at runtime
     * (for plugins that want to define their own songs/events without a zip file)
     * 
     * This is kept separate from the "Song Selection" utilities in ReactiveMusicUtils
     * because song selection is a more general-purpose utility that can be used
     * by plugins and other systems that don't need to know about songpacks.
     * 
     * In the future, if we add more songpack-related functionality, and abstract it
     * away from the core ReactiveMusicState, we might want to add more to this interface
     * to allow plugins to define and interact with those same structured systems.
     * @see ReactiveMusicUtils
     * @see SongpackZip
     */
    public interface Songpack {
        static SongpackZip getCurrent() { return ReactiveMusicState.currentSongpack; }
        static List<SongpackZip> getAvailable() { return List.copyOf(RMSongpackLoader.availableSongpacks); }

        static RuntimeEntry currentEntry() { return ReactiveMusicState.currentEntry; }
        static String currentSong() { return ReactiveMusicState.currentSong; }
        static List<String> recentSongs() { return ReactiveMusicState.recentlyPickedSongs; }
        static List<RuntimeEntry> validEntries() { return List.copyOf(ReactiveMusicState.validEntries); }
        static List<RuntimeEntry> loadedEntries() { return List.copyOf(ReactiveMusicState.loadedEntries); }
        static List<RuntimeEntry> previousValidEntries() { return List.copyOf(ReactiveMusicState.previousValidEntries); }
    }

    static ReactivePlayerManager audioManager() { return ReactiveMusic.audio(); }


}
