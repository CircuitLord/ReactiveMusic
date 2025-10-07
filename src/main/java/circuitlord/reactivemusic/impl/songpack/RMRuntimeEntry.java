package circuitlord.reactivemusic.impl.songpack;

import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.RuntimeEntry;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import circuitlord.reactivemusic.plugins.BiomeTagPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class RMRuntimeEntry implements RuntimeEntry {


    public List<RMEntryCondition> conditions = new ArrayList<>();
    
    public String songpack;
    
    public boolean allowFallback = false;
    public boolean useOverlay = false;
    
    public boolean forceStopMusicOnValid = false;
    public boolean forceStopMusicOnInvalid = false;
    public boolean forceStartMusicOnValid = false;
    public float forceChance = 1.0f;
    
    public List<String> songs = new ArrayList<>();
    
    public String eventString = "";
    public String errorString = "";
        
    public HashMap<String, Object> entryMap = new HashMap<>();
    
    public Object getExternalOption(String key) {
        return entryMap.get(key);
    }
    
    /** 
     * Should import values in the yaml that are *NOT* predefined
     * this means plugin devs can create custom options for events
     * that live in the YAML
     */
    public void setExternalOption(String key, Object value) {
        java.util.Set<String> knownOptions = java.util.Set.of(
            // Actual RMSongpackEntry properties
            "events", "songs", "allowFallback", "useOverlay", "forceStopMusicOnChanged",
            "forceStopMusicOnValid", "forceStopMusicOnInvalid", "forceStartMusicOnValid",
            "forceChance", "startMusicOnEventValid", "stackable", "alwaysPlay", "alwaysStop"
        );
        
        // Only store truly external options
        if (!knownOptions.contains(key)) {
            entryMap.put(key, value);
        }
    }
    
    // getters
    //--------------------------------------------------------------
    public String getEventString() { return eventString; }
    public String getErrorString() { return errorString; }
    public List<String> getSongs() { return songs; }
    public boolean fallbackAllowed() { return allowFallback; }
    public boolean shouldOverlay() { return useOverlay; }
    public float getForceChance() { return forceChance; }
    public boolean shouldStopMusicOnValid() { return forceStopMusicOnValid; }
    public boolean shouldStopMusicOnInvalid() { return forceStopMusicOnInvalid; }
    public boolean shouldStartMusicOnValid() { return forceStartMusicOnValid; }
    public String getSongpack() { return songpack; }
    public List<RMEntryCondition> getConditions() { return conditions; }

    public RMRuntimeEntry(RMSongpackZip songpack, RMSongpackEntry songpackEntry) {

        this.songpack = songpack.config.name;// songpackName;

        this.allowFallback = songpackEntry.allowFallback;
        this.useOverlay = songpackEntry.useOverlay;

        this.forceStopMusicOnValid = songpackEntry.forceStopMusicOnValid || songpackEntry.forceStopMusicOnChanged;
        this.forceStopMusicOnInvalid = songpackEntry.forceStopMusicOnInvalid || songpackEntry.forceStopMusicOnChanged;
        this.forceStartMusicOnValid = songpackEntry.forceStartMusicOnValid;
        this.forceChance = songpackEntry.forceChance;

        if (songpackEntry.songs != null) {
            this.songs = Arrays.stream(songpackEntry.songs).toList();
        }
        
        // Transfer external options from songpack entry
        if (songpackEntry.entryMap != null) {
            for (java.util.Map.Entry<String, Object> externalOption : songpackEntry.entryMap.entrySet()) {
                this.setExternalOption(externalOption.getKey(), externalOption.getValue());
            }
        }

        for (int i = 0; i < songpackEntry.events.length; i++) {
            this.eventString += songpackEntry.events[i] + "_";
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
                        this.errorString += "Invalid syntax: " + eventSection + "!\n\n";
                    }
                }

                // see if it's a biome event
                if (eventSection.startsWith("biome=")) {

                    String biomeName = eventSection.substring(6);
                    if (biomeName.isEmpty())
                        continue;

                    condition.biomeTypes.add(biomeName);
                    eventHasData = true;
                }

                // Biome-tags
                else if (eventSection.startsWith("biometag=")) {

                    String rawTagString = eventSection.substring(9);

                    // convert to v2 biome tag
                    String biomeTagName = cleanBiomeTagString(rawTagString);
                    if (biomeTagName.isEmpty())
                        continue;

                    // Loop over all the cached tags and see if we have anything matching this

                    boolean foundMatch = false;

                    for (int k = 0; k < BiomeTagPlugin.getBiomeTagFields().length; k++) {

                        // i love creating GC
                        String fieldName = BiomeTagPlugin.getBiomeTagFields()[k].getName();

                        // convert our cached field tag to the v2 format (even with v1) since we did the same for the loaded one
                        fieldName = cleanBiomeTagString(fieldName);

                        if (fieldName.equals(biomeTagName)) {

                            var biomeTag = BiomeTagPlugin.getBiomeTagFromField(BiomeTagPlugin.getBiomeTagFields()[k]);

                            if (biomeTag != null) {
                                // We found a match, now put the biometag key into the condition so we can use it later
                                condition.biomeTags.add(biomeTag);
                                eventHasData = true;

                                foundMatch = true;
                                break;
                            }
                        }
                    }

                    // we didn't find a match
                    if (!foundMatch) {
                        this.errorString += "Didn't find biometag with name: " + rawTagString + "!\n\n";
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
                        EventRecord eventRecord = SongpackEvent.get(eventSection.toUpperCase());

                        // it's a songpack event
                        if (eventRecord != SongpackEvent.NONE) {
                            condition.songpackEvents.add(eventRecord);
                            eventHasData = true;
                            continue;
                        }
                    } catch (Exception e) {
                        this.errorString += "Could not find event with name " + eventSection + "!\n\n";
                        //e.printStackTrace();
                    }
                }
            }

            // --- If we didn't find any valid conditions, skip this ---
            if (!eventHasData) {
                continue;
            }

            this.conditions.add(condition);

        }
    }


    public static String cleanBiomeTagString(String input) {
        input = input.toLowerCase();

        // handle cases where the start of the tag changed
        input = input.replace("is_", "");
        input = input.replace("in_", "");
        input = input.replace("climate_", "");

        // converting to 1.21 format with biometag v2


        input = input.replace("tree_coniferous", "coniferous_tree");

        return input;

    }


}



