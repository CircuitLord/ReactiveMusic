package circuitlord.reactivemusic.entries.predicate;

import circuitlord.reactivemusic.SongPicker;

public class RMAttackerPredicate implements RMPredicate {

    private final String entityId;

    public RMAttackerPredicate(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public boolean test() {
        for (var attackerEntity : SongPicker.recentEntityDamageSources.keySet()) {
            if (SongPicker.getEntityTypeId(attackerEntity.getType()).contains(entityId)) {
                return true;
            }
        }
        return false;
    }
}
