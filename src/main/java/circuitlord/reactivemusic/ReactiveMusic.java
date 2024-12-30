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

		SongPicker.initialize();


		thread = new PlayerThread();

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

			// for the cases where something is broken in the base songpack
			if (!RMSongpackLoader.availableSongpacks.getFirst().blockLoading) {
				// first is the default songpack
				setActiveSongpack(RMSongpackLoader.availableSongpacks.getFirst());
			}
		}




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
			resetPlayer();
		}


		// -------------------------


		RMRuntimeEntry newEntry = null;

		List<RMRuntimeEntry> validEntries = getValidEntries();

		// Pick the highest priority one
		if (!validEntries.isEmpty()) {
			newEntry = validEntries.getFirst();
		}

		processValidEvents(validEntries, previousValidEntries);


		if (currentDimBlacklisted)
			newEntry = null;


		if (newEntry != null) {

			List<String> selectedSongs = getSelectedSongs(newEntry, validEntries);


			// wants to switch if our current entry doesn't exist -- or is not the same as the new one
			boolean wantsToSwitch = currentEntry == null || newEntry != currentEntry;

			// if the new entry contains the same song as our current one, then do a "fake" swap to swap over to the new entry
			if (wantsToSwitch && currentSong != null && newEntry.songs.contains(currentSong)) {

				LOGGER.info("doing fake swap to new event: " + newEntry.eventString);

				// do a fake swap
				currentEntry = newEntry;
				wantsToSwitch = false;

				// if this happens, also clear the queued state since we essentially did a switch
				queuedToStopMusic = false;
				queuedToPlayMusic = false;
			}

			// make sure we're fully faded in if we faded out for any reason but this event is valid
			if (thread.isPlaying() && !wantsToSwitch && fadeOutTicks > 0) {
				fadeOutTicks--;

				// Copy the behavior from below where it fades out
				thread.setGainPercentage(1f - (fadeOutTicks / (float)FADE_DURATION));
			}



			// ---- FADE OUT ----

			if (wantsToSwitch && thread.isPlaying()) {

				waitForStopTicks++;

				boolean shouldFadeOutMusic = false;

				// handle fade-out if something's playing when a new event becomes valid
				if (waitForStopTicks > getMusicStopSpeed(currentSongpack)) {
					shouldFadeOutMusic = true;
				}

				// if we're queued to force stop the music, do so here
				if (queuedToStopMusic) {
					shouldFadeOutMusic = true;
				}

				if (shouldFadeOutMusic) {
					tickFadeOut();
				}
			}
			else {
				waitForStopTicks = 0;
			}

			//  ---- SWITCH SONG ----

			if (wantsToSwitch && thread.notQueuedOrPlaying()) {

				waitForNewSongTicks++;

				boolean shouldStartNewSong = false;

				if (waitForNewSongTicks > getMusicDelay(currentSongpack)) {
					shouldStartNewSong = true;
				}

				// if we're queued to start a new song and we're not playing anything, do it
				if (queuedToPlayMusic) {
					shouldStartNewSong = true;
				}

				if (shouldStartNewSong) {

					String picked = SongPicker.pickRandomSong(selectedSongs);

					changeCurrentSong(picked, newEntry);
				}

			}
			else {
				waitForNewSongTicks = 0;
			}




		}

		// no entries are valid, we shouldn't be playing any music!
		// this can happen if no entry is valid or the dimension is blacklisted
		else {

			tickFadeOut();

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

	private static void processValidEvents(List<RMRuntimeEntry> validEntries, List<RMRuntimeEntry> previousValidEntries) {


		for (var entry : previousValidEntries) {

			// if this event was valid before and is invalid now
			if (entry.forceStopMusicOnInvalid && !validEntries.contains(entry)) {
				LOGGER.info("trying forceStopMusicOnInvalid: " + entry.eventString);

				if (entry.cachedRandomChance <= entry.forceChance) {

					LOGGER.info("doing forceStopMusicOnInvalid: " + entry.eventString);
					queuedToStopMusic = true;
				}

				break;
			}
		}

		for (var entry : validEntries) {

			if (!previousValidEntries.contains(entry)) {

				// use the same random chance for all so they always happen together
				entry.cachedRandomChance = rand.nextFloat();
				boolean randSuccess = entry.cachedRandomChance <= entry.forceChance;

				// if this event wasn't valid before and is now
				if (entry.forceStopMusicOnValid) {
					LOGGER.info("trying forceStopMusicOnValid: " + entry.eventString);

					if (randSuccess) {
						LOGGER.info("doing forceStopMusicOnValid: " + entry.eventString);
						queuedToStopMusic = true;
					}
				}

				if (entry.forceStartMusicOnValid) {
					LOGGER.info("trying forceStartMusicOnValid: " + entry.eventString);

					if (randSuccess) {
						LOGGER.info("doing forceStartMusicOnValid: " + entry.eventString);
						queuedToPlayMusic = true;
					}
				}

			}


		}




	}


	public static void tickFadeOut() {

		if (!thread.isPlaying())
			return;

		if (fadeOutTicks < FADE_DURATION) {
			fadeOutTicks++;
			thread.setGainPercentage(1f - (fadeOutTicks / (float)FADE_DURATION));
		}
		else {
			resetPlayer();
		}
	}


	public static void changeCurrentSong(String song, RMRuntimeEntry newEntry) {

		resetPlayer();

		currentSong = song;
		currentEntry = newEntry;

		LOGGER.info("Changing entry: " + newEntry.eventString + " Song name: " + song);

		// go full quiet while switching songs, we'll go back to 1.0f after we load the new song
		thread.setGainPercentage(0.0f);

		thread.play(song);

		queuedToPlayMusic = false;

	}



	public static void setActiveSongpack(SongpackZip songpackZip) {

		// TODO: more than one songpack?
		if (currentSongpack != null) {
			deactivateSongpack(currentSongpack);
		}

		resetPlayer();

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

	static void resetPlayer() {

		// if queued or playing
		if (!thread.notQueuedOrPlaying()) {
			thread.resetPlayer();
		}

		fadeOutTicks = 0;
		queuedToStopMusic = false;
		currentEntry = null;
		currentSong = null;
	}






}