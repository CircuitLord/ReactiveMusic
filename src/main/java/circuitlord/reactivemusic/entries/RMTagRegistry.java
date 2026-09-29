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
import circuitlord.reactivemusic.entries.predicate.RMStructurePredicate;

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

        // combat: entity/attacker shorthand, now with boss-identity detection
        // (boss bar color is hardcoded per boss type in vanilla - see SongPicker.tickEventMap)
        TAGS.put("WARDEN", () -> new RMAttackerPredicate("warden"));
        TAGS.put("NEAR_WARDEN", () -> new RMNearbyEntityPredicate("warden", 40));
        TAGS.put("DRAGON", () -> new RMAttackerPredicate("ender_dragon"));
        TAGS.put("NEAR_DRAGON", () -> new RMOrPredicate(List.of(
                new RMNearbyEntityPredicate("ender_dragon", 120),
                new RMStatePredicate(() -> SongPicker.currentDragonBossBar))));
        TAGS.put("WITHER", () -> new RMAttackerPredicate("wither"));
        TAGS.put("NEAR_WITHER", () -> new RMOrPredicate(List.of(
                new RMNearbyEntityPredicate("wither", 60),
                new RMStatePredicate(() -> SongPicker.currentWitherBossBar))));
        TAGS.put("GUARDIAN", () -> new RMAttackerPredicate("elder_guardian"));
        TAGS.put("RAID", () -> new RMStatePredicate(() -> SongPicker.currentRaidBossBar));

        // structure shorthand - real registry ids verified against the actual 26.1.2 data
        // (a couple of tags.md's guesses didn't match anything: ocean_monument -> monument,
        // jungle_temple -> jungle_pyramid; matching is substring-based like BIOME=/ENTITY=,
        // so e.g. STRUCT=mineshaft also catches mineshaft_mesa)
        TAGS.put("STRONGHOLD", () -> new RMStructurePredicate("stronghold"));
        TAGS.put("FORTRESS", () -> new RMStructurePredicate("fortress"));
        TAGS.put("BASTION", () -> new RMStructurePredicate("bastion_remnant"));
        TAGS.put("MANSION", () -> new RMStructurePredicate("mansion"));
        TAGS.put("MONUMENT", () -> new RMStructurePredicate("monument"));
        TAGS.put("MINESHAFT", () -> new RMStructurePredicate("mineshaft"));
        TAGS.put("ANCIENT_CITY", () -> new RMStructurePredicate("ancient_city"));
        TAGS.put("END_CITY", () -> new RMStructurePredicate("end_city"));
        TAGS.put("TRIAL", () -> new RMStructurePredicate("trial_chambers"));
        TAGS.put("PYRAMID", () -> new RMStructurePredicate("desert_pyramid"));
        TAGS.put("TEMPLE", () -> new RMStructurePredicate("jungle_pyramid"));
        TAGS.put("OUTPOST", () -> new RMStructurePredicate("pillager_outpost"));
        TAGS.put("SHIPWRECK", () -> new RMStructurePredicate("shipwreck"));
    }

    /**
     * @return a fresh predicate for the given bare tag name (case-insensitive), or null if unknown.
     */
    public static RMPredicate resolve(String name) {
        Supplier<RMPredicate> factory = TAGS.get(name.toUpperCase());
        return factory == null ? null : factory.get();
    }
}
