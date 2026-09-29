package circuitlord.reactivemusic.entries;

import circuitlord.reactivemusic.SongpackEntry;
import circuitlord.reactivemusic.SongpackZip;
import circuitlord.reactivemusic.platform.BiomeTagHelper;
import circuitlord.reactivemusic.entries.predicate.RMAttackerPredicate;
import circuitlord.reactivemusic.entries.predicate.RMBiomeNamePredicate;
import circuitlord.reactivemusic.entries.predicate.RMBiomeTagPredicate;
import circuitlord.reactivemusic.entries.predicate.RMDimensionPredicate;
import circuitlord.reactivemusic.entries.predicate.RMNearbyEntityPredicate;
import circuitlord.reactivemusic.entries.predicate.RMNotPredicate;
import circuitlord.reactivemusic.entries.predicate.RMOrPredicate;
import circuitlord.reactivemusic.entries.predicate.RMPredicate;
import circuitlord.reactivemusic.entries.predicate.RMStructurePredicate;
import circuitlord.reactivemusic.entries.predicate.RMStructureTagPredicate;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RMRuntimeEntry {


    public List<RMPredicate> conditions = new ArrayList<>();

    public String songpack;

    public boolean allowFallback = false;

    public boolean forceStopMusicOnValid = false;
    public boolean forceStopMusicOnInvalid = false;

    public boolean forceStartMusicOnValid = false;

    public float forceChance = 1.0f;

    public List<String> songs = new ArrayList<>();

    public String eventString = "";

    public String errorString = "";

    public float cachedRandomChance = 1.0f;


    public static RMRuntimeEntry create(SongpackZip songpack, SongpackEntry songpackEntry) {

        RMRuntimeEntry Entry = new RMRuntimeEntry();
        Entry.songpack = songpack.config.name;// songpackName;

        Entry.allowFallback = songpackEntry.allowFallback;

        Entry.forceStopMusicOnValid = songpackEntry.forceStopMusicOnValid || songpackEntry.forceStopMusicOnChanged;
        Entry.forceStopMusicOnInvalid = songpackEntry.forceStopMusicOnInvalid || songpackEntry.forceStopMusicOnChanged;

        Entry.forceStartMusicOnValid = songpackEntry.forceStartMusicOnValid;

        Entry.forceChance = songpackEntry.forceChance;

        if (songpackEntry.songs != null) {
            Entry.songs = Arrays.stream(songpackEntry.songs).toList();
        }

        for (int i = 0; i < songpackEntry.events.length; i++) {
            Entry.eventString += songpackEntry.events[i] + "_";
        }

        for (String event : songpackEntry.events) {

            String cleanedEvent = event.replaceAll("\\s", "");
            cleanedEvent = cleanedEvent.toLowerCase();

            // backwards compat with v0.5
            if (songpack.convertBiomeToBiomeTag) {
                cleanedEvent = cleanedEvent.replace("biome=", "tag=");
            }

            // Split by "||" (OR)
            String[] eventSections = cleanedEvent.split("\\|\\|");

            List<RMPredicate> segmentPredicates = new ArrayList<>();

            // Parse each "!"-optional, "||"-separated segment (may only be one)
            for (String rawSection : eventSections) {

                boolean negate = rawSection.startsWith("!");
                String eventSection = negate ? rawSection.substring(1) : rawSection;

                RMPredicate predicate = resolveSegment(Entry, eventSection);
                if (predicate == null) continue;

                segmentPredicates.add(negate ? new RMNotPredicate(predicate) : predicate);
            }

            // --- If we didn't find any valid conditions, skip this ---
            if (segmentPredicates.isEmpty()) {
                continue;
            }

            RMPredicate combined = segmentPredicates.size() == 1
                    ? segmentPredicates.get(0)
                    : new RMOrPredicate(segmentPredicates);

            Entry.conditions.add(combined);

        }

        return Entry;

    }

    private static RMPredicate resolveSegment(RMRuntimeEntry Entry, String eventSection) {

        // see if it's a biome event
        if (eventSection.startsWith("biome=")) {

            String biomeName = eventSection.substring(6);
            if (biomeName.isEmpty())
                return null;

            return new RMBiomeNamePredicate(biomeName);
        }

        // Biome-tags
        else if (eventSection.startsWith("tag=")) {

            String rawTagString = eventSection.substring(4);
            if (rawTagString.isEmpty())
                return null;

            String path = normalizeBiomeTagPath(rawTagString);
            TagKey<Biome> biomeTag = BiomeTagHelper.INSTANCE.createBiomeTagKey(path);

            if (biomeTag != null) {
                return new RMBiomeTagPredicate(biomeTag);
            } else {
                Entry.errorString += "Biome tag '" + rawTagString + "' is not available on this platform!\n\n";
                return null;
            }
        }

        // nearby entities of a given type: entity=<id>[,radius]
        else if (eventSection.startsWith("entity=")) {

            String entityData = eventSection.substring(7);
            if (entityData.isEmpty())
                return null;

            String[] parts = entityData.split(",");
            double radius = 20.0;

            if (parts.length >= 2) {
                if (parts[1].matches("\\d+(\\.\\d+)?")) {
                    radius = Double.parseDouble(parts[1]);
                } else {
                    Entry.errorString += "Invalid radius in: " + eventSection + "!\n\n";
                    return null;
                }
            }

            return new RMNearbyEntityPredicate(parts[0], radius);
        }

        // entities that recently attacked/were attacked by the player: attacker=<id>
        else if (eventSection.startsWith("attacker=")) {

            String entityId = eventSection.substring(9);
            if (entityId.isEmpty())
                return null;

            return new RMAttackerPredicate(entityId);
        }

        // structures: struct=<id>
        else if (eventSection.startsWith("struct=")) {

            String structureId = eventSection.substring(7);
            if (structureId.isEmpty())
                return null;

            return new RMStructurePredicate(structureId);
        }

        // structure tags: structtag=<id>
        else if (eventSection.startsWith("structtag=")) {

            String tagId = eventSection.substring(10);
            if (tagId.isEmpty())
                return null;

            return new RMStructureTagPredicate(tagId);
        }

        // dimensions
        else if (eventSection.startsWith("dim=")) {
            String dimName = eventSection.substring(4);
            if (dimName.isEmpty())
                return null;

            return new RMDimensionPredicate(dimName);
        }

        // bare-word tags: plain SongpackEventType lookups and macro-expanded composites
        else {
            RMPredicate predicate = RMTagRegistry.resolve(eventSection);
            if (predicate == null) {
                Entry.errorString += "Could not find event with name " + eventSection + "!\n\n";
            }
            return predicate;
        }
    }


    public static String normalizeBiomeTagPath(String input) {
        String path = input.toLowerCase().trim();
        // Tags that don't start with a known prefix get "is_" prepended
        // This handles backward compat: "WASTELAND" -> "is_wasteland", "HOT" -> "is_hot"
        if (!path.startsWith("is_") && !path.startsWith("has_") && !path.startsWith("no_default_")
                && !path.startsWith("hidden_from_")) {
            path = "is_" + path;
        }
        return path;
    }


}
