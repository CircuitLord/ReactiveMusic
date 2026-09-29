package circuitlord.reactivemusic.entries.predicate;

import circuitlord.reactivemusic.SongPicker;

public class RMStructurePredicate implements RMPredicate {

    private final String structureId;

    public RMStructurePredicate(String structureId) {
        this.structureId = structureId;
    }

    @Override
    public boolean test() {
        return SongPicker.currentStructureIds.stream().anyMatch(id -> id.contains(structureId));
    }
}
