package circuitlord.reactivemusic.entries.predicate;

import circuitlord.reactivemusic.SongPicker;

public class RMDimensionPredicate implements RMPredicate {

    private final String dimensionName;

    public RMDimensionPredicate(String dimensionName) {
        this.dimensionName = dimensionName;
    }

    @Override
    public boolean test() {
        return SongPicker.currentDimName.contains(dimensionName);
    }
}
