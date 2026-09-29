package circuitlord.reactivemusic.entries.predicate;

import circuitlord.reactivemusic.SongPicker;

public class RMStructureTagPredicate implements RMPredicate {

    private final String tagId;

    public RMStructureTagPredicate(String tagId) {
        this.tagId = tagId;
    }

    @Override
    public boolean test() {
        return SongPicker.currentStructureTagIds.stream().anyMatch(id -> id.contains(tagId));
    }
}
