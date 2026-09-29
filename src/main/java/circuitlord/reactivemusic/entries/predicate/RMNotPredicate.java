package circuitlord.reactivemusic.entries.predicate;

public class RMNotPredicate implements RMPredicate {

    private final RMPredicate inner;

    public RMNotPredicate(RMPredicate inner) {
        this.inner = inner;
    }

    @Override
    public boolean test() {
        return !inner.test();
    }
}
