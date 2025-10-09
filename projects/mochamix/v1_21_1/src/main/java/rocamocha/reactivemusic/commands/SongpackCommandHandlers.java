package rocamocha.reactivemusic.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import rocamocha.reactivemusic.ReactiveMusicDebug;
import rocamocha.reactivemusic.ReactiveMusicState;
import rocamocha.reactivemusic.ReactiveMusicDebug.TextBuilder;
import rocamocha.reactivemusic.api.eventsys.EventRecord;
import rocamocha.reactivemusic.api.songpack.RuntimeEntry;
import rocamocha.reactivemusic.plugins.OverlayTrackPlugin;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.util.Formatting;

public class SongpackCommandHandlers {

    public static int songpackInfo(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(ReactiveMusicDebug.NON_IMPL_WARN_BUILT);
        return 1;
    }
    
    
    public static int listValidEntries(CommandContext<FabricClientCommandSource> ctx) {
        int n = 0;
        TextBuilder validEntryList = new TextBuilder();
        
        validEntryList.header("VALID ENTRIES");
        for (RuntimeEntry entry : ReactiveMusicState.validEntries) {
            String eventString = entry != null ? entry.getEventString() : null;
            validEntryList.line(Integer.toString(n), eventString != null ? eventString : "<null>", Formatting.AQUA);
            n += 1;
        }
        validEntryList.raw("\n"+"There are a total of [ " + ReactiveMusicState.validEntries.size() + " ]  valid entries", Formatting.BOLD, Formatting.LIGHT_PURPLE);
        
        ctx.getSource().sendFeedback(validEntryList.build());
        return 1;
    }
    
    public static int listAllEntries(CommandContext<FabricClientCommandSource> ctx) {
        int n = 0;
        TextBuilder entryList = new TextBuilder();
        
        entryList.header("SONGPACK ENTRIES");
        for (RuntimeEntry entry : ReactiveMusicState.loadedEntries) {
            
            boolean isValid = ReactiveMusicState.validEntries.contains(entry);
            boolean isCurrent = ReactiveMusicState.currentEntry == entry;
            boolean isOverlayCurrent = OverlayTrackPlugin.usingOverlay() && OverlayTrackPlugin.currentEntry == entry;
            
            Formatting formatting;
            if (isOverlayCurrent) {
                formatting = Formatting.GOLD;  // Gold for active overlay entry
            } else if (isValid && isCurrent) {
                formatting = Formatting.GREEN; // Green for current main entry
            } else if (isValid) {
                formatting = Formatting.AQUA;  // Aqua for other valid entries
            } else {
                formatting = Formatting.GRAY;  // Gray for invalid entries
            }
            
            String eventString = entry.getEventString();
            String entryString = (eventString != null && eventString.length() >= 32) ? 
                eventString.substring(0, 32) + "..." : 
                (eventString != null ? eventString : "<null>");
            
            entryList.line(Integer.toString(n), entryString, formatting);
            n += 1;
            
        }
        entryList.line("There are a total of [ " + ReactiveMusicState.validEntries.size() + " ]  valid entries", Formatting.BOLD, Formatting.LIGHT_PURPLE)
        .raw("Use ", Formatting.WHITE).raw("/songpack entry <entryIndex>", Formatting.YELLOW, Formatting.BOLD)
        .raw(" to see the more details about that entry.");
        
        ctx.getSource().sendFeedback(entryList.build());
        return 1;
    }

    public static int currentEntryInfo(CommandContext<FabricClientCommandSource> ctx) {
        RuntimeEntry e;
        boolean isOverlayActive = OverlayTrackPlugin.usingOverlay();
        
        if (isOverlayActive && OverlayTrackPlugin.currentEntry != null) {
            e = OverlayTrackPlugin.currentEntry;
        } else {
            e = ReactiveMusicState.currentEntry;
        }
        
        TextBuilder info = new TextBuilder();
    
        info.header("CURRENT ENTRY" + (isOverlayActive ? " (OVERLAY)" : ""));
        
        if (e == null) {
            info.line("No current entry", "null", Formatting.YELLOW);
        } else {
            String eventString = e.getEventString();
            info.line("events", eventString != null ? eventString : "<null>", Formatting.WHITE)
                .line("allowFallback ", e.fallbackAllowed() ? "YES" : "NO", e.fallbackAllowed() ? Formatting.GREEN : Formatting.GRAY)
                .line("useOverlay", e.shouldOverlay() ? "YES" : "NO", e.shouldOverlay() ? Formatting.GREEN : Formatting.GRAY )
                .line("forceStopMusicOnValid", e.shouldStopMusicOnValid() ? "YES" : "NO", e.shouldStopMusicOnValid() ? Formatting.GREEN : Formatting.GRAY)
                .line("forceStopMusicOnInvalid", e.shouldStopMusicOnInvalid() ? "YES" : "NO", e.shouldStopMusicOnInvalid() ? Formatting.GREEN : Formatting.GRAY)
                .line("forceStartMusicOnValid", e.shouldStartMusicOnValid() ? "YES" : "NO", e.shouldStartMusicOnValid() ? Formatting.GREEN : Formatting.GRAY)
                .line("forceChance", Float.toString(e.getForceChance()), e.getForceChance() != 0 ? Formatting.AQUA : Formatting.GRAY)
                .line("\n"+"Now playing:", ReactiveMusicState.currentSong != null ? ReactiveMusicState.currentSong : "<none>", Formatting.ITALIC);
        }
        
        ctx.getSource().sendFeedback(info.build());
        return 1;
    }
    
