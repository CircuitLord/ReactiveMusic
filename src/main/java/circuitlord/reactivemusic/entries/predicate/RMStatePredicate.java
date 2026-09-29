package circuitlord.reactivemusic.entries.predicate;

import java.util.function.BooleanSupplier;

/**
 * Wraps an arbitrary live-state check (e.g. player height, health) so it can be
 * composed with the other predicates without a dedicated class per check.
 */
public class RMStatePredicate implements RMPredicate {

    private final BooleanSupplier check;

    public RMStatePredicate(BooleanSupplier check) {
        this.check = check;
    }

    @Override
    public boolean test() {
        return check.getAsBoolean();
    }
}
