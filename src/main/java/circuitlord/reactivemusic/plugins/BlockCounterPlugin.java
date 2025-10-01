package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.Block;

import java.util.*;

public final class BlockCounterPlugin extends ReactiveMusicPlugin {
    public BlockCounterPlugin() {
        super("reactivemusic", "block_counter");
    }

    // --- config (mirrors your current setup) ---
    private static final int RADIUS = 25;
    private static final Set<String> BLOCK_COUNTER_BLACKLIST = Set.of("ore", "debris");

    // --- plugin-owned state (removed from SongPicker) ---
    private static final Map<String, Integer> blockCounterMap = new HashMap<>();
    private static BlockPos cachedBlockCounterOrigin;
    private static int currentBlockCounterX = 99999; // start out-of-range to force snap to origin on first wrap
    // Note: your Y sweep is commented-out in the original; we keep the same single-axis sweep.

    @Override public void init() { /* no-op */ }
    @Override public int tickSchedule() { return 1; }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<EventRecord, Boolean> out) {
        if (!(player instanceof ClientPlayerEntity) || world == null) return;

        // lazily initialize origin
        if (cachedBlockCounterOrigin == null) {
            cachedBlockCounterOrigin = player.getBlockPos();
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
            if (SongPicker.queuedToPrintBlockCounter) {
                player.sendMessage(Text.of("[ReactiveMusic]: Logging Block Counter map!"));
                blockCounterMap.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                        .forEach(e -> player.sendMessage(Text.of(e.getKey() + ": " + e.getValue()), false));
                SongPicker.queuedToPrintBlockCounter = false;
            }

            // publish to the cache that isEntryValid() reads
            SongPicker.cachedBlockChecker.clear();
            SongPicker.cachedBlockChecker.putAll(blockCounterMap);

            // reset for next sweep
            blockCounterMap.clear();
            cachedBlockCounterOrigin = player.getBlockPos();
        }

        // scan a vertical column (Y) for all Z at the current X slice
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        for (int y = -RADIUS; y <= RADIUS; y++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                mutablePos.set(
                        cachedBlockCounterOrigin.getX() + currentBlockCounterX,
                        cachedBlockCounterOrigin.getY() + y,
                        cachedBlockCounterOrigin.getZ() + z
                );

                Block block = world.getBlockState(mutablePos).getBlock();
                String key = Registries.BLOCK.getId(block).toString();

                boolean blacklisted = false;
                for (String s : BLOCK_COUNTER_BLACKLIST) {
                    if (key.contains(s)) { blacklisted = true; break; }
                }
                if (blacklisted) continue;

                blockCounterMap.merge(key, 1, Integer::sum);
            }
        }

        // timing (kept but not logged)
        long elapsed = System.nanoTime() - startNano;
        @SuppressWarnings("unused")
        double elapsedMs = elapsed / 1_000_000.0;
        // (optional) log if you want: ReactiveMusicDebug.LOGGER.info("BlockCounterPlugin tick: " + elapsedMs + "ms");
    }
}
