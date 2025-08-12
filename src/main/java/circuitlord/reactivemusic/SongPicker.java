package circuitlord.reactivemusic;

import circuitlord.reactivemusic.ReactiveMusicDebug.ChangeLogger;
import circuitlord.reactivemusic.ReactiveMusicDebug.Wrapper;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.RuntimeEntry;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.network.ClientPlayerEntity;
//import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.lang.reflect.Field;
import java.util.*;

import org.jetbrains.annotations.NotNull;

public final class SongPicker {
    private static final ChangeLogger CHANGE_LOGGER = ReactiveMusic.debugTools.new ChangeLogger();

    static int pluginTickCounter = 0;
    // TODO: Put this stuff in the plugins, silly 😝
    //-------------------------------------------------------------------------------------

    public static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();

    public static boolean queuedToPrintBlockCounter = false;
    public static BlockPos cachedBlockCounterOrigin;


    public static Map<String, Integer> blockCounterMap = new HashMap<>();
    public static Map<String, Integer> cachedBlockChecker = new HashMap<>();

    public static String currentBiomeName = "";
    public static String currentDimName = "";

    public static final Field[] BIOME_TAG_FIELDS = ConventionalBiomeTags.class.getDeclaredFields();
    public static final List<TagKey<Biome>> BIOME_TAGS = new ArrayList<>();

    static {

        for (Field field : BIOME_TAG_FIELDS) {
            TagKey<Biome> biomeTag = getBiomeTagFromField(field);

            BIOME_TAGS.add(biomeTag);
            biomeTagEventMap.put(biomeTag, false);
        }
    }

    public static TagKey<Biome> getBiomeTagFromField(Field field) {
        if (field.getType() == TagKey.class) {
            try {
                @SuppressWarnings("unchecked")
                TagKey<Biome> tag = (TagKey<Biome>) field.get(null);
                return tag;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void tickEventMap() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null)
            return;

        ClientPlayerEntity player = mc.player;
        World world = mc.world;

        
        pluginTickCounter++;
        
        for (ReactiveMusicPlugin plugin : ReactiveMusic.PLUGINS) {
            
            if (ReactiveMusicState.logicFreeze.computeIfAbsent(plugin.pluginId, k -> false)) {
                ReactiveMusicState.LOGGER.info("Skipping execution for " + plugin.pluginId.getId());
                continue;
            }

            plugin.newTick();

            if (player == null || world == null) {
                continue;
            }
            
            
            // throttled tick
            int interval = plugin.tickSchedule();
            if (interval <= 1 || (pluginTickCounter % interval) == 0L) {
                plugin.gameTick(player, world, ReactiveMusicState.songpackEventMap);              
            }
        }

        if (player == null || world == null) initialize();

        ReactiveMusicState.songpackEventMap.put(SongpackEvent.GENERIC, true);
        ReactiveMusicState.songpackEventMap.put(SongpackEvent.MAIN_MENU, (player == null || world == null));
        ReactiveMusicState.songpackEventMap.put(SongpackEvent.CREDITS, (mc.currentScreen instanceof CreditsScreen));

    }

    public static void initialize() {
        // build string -> type map from the internal registry
        ReactiveMusicState.songpackEventMap.clear();
        for (EventRecord eventRecord : SongpackEvent.values()) {
            ReactiveMusicState.songpackEventMap.put(eventRecord, false);
        }
    }

    //----------------------------------------------------------------------------------------
    public static boolean isEntryValid(RuntimeEntry entry) {

        for (var condition : entry.getConditions()) {

            // each condition functions as an OR, if at least one of them is true then the condition is true


            boolean songpackEventsValid = false;
            for (var eventRecord : condition.songpackEvents) {
                if (eventRecord == null) break;
                CHANGE_LOGGER.writeInfo(ReactiveMusicState.songpackEventMap.containsKey(eventRecord) ? "The event record key was found in the event map!" : "Oh no!" );
                if (ReactiveMusicState.songpackEventMap.containsKey(eventRecord) && ReactiveMusicState.songpackEventMap.get(SongpackEvent.get(eventRecord.getEventId()))) {
                    songpackEventsValid = true;
                    break;
                }
            }

            boolean blocksValid = false;
            for (var blockCond : condition.blocks) {
                for (var kvp : cachedBlockChecker.entrySet()) {
                    if (kvp.getKey().contains(blockCond.block) && kvp.getValue() >= blockCond.requiredCount) {
                        blocksValid = true;
                        break;
                    }
                }
            }

            boolean biomeTypesValid = false;
            for (var biome : condition.biomeTypes) {
                if (currentBiomeName.contains(biome)) {
                    biomeTypesValid = true;
                    break;
                }
            }

            boolean biomeTagsValid = false;
            for (var biomeTag : condition.biomeTags) {
                if (biomeTagEventMap.containsKey(biomeTag) && biomeTagEventMap.get(biomeTag)) {
                    biomeTagsValid = true;
                    break;
                }
            }

            boolean dimsValid = false;
            for (var dim : condition.dimTypes) {
                if (currentDimName.contains(dim)) {
                    dimsValid = true;
                    break;
                }
            }


            if (!songpackEventsValid && !biomeTypesValid && !biomeTagsValid && !dimsValid && !blocksValid) {
                // none of the OR conditions were valid on this condition, return false
                return false;
            }

        }

        // we passed without failing so it must be true
        return true;
        
    }

    public static @NotNull List<String> getSelectedSongs(RuntimeEntry newEntry, List<RuntimeEntry> validEntries) {
		// if we have non-recent songs then just return those
		if (ReactiveMusicUtils.hasSongNotPlayedRecently(newEntry.getSongs())) {
			return newEntry.getSongs();
		}
		// Fallback behaviour
		if (newEntry.fallbackAllowed()) {
			for (int i = 1; i < ReactiveMusicState.validEntries.size(); i++) {
				if (ReactiveMusicState.validEntries.get(i) == null)
					continue;
				// check if we have songs not played recently and early out
				if (ReactiveMusicUtils.hasSongNotPlayedRecently(ReactiveMusicState.validEntries.get(i).getSongs())) {
					return ReactiveMusicState.validEntries.get(i).getSongs();
				}
			}
		}
		// we've played everything recently, just give up and return this event's songs
		return newEntry.getSongs();
	}
}
