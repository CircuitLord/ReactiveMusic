package circuitlord.reactivemusic.entries.predicate;

import circuitlord.reactivemusic.SongPicker;

public class RMBlockCountPredicate implements RMPredicate {

    private final String block;
    private final int requiredCount;

    public RMBlockCountPredicate(String block, int requiredCount) {
        this.block = block;
        this.requiredCount = requiredCount;
    }

    @Override
    public boolean test() {
        for (var kvp : SongPicker.cachedBlockChecker.object2IntEntrySet()) {
            if (kvp.getKey().contains(block) && kvp.getIntValue() >= requiredCount) {
                return true;
            }
        }
        return false;
    }
}
