package circuitlord.reactivemusic.entries;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.SongpackEntry;
import circuitlord.reactivemusic.SongpackEventType;
import circuitlord.reactivemusic.SongpackZip;
import circuitlord.reactivemusic.platform.BiomeTagHelper;
//? if >=1.20 {
/*import net.minecraft.registry.tag.TagKey;
*///?} else {
import net.minecraft.tag.TagKey;
//?}
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RMRuntimeEntry {


    public List<RMEntryCondition> conditions = new ArrayList<>();

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

            RMEntryCondition condition = new RMEntryCondition();

            String cleanedEvent = event.replaceAll("\\s", "");
            cleanedEvent = cleanedEvent.toLowerCase();

            // backwards compat with v0.5
            if (songpack.convertBiomeToBiomeTag) {
                cleanedEvent = cleanedEvent.replace("biome=", "biometag=");
            }

            // Split by "||"
            String[] eventSections = cleanedEvent.split("\\|\\|");

            boolean eventHasData = false;

            // Parse each event section (may only be one)
            for (String eventSection : eventSections) {

                if (eventSection.startsWith("block=")) {

                    String blockData = eventSection.substring(6);
                    String[] parts = blockData.split(",");

                    // make sure it's a number, dunno why this is the syntax for that
                    if (parts.length >= 2 && parts[1].matches("\\d+")) {

                        RMEntryBlockCondition blockCond = new RMEntryBlockCondition();
                        blockCond.block = parts[0];
                        blockCond.requiredCount = Integer.parseInt(parts[1]);

                        condition.blocks.add(blockCond);

                        eventHasData = true;
                    }
                    else {
                        Entry.errorString += "Invalid syntax: " + eventSection + "!\n\n";
                    }
                }

                // see if it's a biome event
                else if (eventSection.startsWith("biome=")) {

                    String biomeName = eventSection.substring(6);
                    if (biomeName.isEmpty())
                        continue;

                    condition.biomeTypes.add(biomeName);
                    eventHasData = true;
                }

                // Biome-tags
                else if (eventSection.startsWith("biometag=")) {

                    String rawTagString = eventSection.substring(9);
                    if (rawTagString.isEmpty())
                        continue;

                    String path = normalizeBiomeTagPath(rawTagString);
                    TagKey<Biome> biomeTag = BiomeTagHelper.INSTANCE.createBiomeTagKey(path);

                    if (biomeTag != null) {
                        condition.biomeTags.add(biomeTag);
                        SongPicker.registerBiomeTag(biomeTag);
                        eventHasData = true;
                    } else {
                        Entry.errorString += "Biome tag '" + rawTagString + "' is not available on this platform!\n\n";
                    }

                }

                // dimensions
                else if (eventSection.startsWith("dim=")) {
                    String dimName = eventSection.substring(4);
                    if (dimName.isEmpty())
                        continue;

                    condition.dimTypes.add(dimName);
                    eventHasData = true;
                }

                // songpack events
                else {
                    try {
                        // try to cast to SongpackEvent
                        // needs uppercase for enum names
                        SongpackEventType eventType = Enum.valueOf(SongpackEventType.class, eventSection.toUpperCase());

                        // it's a songpack event
                        if (eventType != SongpackEventType.NONE) {
                            condition.songpackEvents.add(eventType);
                            eventHasData = true;
                            continue;
                        }
                    } catch (Exception e) {
                        Entry.errorString += "Could not find event with name " + eventSection + "!\n\n";
                        //e.printStackTrace();
                    }
                }
            }

            // --- If we didn't find any valid conditions, skip this ---
            if (!eventHasData) {
                continue;
            }

            Entry.conditions.add(condition);

        }

        return Entry;

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



