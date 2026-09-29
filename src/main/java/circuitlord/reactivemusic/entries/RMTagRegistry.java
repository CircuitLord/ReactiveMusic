package circuitlord.reactivemusic.entries;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.SongpackEventType;
import circuitlord.reactivemusic.entries.predicate.RMAttackerPredicate;
import circuitlord.reactivemusic.entries.predicate.RMDimensionPredicate;
import circuitlord.reactivemusic.entries.predicate.RMNearbyEntityPredicate;
import circuitlord.reactivemusic.entries.predicate.RMNotPredicate;
import circuitlord.reactivemusic.entries.predicate.RMOrPredicate;
import circuitlord.reactivemusic.entries.predicate.RMAndPredicate;
import circuitlord.reactivemusic.entries.predicate.RMPredicate;
import circuitlord.reactivemusic.entries.predicate.RMSongpackEventPredicate;
import circuitlord.reactivemusic.entries.predicate.RMStatePredicate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Bare-word tags (no "key=value" prefix): both the ones that are just a direct
 * SongpackEventType lookup, and the composite ones from tags.md that expand
 * into a small predicate tree over dimension/height/health/entity checks.
 */
public class RMTagRegistry {

    private static final Map<String, Supplier<RMPredicate>> TAGS = new HashMap<>();

    private static RMPredicate event(SongpackEventType type) {
        return new RMSongpackEventPredicate(type);
    }

    static {
        // Direct SongpackEventType-backed tags
        for (SongpackEventType type : SongpackEventType.values()) {
            if (type == SongpackEventType.NONE) continue;
            TAGS.put(type.name(), () -> event(type));
        }

        // dimension (also reachable via DIM=, kept here as bare words per tags.md)
        TAGS.put("END", () -> new RMDimensionPredicate("end"));
        TAGS.put("NETHER", () -> new RMDimensionPredicate("nether"));
        TAGS.put("OVERWORLD", () -> new RMDimensionPredicate("overworld"));

        // weather
        TAGS.put("CLEAR", () -> new RMNotPredicate(new RMOrPredicate(List.of(event(SongpackEventType.RAIN), event(SongpackEventType.STORM)))));

        // location: dimension + height/sky composites
        TAGS.put("ROOF", () -> new RMAndPredicate(List.of(
                new RMDimensionPredicate("nether"),
                new RMStatePredicate(() -> SongPicker.currentPlayerY > 128))));
        TAGS.put("CLOUDS", () -> new RMAndPredicate(List.of(
                new RMDimensionPredicate("overworld"),
                new RMStatePredicate(() -> SongPicker.currentPlayerY > 320))));
        TAGS.put("PEAKS", () -> new RMAndPredicate(List.of(
                new RMDimensionPredicate("overworld"),
                new RMStatePredicate(() -> SongPicker.currentPlayerY > 220))));
        TAGS.put("LEVEL", () -> new RMAndPredicate(List.of(
                new RMDimensionPredicate("overworld"),
                new RMStatePredicate(() -> SongPicker.currentPlayerY > SongPicker.currentSeaLevel))));
        TAGS.put("UNDER", () -> new RMAndPredicate(List.of(
                new RMDimensionPredicate("overworld"),
                new RMStatePredicate(() -> SongPicker.currentSkyHidden))));
        TAGS.put("DEEP", () -> new RMAndPredicate(List.of(
                new RMDimensionPredicate("overworld"),
                new RMStatePredicate(() -> SongPicker.currentPlayerY < 0))));

        // combat: health thresholds (hp on Minecraft's 0-20 scale, as in tags.md)
        TAGS.put("LOW", () -> new RMStatePredicate(() -> SongPicker.currentHealth < 10f));
        TAGS.put("CRIT", () -> new RMStatePredicate(() -> SongPicker.currentHealth < 4f));

        // combat: entity/attacker shorthand
        // NOTE: the "...or <boss> bossbar is visible" alternative from tags.md isn't included yet -
        // that needs boss-identity detection (which boss a bossbar belongs to), not just entity/attacker checks.
        TAGS.put("WARDEN", () -> new RMAttackerPredicate("warden"));
        TAGS.put("NEAR_WARDEN", () -> new RMNearbyEntityPredicate("warden", 40));
        TAGS.put("DRAGON", () -> new RMAttackerPredicate("ender_dragon"));
        TAGS.put("NEAR_DRAGON", () -> new RMNearbyEntityPredicate("ender_dragon", 120));
        TAGS.put("WITHER", () -> new RMAttackerPredicate("wither"));
        TAGS.put("NEAR_WITHER", () -> new RMNearbyEntityPredicate("wither", 60));
        TAGS.put("GUARDIAN", () -> new RMAttackerPredicate("elder_guardian"));
    }

    /**
     * @return a fresh predicate for the given bare tag name (case-insensitive), or null if unknown.
     */
    public static RMPredicate resolve(String name) {
        Supplier<RMPredicate> factory = TAGS.get(name.toUpperCase());
        return factory == null ? null : factory.get();
    }
}
