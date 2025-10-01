package circuitlord.reactivemusic.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import circuitlord.reactivemusic.ReactiveMusicDebug;
import circuitlord.reactivemusic.ReactiveMusicState;
import circuitlord.reactivemusic.ReactiveMusicDebug.TextBuilder;
import circuitlord.reactivemusic.api.songpack.RuntimeEntry;
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
            validEntryList.line(Integer.toString(n), entry.getEventString(), Formatting.AQUA);
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
            Formatting formatting = isValid ? isCurrent ? Formatting.GREEN : Formatting.AQUA : Formatting.GRAY;
            String entryString = entry.getEventString().length() >= 32 ? entry.getEventString().substring(0, 32) + "..." : entry.getEventString();
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
        RuntimeEntry e = ReactiveMusicState.currentEntry;
        TextBuilder info = new TextBuilder();
    
        info.header("CURRENT ENTRY")
    
        .line("events", e.getEventString(), Formatting.WHITE)
        .line("allowFallback ", e.fallbackAllowed() ? "YES" : "NO", e.fallbackAllowed() ? Formatting.GREEN : Formatting.GRAY)
        .line("useOverlay", e.shouldOverlay() ? "YES" : "NO", e.shouldOverlay() ? Formatting.GREEN : Formatting.GRAY )
        .line("forceStopMusicOnValid", e.shouldStopMusicOnValid() ? "YES" : "NO", e.shouldStopMusicOnValid() ? Formatting.GREEN : Formatting.GRAY)
        .line("forceStopMusicOnInvalid", e.shouldStopMusicOnInvalid() ? "YES" : "NO", e.shouldStopMusicOnInvalid() ? Formatting.GREEN : Formatting.GRAY)
        .line("forceStartMusicOnValid", e.shouldStartMusicOnValid() ? "YES" : "NO", e.shouldStartMusicOnValid() ? Formatting.GREEN : Formatting.GRAY)
        .line("forceChance", Float.toString(e.getForceChance()), e.getForceChance() != 0 ? Formatting.AQUA : Formatting.GRAY)
        .line("\n"+"Now playing:", ReactiveMusicState.currentSong, Formatting.ITALIC);
        
        ctx.getSource().sendFeedback(info.build());
        return 1;
    }
    
    public static int indexedEntryInfo(CommandContext<FabricClientCommandSource> ctx) {
        
        int index = IntegerArgumentType.getInteger(ctx, "index");

        RuntimeEntry e = ReactiveMusicState.loadedEntries.get(index);
        TextBuilder info = new TextBuilder();
    
        String indexAsString = Integer.toString(index);
        info.header("ENTRY #" + indexAsString)
    
        .line("events", e.getEventString(), Formatting.WHITE)
        .line("allowFallback ", e.fallbackAllowed() ? "YES" : "NO", e.fallbackAllowed() ? Formatting.GREEN : Formatting.GRAY)
        .line("useOverlay", e.shouldOverlay() ? "YES" : "NO", e.shouldOverlay() ? Formatting.GREEN : Formatting.GRAY )
        .line("forceStopMusicOnValid", e.shouldStopMusicOnValid() ? "YES" : "NO", e.shouldStopMusicOnValid() ? Formatting.GREEN : Formatting.GRAY)
        .line("forceStopMusicOnInvalid", e.shouldStopMusicOnInvalid() ? "YES" : "NO", e.shouldStopMusicOnInvalid() ? Formatting.GREEN : Formatting.GRAY)
        .line("forceStartMusicOnValid", e.shouldStartMusicOnValid() ? "YES" : "NO", e.shouldStartMusicOnValid() ? Formatting.GREEN : Formatting.GRAY)
        .line("forceChance", Float.toString(e.getForceChance()), e.getForceChance() != 0 ? Formatting.AQUA : Formatting.GRAY)
        .line("songs", Integer.toString(e.getSongs().size()), Formatting.LIGHT_PURPLE);
        
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

        for (String songId : e.getSongs()) {
            info.line(Integer.toString(n), songId, Formatting.WHITE);
            n += 1;
        }
        
        ctx.getSource().sendFeedback(info.build());
        return 1;
    }
}