    public static int indexedEntryInfo(CommandContext<FabricClientCommandSource> ctx) {
        
        int index = IntegerArgumentType.getInteger(ctx, "index");

        RuntimeEntry e = ReactiveMusicState.loadedEntries.get(index);
        TextBuilder info = new TextBuilder();
    
        String indexAsString = Integer.toString(index);
        info.header("ENTRY #" + indexAsString)
    
        .line("events", e != null && e.getEventString() != null ? e.getEventString() : "<null>", Formatting.WHITE)
        .line("allowFallback ", e.fallbackAllowed() ? "YES" : "NO", e.fallbackAllowed() ? Formatting.GREEN : Formatting.GRAY)
        .line("useOverlay", e.shouldOverlay() ? "YES" : "NO", e.shouldOverlay() ? Formatting.GREEN : Formatting.GRAY )
        .line("forceStopMusicOnValid", e.shouldStopMusicOnValid() ? "YES" : "NO", e.shouldStopMusicOnValid() ? Formatting.GREEN : Formatting.GRAY)
        .line("forceStopMusicOnInvalid", e.shouldStopMusicOnInvalid() ? "YES" : "NO", e.shouldStopMusicOnInvalid() ? Formatting.GREEN : Formatting.GRAY)
        .line("forceStartMusicOnValid", e.shouldStartMusicOnValid() ? "YES" : "NO", e.shouldStartMusicOnValid() ? Formatting.GREEN : Formatting.GRAY)
        .line("forceChance", Float.toString(e.getForceChance()), e.getForceChance() != 0 ? Formatting.AQUA : Formatting.GRAY)
        .line("songs", e != null && e.getSongs() != null ? Integer.toString(e.getSongs().size()) : "0", Formatting.LIGHT_PURPLE);
        
        ctx.getSource().sendFeedback(info.build());
        return 1;
    }
    
    public static int indexedEntrySongs(CommandContext<FabricClientCommandSource> ctx) {
        
        int index = IntegerArgumentType.getInteger(ctx, "index");
        String indexAsString = Integer.toString(index);
        int n = 0;
        
        RuntimeEntry e = ReactiveMusicState.loadedEntries.get(index);
        TextBuilder info = new TextBuilder();
    
        info.header("ENTRY #" + indexAsString + " SONGS");

        if (e != null && e.getSongs() != null) {
            for (String songId : e.getSongs()) {
                info.line(Integer.toString(n), songId != null ? songId : "<null>", Formatting.WHITE);
                n += 1;
            }
        } else {
            info.line("No songs available", "<null>", Formatting.GRAY);
        }
        
        ctx.getSource().sendFeedback(info.build());
        return 1;
    }
    
