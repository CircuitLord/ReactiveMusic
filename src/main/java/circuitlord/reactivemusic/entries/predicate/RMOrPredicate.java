package circuitlord.reactivemusic.entries.predicate;

import java.util.List;

public class RMOrPredicate implements RMPredicate {

    private final List<RMPredicate> children;

    public RMOrPredicate(List<RMPredicate> children) {
        this.children = children;
    }

    @Override
    public boolean test() {
        for (RMPredicate child : children) {
            if (child.test()) return true;
        }
        return false;
    }
}
