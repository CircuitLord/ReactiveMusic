package circuitlord.reactivemusic.entries.predicate;

import circuitlord.reactivemusic.SongPicker;

public class RMBiomeNamePredicate implements RMPredicate {

    private final String biomeName;

    public RMBiomeNamePredicate(String biomeName) {
        this.biomeName = biomeName;
    }

    @Override
    public boolean test() {
        return SongPicker.currentBiomeName.contains(biomeName);
    }
}
