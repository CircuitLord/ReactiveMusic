package circuitlord.reactivemusic;

import circuitlord.reactivemusic.config.ModConfig;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class ReactiveMusic implements ModInitializer {

	public static final String MOD_ID = "reactive_music";
	public static final String MOD_VERSION = "0.3";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final int WAIT_FOR_SWITCH_DURATION = 100;
	public static final int FADE_DURATION = 150;
	public static final int SILENCE_DURATION = 100;

	public static int additionalSilence = 0;

	public static PlayerThread thread;

	static String currentSong;
	static SongpackEntry currentEntry = null;
	
	//static String nextSong;
	static int waitForSwitchTicks = 0;
	static int fadeOutTicks = 0;
	static int fadeInTicks = 0;
	static int silenceTicks = 0;

	static int slowTickUpdateCounter = 0;

	boolean doSilenceForNextQueuedSong = true;


	static Random rand = new Random();

	//public static final circuitlord.reactivemusic.ReactiveMusicConfig CONFIG = circuitlord.reactivemusic.ReactiveMusicConfig.createAndLoad();


	static ModConfig config;


	@Override
	public void onInitialize() {

		LOGGER.info("--------------------------------------------");
		LOGGER.info("|     Reactive Music initialization...     |");
		LOGGER.info("|                version " + MOD_VERSION +"               |");
		LOGGER.info("--------------------------------------------");



		ModConfig.GSON.load();
		config = ModConfig.getConfig();

		SongLoader.fetchAvailableSongpacks();

		boolean loadedUserSongpack = false;

		// try to load a saved songpack
		if (!config.loadedUserSongpack.isEmpty()) {

			for (var songpack : SongLoader.availableSongpacks) {
				if (!songpack.config.name.equals(config.loadedUserSongpack)) continue;

				SongLoader.setActiveSongpack(songpack, false);
				loadedUserSongpack = true;

				break;
			}
		}

		// load the default one
		if (!loadedUserSongpack) {
			SongLoader.setActiveSongpack(null, true);
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
				)));



	}

	public static void tick() {

		if (thread == null) return;

		if (SongLoader.activeSongpack == null) return;

		if (!thread.isPlaying()) silenceTicks++;
		else silenceTicks = 0;

		slowTickUpdateCounter++;

		if (slowTickUpdateCounter > 20) {
			SongPicker.tickEventMap();

			slowTickUpdateCounter = 0;
		}


		SongpackEntry newEntry = SongPicker.getCurrentEntry();


		// If a valid event exists
		if (newEntry != null && newEntry.songs.length > 0) {


			if (currentEntry == null || newEntry.id != currentEntry.id) waitForSwitchTicks++;
			else waitForSwitchTicks = 0;


			boolean playNewSong = false;
			boolean doSilenceAfter = true;


			// No song is playing and we've waiting through the silence, just start one randomly
			// Skip wait in debug mode
			if (thread.notQueuedOrPlaying()
					&& ((silenceTicks > additionalSilence) || config.debugModeEnabled)) {
				playNewSong = true;
			}

			// the newEntry is defined to always play, just start it immediately
			else if (thread.notQueuedOrPlaying() && newEntry.alwaysPlay) {
				playNewSong = true;
			}

			// if our previous song was defined to stop for this song, then start playing the new event immediately
			else if (thread.notQueuedOrPlaying() && (currentEntry != null && currentEntry.alwaysStop)) {
				playNewSong = true;
			}

			// Fading

			// If we changed what event is active, we need to fade out
			// Wait for a bit to make sure we stay on a different event
			// Also only fade out if it's specifically defined we should stop/start in the songpack
			else if (thread.isPlaying() && currentEntry != null && newEntry.id != currentEntry.id
					&& waitForSwitchTicks > WAIT_FOR_SWITCH_DURATION
					&& (currentEntry.alwaysStop || newEntry.alwaysPlay || config.debugModeEnabled)
			) {

				if (fadeOutTicks < FADE_DURATION) {

					fadeOutTicks++;

					thread.setGainPercentage(1f - (fadeOutTicks / (float)FADE_DURATION));
				}
				else {
					thread.resetPlayer();
					fadeOutTicks = 0;
				}

			}


			if (playNewSong) {
				String picked = SongPicker.pickRandomSong(newEntry.songs);
				changeCurrentSong(picked, newEntry);

				int minTickSilence = 0;
				int maxTickSilence = 0;

				switch (ModConfig.getConfig().musicDelayLength) {
					case SHORT -> {
						minTickSilence = 500;
						maxTickSilence = 1500;
					}
					case NORMAL -> {
						minTickSilence = 1000;
						maxTickSilence = 5000;
					}
					case LONG -> {
						minTickSilence = 2000;
						maxTickSilence = 7000;
					}
				}

				additionalSilence = rand.nextInt(minTickSilence, maxTickSilence);
			}

		}



		thread.processRealGain();

	}



	public static void changeCurrentSong(String song, SongpackEntry newEntry) {
		currentSong = song;
		currentEntry = newEntry;

		String entryName = "";

		for (int i = 0; i < newEntry.events.length; i++) {
			entryName += newEntry.events[i].toString();
		}

		LOGGER.info("Changing entry: " + entryName + " Song name: " + song);

		// go full quiet while switching songs, we'll go back to 1.0f after we load the new song
		thread.setGainPercentage(0.0f);

		thread.play(song);

	}


	public static void refreshSongpack() {

		thread.resetPlayer();
		additionalSilence = 0;

	}





}