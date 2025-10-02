package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.ReactiveMusicDebug.TextBuilder;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.commands.AwaitValueCommandUtility;
import circuitlord.reactivemusic.util.ReactiveBlockTools;
import net.minecraft.util.Formatting;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

import java.util.*;

public final class BlockCounterPlugin extends ReactiveMusicPlugin {
    public BlockCounterPlugin() {
        super("reactivemusic", "block_counter");
    }

    // --- config (mirrors your current setup) ---
    private static final int RADIUS = 25;
    private static final Set<String> BLOCK_COUNTER_BLACKLIST = Set.of("ore", "debris");

    // --- plugin-owned state (removed from SongPicker) ---
    private static boolean queuedToPrint = false;
    private static Map<String, Integer> blockCounterMap = new HashMap<>();
    private static MinecraftVector3 cachedBlockCounterOrigin;
    private static int currentBlockCounterX = 99999; // start out-of-range to force snap to origin on first wrap
    // Note: your Y sweep is commented-out in the original; we keep the same single-axis sweep.

    @Override public void init() { /* no-op */ }
    @Override public int tickSchedule() { return 1; }

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> out) {
        if (player == null || world == null) return;

        // lazily initialize origin
        if (cachedBlockCounterOrigin == null) {
            cachedBlockCounterOrigin = player.location().pos();
        }

        long startNano = System.nanoTime();

        // advance X
        currentBlockCounterX++;
        if (currentBlockCounterX > RADIUS) {
            currentBlockCounterX = -RADIUS;
        }

        

        // finished iterating, copy & reset
        if (currentBlockCounterX == -RADIUS) {
            // Print request
            if (queuedToPrint) {
                TextBuilder msg = new TextBuilder();
                blockCounterMap.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                        .forEach(e -> {
                            msg.raw(e.getKey().split(":", 2)[0] + ":", Formatting.GRAY);
                            msg.raw(e.getKey().split(":", 2)[1], Formatting.YELLOW, Formatting.BOLD);
                            msg.newline();
                            msg.raw(indentByCount(e.getValue()), Formatting.BOLD, Formatting.DARK_GREEN);
                            msg.raw(e.getValue().toString(), Formatting.BOLD, Formatting.GREEN);
                            msg.newline();
                            msg.newline();
                        });
                
                AwaitValueCommandUtility.complete("BlockCounterPlugin", msg.build());
                queuedToPrint = false;
            }

            // publish to the cache that isEntryValid() reads
            SongPicker.cachedBlockChecker.clear();
            SongPicker.cachedBlockChecker.putAll(blockCounterMap);

            // reset for next sweep
            blockCounterMap.clear();
            cachedBlockCounterOrigin = player.location().pos();
        }

        // scan a vertical column (Y) for all Z at the current X slice
        blockCounterMap = ReactiveBlockTools.countAllInBox(world, cachedBlockCounterOrigin, RADIUS, BLOCK_COUNTER_BLACKLIST);

        // timing (kept but not logged)
        long elapsed = System.nanoTime() - startNano;
        @SuppressWarnings("unused")
        double elapsedMs = elapsed / 1_000_000.0;
        // (optional) log if you want: ReactiveMusicDebug.LOGGER.info("BlockCounterPlugin tick: " + elapsedMs + "ms");
    }

    public static void queueToPrint() { queuedToPrint = true; }

    // helper for the log
    static String indentByCount(int c) {
        String s = "";
        int count = 0;
        while (count < c) {
            count = count + 10;
            s = s + "-";
            if (count == 400) {
                break;
            }
        }
        return s + "> ";
    }


}
