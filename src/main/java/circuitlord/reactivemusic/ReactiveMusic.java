package circuitlord.reactivemusic;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class ReactiveMusic implements ModInitializer {

	public static final String MOD_ID = "reactive_music";
	public static final String MOD_VERSION = "0.2";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final int WAIT_FOR_SWITCH_DURATION = 100;
	public static final int FADE_DURATION = 150;
	public static final int SILENCE_DURATION = 100;

	public static int additionalSilence = 0;

	public static PlayerThread thread;

	static String currentSong;
	static int currentEntry = -1;
	
	//static String nextSong;
	static int waitForSwitchTicks = 0;
	static int fadeOutTicks = 0;
	static int fadeInTicks = 0;
	static int silenceTicks = 0;

	static int slowTickUpdateCounter = 0;


	static Random rand = new Random();



	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("╔══════════════════════════════════════════╗");
		LOGGER.info("║     Reactive Music initialization...     ║");
		LOGGER.info("║            ▓▒░ version " + MOD_VERSION +" ░▒▓           ║");
		LOGGER.info("╚══════════════════════════════════════════╝");

		//File configDir = FabricLoader.getInstance().getConfigDir().toFile();

		// TODO: user songpacks
		//File betterMusicDir = new File(configDir.getParentFile(), "better_music");
		//if(!betterMusicDir.exists())
		//	betterMusicDir.mkdir();

		SongLoader.loadFrom(null);

		SongPicker.initialize();

		if(SongLoader.enabled)
			thread = new PlayerThread();

	}

	public static void tick() {

		if (thread == null) return;

		if (!thread.isPlaying()) silenceTicks++;
		else silenceTicks = 0;

		slowTickUpdateCounter++;

		if (slowTickUpdateCounter > 40) {
			SongPicker.tickEventMap();

			slowTickUpdateCounter = 0;
		}


		var entryPair = SongPicker.getCurrentEntry();

		int newEntry = entryPair.getLeft();
		String[] songs = entryPair.getRight();

		// If a valid event exists
		if (newEntry >= 0 && songs.length > 0) {

			if (newEntry != currentEntry) waitForSwitchTicks++;
			else waitForSwitchTicks = 0;


			// No song is playing, just start one randomly
			if (thread.notQueuedOrPlaying() && silenceTicks > SILENCE_DURATION + additionalSilence) {

				String picked = SongPicker.pickRandomSong(songs);
				changeCurrentSong(picked, newEntry);

				// Potentially wait for a while
				additionalSilence = rand.nextInt(2000);
			}

			// If we changed what event is active (with a buffer to prevent quick switches)
			else if (thread.isPlaying() && newEntry != currentEntry && waitForSwitchTicks > WAIT_FOR_SWITCH_DURATION) {

				if (fadeOutTicks < FADE_DURATION) {

					fadeOutTicks++;

					thread.setGainPercentage(1f - (fadeOutTicks / (float)FADE_DURATION));
				}
				else {
					String picked = SongPicker.pickRandomSong(songs);
					changeCurrentSong(picked, newEntry);

					fadeOutTicks = 0;
				}

			}

		}
		else {
			currentEntry = -1;
		}


		thread.processRealGain();

	}



	public static void changeCurrentSong(String song, int newEvent) {
		currentSong = song;
		currentEntry = newEvent;

		String entryName = "";

		for (int i = 0; i < SongLoader.activeSongpack.entries[newEvent].events.length; i++) {
			entryName += SongLoader.activeSongpack.entries[newEvent].events[i].toString();
		}

		LOGGER.info("Changing entry: " + entryName + " Song name: " + song);

		thread.setGainPercentage(1.0f);

		thread.play(song);
	}

}