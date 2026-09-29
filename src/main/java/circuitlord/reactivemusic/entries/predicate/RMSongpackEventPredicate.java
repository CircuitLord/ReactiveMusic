package circuitlord.reactivemusic.entries.predicate;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.SongpackEventType;

public class RMSongpackEventPredicate implements RMPredicate {

    private final SongpackEventType type;

    public RMSongpackEventPredicate(SongpackEventType type) {
        this.type = type;
    }

    @Override
    public boolean test() {
        return SongPicker.songpackEventMap.containsKey(type) && SongPicker.songpackEventMap.get(type);
    }
}
