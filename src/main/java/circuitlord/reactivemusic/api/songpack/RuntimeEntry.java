package circuitlord.reactivemusic.api.songpack;

import java.util.List;

import circuitlord.reactivemusic.impl.songpack.RMEntryCondition;
import circuitlord.reactivemusic.impl.songpack.RMRuntimeEntry;

/** Marker for type-safety without exposing internals.*/
public interface RuntimeEntry {
    /**
     * Not implemented yet. TODO: Second parse of the yaml?
     * The dynamic keys can't be typecast beforehand, so we need to get them as a raw map.
     * @return External option defined in the yaml config.
     * @see RMRuntimeEntry#setExternalOption(String key, Object value)
     */
    Object getExternalOption(String key);

    String getSongpack();
    String getEventString();
    String getErrorString();
    List<String> getSongs();
    
    boolean fallbackAllowed();
    boolean shouldOverlay();
    
    boolean shouldStopMusicOnValid();
    boolean shouldStopMusicOnInvalid();
    boolean shouldStartMusicOnValid();
    float getForceChance();
    
    List<RMEntryCondition> getConditions();
    
    /***************************************************************
     * TODO: These should be removed and refactored ⚠️
     * all of the entries use the same random number internally
     * so there is no reason to hold it within the entry
     */
    @Deprecated float getCachedRandomChance();
    @Deprecated void setCachedRandomChance(float c);
    
}
