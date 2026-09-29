package circuitlord.reactivemusic.entries.predicate;

import java.util.List;

public class RMAndPredicate implements RMPredicate {

    private final List<RMPredicate> children;

    public RMAndPredicate(List<RMPredicate> children) {
        this.children = children;
    }

    @Override
    public boolean test() {
        for (RMPredicate child : children) {
            if (!child.test()) return false;
        }
        return true;
    }
}
