package circuitlord.reactivemusic.entries.predicate;

import circuitlord.reactivemusic.SongPicker;

public class RMNearbyEntityPredicate implements RMPredicate {

    private final String entityId;
    private final double radius;

    public RMNearbyEntityPredicate(String entityId, double radius) {
        this.entityId = entityId;
        this.radius = radius;
    }

    @Override
    public boolean test() {
        for (var nearby : SongPicker.cachedNearbyEntities) {
            if (nearby.distance <= radius && nearby.typeId.contains(entityId)) {
                return true;
            }
        }
        return false;
    }
}
