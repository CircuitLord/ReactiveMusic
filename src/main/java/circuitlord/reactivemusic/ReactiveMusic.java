package circuitlord.reactivemusic;

import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.config.MusicDelayLength;
import circuitlord.reactivemusic.config.MusicSwitchSpeed;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReactiveMusic implements ModInitializer {

	public static final String MOD_ID = "reactive_music";
	public static final String MOD_VERSION = "1.0";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final int WAIT_FOR_SWITCH_DURATION = 100;
	public static final int FADE_DURATION = 150;
	public static final int SILENCE_DURATION = 100;

	public static int additionalSilence = 0;

	public static PlayerThread thread;


	public static SongpackZip currentSongpack = null;

	static boolean queuedToStopMusic = false;
	static boolean queuedToPlayMusic = false;

	//static List<RMRuntimeEntry> currentEntries = new ArrayList<>();

	static String currentSong = null;
	static RMRuntimeEntry currentEntry = null;

	//static List<SongpackEntry> currentGenericEntries = new ArrayList<>();
	
	//static String nextSong;
	static int waitForStopTicks = 0;
	static int waitForNewSongTicks = 99999;
	static int fadeOutTicks = 0;
	//static int fadeInTicks = 0;
	static int silenceTicks = 0;

	static int slowTickUpdateCounter = 0;

	static boolean currentDimBlacklisted = false;

	boolean doSilenceForNextQueuedSong = true;

	static List<RMRuntimeEntry> previousValidEntries = new ArrayList<>();


	static Random rand = new Random();


	static ModConfig config;


	// Add this static list to the class
	//private static List<SongpackEntry> validEntries = new ArrayList<>();


	private static List<RMRuntimeEntry> loadedEntries = new ArrayList<>();



	@Override
	public void onInitialize() {

		LOGGER.info("--------------------------------------------");
		LOGGER.info("|     Reactive Music initialization...     |");
		LOGGER.info("|                version " + MOD_VERSION +"              |");
		LOGGER.info("--------------------------------------------");



		ModConfig.GSON.load();
		config = ModConfig.getConfig();


		//Path test = Path.of("K:\\Projects\\MC\\BetterMusic\\run\\resourcepacks\\AwesomeMixVol2");
		//SongpackZip zip = RMSongpackLoader.loadSongpack(test, false);


		//SongLoader.fetchAvailableSongpacks();

		RMSongpackLoader.fetchAvailableSongpacks();

		boolean loadedUserSongpack = false;

		// try to load a saved songpack
		if (!config.loadedUserSongpack.isEmpty()) {

			for (var songpack : RMSongpackLoader.availableSongpacks) {
				if (!songpack.config.name.equals(config.loadedUserSongpack)) continue;

				// something is broken in this songpack, don't load it
				if (songpack.blockLoading)
					continue;

				setActiveSongpack(songpack);
				loadedUserSongpack = true;

				break;
			}
		}

		// load the default one
		if (!loadedUserSongpack) {
			// first is the default songpack
			setActiveSongpack(RMSongpackLoader.availableSongpacks.getFirst());
		}


		SongPicker.initialize();


		thread = new PlayerThread();



        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("reactivemusic")
				.executes(context -> {
							MinecraftClient mc = context.getSource().getClient();
							Screen screen = ModConfig.createScreen(mc.currentScreen);
							mc.send(() -> mc.setScreen(screen));
							return 1;
						}
				)

				.then(ClientCommandManager.literal("blacklistDimension")
						.executes(context -> {

							String key = context.getSource().getClient().world.getRegistryKey().getValue().toString();

							if (config.blacklistedDimensions.contains(key)) {
								context.getSource().sendFeedback(Text.literal("ReactiveMusic: " + key + " was already in blacklist."));
								return 1;
							}

							context.getSource().sendFeedback(Text.literal("ReactiveMusic: Added " + key + " to blacklist."));

							config.blacklistedDimensions.add(key);
							ModConfig.saveConfig();

							return 1;
						})
				)

				.then(ClientCommandManager.literal("unblacklistDimension")
						.executes(context -> {
							String key = context.getSource().getClient().world.getRegistryKey().getValue().toString();

							if (!config.blacklistedDimensions.contains(key)) {
								context.getSource().sendFeedback(Text.literal("ReactiveMusic: " + key + " was not in blacklist."));
								return 1;
							}

							context.getSource().sendFeedback(Text.literal("ReactiveMusic: Removed " + key + " from blacklist."));

							config.blacklistedDimensions.remove(key);
							ModConfig.saveConfig();

							return 1;
						})
				)

			)
		);
	}

	public static void newTick() {

		if (thread == null) return;
		if (currentSongpack == null) return;
		if (loadedEntries.isEmpty()) return;

		MinecraftClient mc = MinecraftClient.getInstance();

		slowTickUpdateCounter++;
		if (slowTickUpdateCounter > 20) {

			currentDimBlacklisted = false;

			// see if the dimension we're in is blacklisted -- update at same time as event map to keep them in sync
			if (mc != null && mc.world != null) {
				String curDim = mc.world.getRegistryKey().getValue().toString();

				for (String dim : config.blacklistedDimensions) {
					if (dim.equals(curDim)) {
						currentDimBlacklisted = true;
						break;
					}
				}
			}

			SongPicker.tickEventMap();

			slowTickUpdateCounter = 0;
		}


		// -------------------------

		// clear playing state if not playing
		if (thread.notQueuedOrPlaying()) {
			fadeOutTicks = 0;
			queuedToStopMusic = false;
			currentEntry = null;
			currentSong = null;
		}


		// -------------------------


		RMRuntimeEntry newEntry = null;

		List<RMRuntimeEntry> validEntries = getValidEntries();

		// Pick the highest priority one
		if (!validEntries.isEmpty()) {
			newEntry = validEntries.getFirst();
		}

		updateQueuedToStopMusic(validEntries, previousValidEntries);


		if (currentDimBlacklisted)
			newEntry = null;


		if (newEntry != null) {

			List<String> selectedSongs = getSelectedSongs(newEntry, validEntries);


			// wants to switch if our current entry doesn't exist -- or is not the same as the new one
			boolean wantsToSwitch = currentEntry == null || newEntry != currentEntry;

			// make sure we're fully faded in if we faded out for any reason but this event is valid
			if (thread.isPlaying() && !wantsToSwitch && fadeOutTicks > 0) {
				fadeOutTicks--;

				// Copy the behavior from below where it fades out
				thread.setGainPercentage(1f - (fadeOutTicks / (float)FADE_DURATION));
			}


			if (wantsToSwitch) waitForStopTicks++;
			else waitForStopTicks = 0;

			if (thread.notQueuedOrPlaying()) waitForNewSongTicks++;
			else waitForNewSongTicks = 0;


			// ---- FADE OUT ----

			boolean shouldFadeOutMusic = false;

			// handle fade-out if something's playing when a new event becomes valid
			if (wantsToSwitch && waitForStopTicks > getMusicStopSpeed(currentSongpack)) {
				shouldFadeOutMusic = true;
			}

			// if we're queued to force stop the music, do so here
			if (queuedToStopMusic) {
				shouldFadeOutMusic = true;
			}


			if (thread.isPlaying() && shouldFadeOutMusic) {
				tickFadeOut();
			}



			//  ---- SWITCH SONG ----

			boolean shouldStartNewSong = false;

			if (wantsToSwitch && waitForNewSongTicks > getMusicDelay(currentSongpack)) {
				shouldStartNewSong = true;
			}

			if (queuedToPlayMusic) {
				shouldStartNewSong = true;
			}

			if (shouldStartNewSong) {

				// TODO: prioritize current event

				String picked = SongPicker.pickRandomSong(selectedSongs);

				changeCurrentSong(picked, newEntry);
			}



		}

		// no entries are valid, we shouldn't be playing any music!
		// this can happen if no entry is valid or the dimension is blacklisted
		else {
			if (thread.isPlaying()) {
				tickFadeOut();
			}
			else {
				currentEntry = null;
				currentSong = null;
			}
		}



		thread.processRealGain();


		previousValidEntries = validEntries;

	}

	private static @NotNull List<String> getSelectedSongs(RMRuntimeEntry newEntry, List<RMRuntimeEntry> validEntries) {

		// if we have non-recent songs then just return those
		if (SongPicker.hasSongNotPlayedRecently(newEntry.songs)) {
			return newEntry.songs;
		}

		// Fallback behaviour
		if (newEntry.allowFallback) {
			for (int i = 1; i < validEntries.size(); i++) {
				if (validEntries.get(i) == null)
					continue;

				// check if we have songs not played recently and early out
				if (SongPicker.hasSongNotPlayedRecently(validEntries.get(i).songs)) {
					return validEntries.get(i).songs;
				}
			}
		}


		// we've played everything recently, just give up and return this event's songs
		return newEntry.songs;
	}


	public static List<RMRuntimeEntry> getValidEntries() {
		List<RMRuntimeEntry> validEntries = new ArrayList<>();

        for (RMRuntimeEntry loadedEntry : loadedEntries) {

            boolean isValid = SongPicker.isEntryValid(loadedEntry);

            if (isValid) {
                validEntries.add(loadedEntry);
            }
        }

		return validEntries;
	}

	private static void updateQueuedToStopMusic(List<RMRuntimeEntry> validEntries, List<RMRuntimeEntry> previousValidEntries) {
		for (var entry : previousValidEntries) {
			// if this event was valid before and is invalid now
			if (entry.stopMusicOnEventChanged && !validEntries.contains(entry)) {
				LOGGER.info("stopMusicOnEventChanged, entry invalidated");
				queuedToStopMusic = true;
				break;
			}
		}

		for (var entry : validEntries) {
			// if this event wasn't valid before and is now
			if (entry.stopMusicOnEventChanged && !previousValidEntries.contains(entry)) {
				LOGGER.info("stopMusicOnEventChanged, entry valid!");
				queuedToStopMusic = true;
			}
		}
	}


	/*public static void tick() {

		if (thread == null) return;

		if (currentSongpack == null) return;

		MinecraftClient mc = MinecraftClient.getInstance();

		if (!thread.isPlaying()) silenceTicks++;
		else silenceTicks = 0;

		slowTickUpdateCounter++;

		if (slowTickUpdateCounter > 20) {

			currentDimBlacklisted = false;

			// see if the dimension we're in is blacklisted -- update at same time as event map to keep them in sync
			if (mc != null && mc.world != null) {
				String curDim = mc.world.getRegistryKey().getValue().toString();

				for (String dim : config.blacklistedDimensions) {
					if (dim.equals(curDim)) {
						currentDimBlacklisted = true;
						break;
					}
				}
			}

			SongPicker.tickEventMap();

			slowTickUpdateCounter = 0;
		}

		// --- find new potential entry ---

		validEntries = SongPicker.getAllValidEntries();

		SongpackEntry newEntry = null;
		String[] selectedSongs = {};

		if (!currentDimBlacklisted) {
			currentGenericEntries.clear();

			// Try to find the highest entry with a song we haven't played recently
			for (var entry : validEntries) {
				// if this entry can stack on entries below it, keep it in a separate list to add to the song picker pool
				// only consider when the size of valid entries is more than 1
				if (entry.stackable && validEntries.size() > 1) {
					selectedSongs = ArrayUtils.addAll(selectedSongs, entry.songs);
					continue;
				}

				// if this is the same entry and we're still playing the song we picked, keep it
				if (currentEntry == entry && thread.isPlaying()) {
					newEntry = currentEntry;
					break;
				}

				// if this entry has songs we haven't played recently -- or it doesn't allow fallback
				// then pick it as the "new" potential entry
				if (SongPicker.hasSongNotPlayedRecently(entry.songs) || !entry.allowFallback) {
					newEntry = entry;
					break;
				}
			}

			// if we didn't find any entries, just use the highest priority one
			if (newEntry == null && !validEntries.isEmpty()) {
				for (var i = 0; i < validEntries.size(); i++) {
					newEntry = validEntries.get(i);
					// choose an entry that is not stackable unless it's the only entry in the list
					if (!newEntry.stackable || validEntries.size() == 1)
						break;
				}
			}

			if (newEntry != null) {
				selectedSongs = ArrayUtils.addAll(selectedSongs, newEntry.songs);
			}
		}


		// TODO: NOTE
		// when we just skip switching songs if the new event has the music, we should still update currentEntry to match that or it has some issues

		// --- main loop, check new entry and see if we should play it ---

		// If a new valid entry exists, check it
		if (newEntry != null && selectedSongs.length > 0) {


			if (currentEntry == null || newEntry.id != currentEntry.id) waitForStopTicks++;
			else waitForStopTicks = 0;


			// if we started fading out, and the event became valid again, we need to fade back in
			if (thread.isPlaying() && currentEntry.id == newEntry.id && fadeOutTicks > 0) {
				// Copy the behavior from below where it fades out
				thread.setGainPercentage(1f - (fadeOutTicks / (float)FADE_DURATION));

				fadeOutTicks--;
			}


			boolean playNewSong = false;
			boolean doSilenceAfter = true;


			// No song is playing and we've waiting through the silence, just start one randomly
			// Skip wait in debug mode
			if (thread.notQueuedOrPlaying()
					&& ((silenceTicks > additionalSilence) || config.debugModeEnabled)) {
				playNewSong = true;
			}


			// TODO: bug with the two below modes:
			// TODO: when underground, we have alwaysplay so it means this immediately triggers again when it stops

			// the newEntry is defined to always play, just start it immediately
			else if (thread.notQueuedOrPlaying() && newEntry.alwaysPlay) {
				playNewSong = true;
			}

			// if our previous song was defined to stop for this song, then start playing the new event immediately
			else if (thread.notQueuedOrPlaying() && (currentEntry != null && currentEntry.alwaysStop)) {
				playNewSong = true;
			}

			// --- Fading current song to switch to new event ---

			// If we changed what event is active, we need to fade out
			// Wait for a bit to make sure we stay on a different event
			// Also only fade out if it's specifically defined we should stop/start in the songpack
			else if (thread.isPlaying() && currentEntry != null && newEntry.id != currentEntry.id
					&& waitForStopTicks > WAIT_FOR_SWITCH_DURATION
					&& (currentEntry.alwaysStop || newEntry.alwaysPlay || config.debugModeEnabled)

					// make sure the new entry doesn't have the song we're playing already -- because then we wouldn't want to switch
					&& !Arrays.asList(selectedSongs).contains(currentSong)
			) {

				tickFadeOut();
			}

			if (playNewSong) {
				String picked = SongPicker.pickRandomSong(selectedSongs);
				changeCurrentSong(picked, newEntry);

				int minTickSilence = 0;
				int maxTickSilence = 0;

				switch (ModConfig.getConfig().musicDelayLength) {
					case SHORT -> {
						minTickSilence = 250;
						maxTickSilence = 1000;
					}
					case NORMAL -> {
						minTickSilence = 1000;
						maxTickSilence = 4000;
					}
					case LONG -> {
						minTickSilence = 2000;
						maxTickSilence = 7000;
					}
				}

				additionalSilence = rand.nextInt(minTickSilence, maxTickSilence);
			}

		}

		// no entries are valid, we shouldn't be playing any music!
		// this can happen if no entry is valid or the dimension is blacklisted
		else {
			if (thread.isPlaying()) {
				tickFadeOut();
			}
			else {
				currentEntry = null;
				currentSong = null;
			}
		}



		thread.processRealGain();

	}*/


	public static void tickFadeOut() {

		if (!thread.isPlaying())
			return;

		if (fadeOutTicks < FADE_DURATION) {
			fadeOutTicks++;
			thread.setGainPercentage(1f - (fadeOutTicks / (float)FADE_DURATION));
		}
		else {
			thread.resetPlayer();
		}
	}


	public static void changeCurrentSong(String song, RMRuntimeEntry newEntry) {
		currentSong = song;
		currentEntry = newEntry;

		LOGGER.info("Changing entry: " + newEntry.eventString + " Song name: " + song);

		// go full quiet while switching songs, we'll go back to 1.0f after we load the new song
		thread.setGainPercentage(0.0f);

		thread.play(song);

		queuedToPlayMusic = false;

	}


	public static void refreshSongpack() {

		thread.resetPlayer();
		additionalSilence = 0;

	}


	public static void setActiveSongpack(SongpackZip songpackZip) {

		// TODO: more than one songpack?
		if (currentSongpack != null) {
			deactivateSongpack(currentSongpack);
		}

		currentSongpack = songpackZip;

		loadedEntries = songpackZip.runtimeEntries;

		// always start new music immediately
		queuedToPlayMusic = true;

	}

	public static void deactivateSongpack(SongpackZip songpackZip) {

		// remove all entries that match that name
		for (int i = loadedEntries.size() - 1; i >= 0; i--) {
			if (loadedEntries.get(i).songpack == songpackZip.config.name) {
				loadedEntries.remove(i);
			}
		}

	}

	public static int getMusicStopSpeed(SongpackZip songpack) {

		MusicSwitchSpeed speed = config.musicSwitchSpeed2;

		if (config.musicSwitchSpeed2 == MusicSwitchSpeed.SONGPACK_DEFAULT) {
			speed = songpack.config.musicSwitchSpeed;
		}

		if (config.debugModeEnabled) {
			speed = MusicSwitchSpeed.INSTANT;
		}

		switch (speed) {

			case INSTANT:
				return 100;

			case SHORT:
				return 350;

			case NORMAL:
				return 1000;

			case LONG:
				return 2500;

		}

		return 100;

	}

	public static int getMusicDelay(SongpackZip songpack) {

		MusicDelayLength delay = config.musicDelayLength2;

		if (config.musicDelayLength2 == MusicDelayLength.SONGPACK_DEFAULT) {
			delay = songpack.config.musicDelayLength;
		}

		if (config.debugModeEnabled) {
			delay = MusicDelayLength.NONE;
		}

		switch (delay) {
			case NONE:
				return 0;
			case SHORT:
				return 350;
			case NORMAL:
				return 1000;
			case LONG:
				return 2500;
		}

		return 100;

	}






}