    public static int listEvents(CommandContext<FabricClientCommandSource> ctx) {
        int n = 0;
        TextBuilder eventList = new TextBuilder();
        
        eventList.header("SONGPACK EVENTS");
        
        // Check if there are any events registered
        if (ReactiveMusicState.songpackEventMap.isEmpty()) {
            eventList.line("No events registered", "<empty>", Formatting.GRAY);
        } else {
            // Group events by namespace and path
            Map<String, Map<String, List<EventRecord>>> eventsByNamespaceAndPath = new LinkedHashMap<>();
            List<EventRecord> eventOrder = new ArrayList<>(ReactiveMusicState.songpackEventMap.keySet());
            
            for (EventRecord eventRecord : eventOrder) {
                String namespace = "<unknown>";
                String path = "<unknown>";
                
                if (eventRecord != null && eventRecord.getPluginId() != null) {
                    String fullPluginId = eventRecord.getPluginId().getId();
                    if (fullPluginId.contains(":")) {
                        namespace = fullPluginId.substring(0, fullPluginId.indexOf(":"));
                        path = fullPluginId.substring(fullPluginId.indexOf(":") + 1);
                    } else {
                        namespace = fullPluginId;
                        path = "default";
                    }
                }
                
                eventsByNamespaceAndPath
                    .computeIfAbsent(namespace, k -> new LinkedHashMap<>())
                    .computeIfAbsent(path, k -> new ArrayList<>())
                    .add(eventRecord);
            }
            
            // Display events with two-level hierarchy
            for (Map.Entry<String, Map<String, List<EventRecord>>> namespaceEntry : eventsByNamespaceAndPath.entrySet()) {
                String namespace = namespaceEntry.getKey();
                Map<String, List<EventRecord>> pathsMap = namespaceEntry.getValue();
                
                // Namespace header (level 0)
                eventList.line(namespace, Formatting.YELLOW, Formatting.BOLD);
                
                for (Map.Entry<String, List<EventRecord>> pathEntry : pathsMap.entrySet()) {
                    String path = pathEntry.getKey();
                    List<EventRecord> events = pathEntry.getValue();
                    
                    // Path header (level 1)
                    eventList.line("  " + path, Formatting.AQUA, Formatting.BOLD);
                    
                    // Events under this path (level 2)
                    for (EventRecord eventRecord : events) {
                        boolean isActive = ReactiveMusicState.songpackEventMap.get(eventRecord);
                        String eventId = eventRecord != null ? eventRecord.getEventId() : "<null>";
                        
                        Formatting formatting = isActive ? Formatting.GREEN : Formatting.GRAY;
                        
                        eventList.line("    " + Integer.toString(n), eventId, formatting);
                        n += 1;
                    }
                }
            }
        }
        
        // Add summary information
        long activeCount = ReactiveMusicState.songpackEventMap.values().stream()
                .mapToLong(active -> active ? 1 : 0)
                .sum();
        
        eventList.raw("\n" + "There are [ " + activeCount + " ] active events out of [ " + 
                     ReactiveMusicState.songpackEventMap.size() + " ] total registered events", 
                     Formatting.BOLD, Formatting.LIGHT_PURPLE);
        
        ctx.getSource().sendFeedback(eventList.build());
        return 1;
    }
    
    /**
     * Check external options for the current playing entry.
     * Usage: /songpack external
     */
    public static int externalOptionsInfo(CommandContext<FabricClientCommandSource> ctx) {
        try {
            TextBuilder response = new TextBuilder();
            response.line("=== External Options Test ===", Formatting.GOLD, Formatting.BOLD);
            
            // Check current entry
            RuntimeEntry currentEntry = ReactiveMusicState.currentEntry;
            String currentSong = ReactiveMusicState.currentSong;
            
            response.line("Current Song: " + (currentSong != null ? currentSong : "None"), 
                currentSong != null ? Formatting.AQUA : Formatting.GRAY);
            
            if (currentEntry != null) {
                response.line("Current Entry Events: " + currentEntry.getEventString(), Formatting.WHITE);
                
                Object zonesOption = currentEntry.getExternalOption("zones");
                if (zonesOption != null) {
                    response.line("✓ Current entry has zones: " + zonesOption, Formatting.GREEN);
                    response.line("  Type: " + zonesOption.getClass().getSimpleName(), Formatting.GRAY);
                } else {
                    response.line("✗ Current entry has no zones option", Formatting.YELLOW);
                }
                
                // Check all external options
                response.line("All external options:", Formatting.WHITE);
                boolean hasAnyExternal = false;
                for (String key : new String[]{"zones", "test", "custom"}) {
                    Object option = currentEntry.getExternalOption(key);
                    if (option != null) {
                        response.line("  " + key + ": " + option, Formatting.GRAY);
                        hasAnyExternal = true;
                    }
                }
                if (!hasAnyExternal) {
                    response.line("  (No external options found)", Formatting.GRAY);
                }
                
            } else {
                response.line("Current Entry: None", Formatting.YELLOW);
                
                // Check valid entries as fallback
                java.util.List<RuntimeEntry> validEntries = 
                    ReactiveMusicState.validEntries;
                    
                if (!validEntries.isEmpty()) {
                    response.line("Valid Entries Found: " + validEntries.size(), Formatting.AQUA);
                    RuntimeEntry firstValid = validEntries.get(0);
                    response.line("First Valid Entry Events: " + firstValid.getEventString(), Formatting.WHITE);
                    
                    Object zonesOption = firstValid.getExternalOption("zones");
                    if (zonesOption != null) {
                        response.line("✓ First valid entry has zones: " + zonesOption, Formatting.GREEN);
                    } else {
                        response.line("✗ First valid entry has no zones option", Formatting.YELLOW);
                    }
                } else {
                    response.line("No valid entries found", Formatting.RED);
                }
            }
            
            ctx.getSource().sendFeedback(response.build());
            return 1;
            
        } catch (Exception e) {
            TextBuilder error = new TextBuilder();
            error.line("Failed to check external options: " + e.getMessage(), Formatting.RED);
            e.printStackTrace();
            ctx.getSource().sendFeedback(error.build());
            return 0;
        }
    }
}
