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
	static SongpackEntry currentEntry = null;
	
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


			// No song is playing and we've waiting through the silence, just start one randomly
			if (thread.notQueuedOrPlaying() && silenceTicks > SILENCE_DURATION + additionalSilence) {
				playNewSong = true;
			}

			// We're not playing a song and the newEntry is defined to always play, just start it immediately
			else if (thread.notQueuedOrPlaying() && newEntry.alwaysPlay) {
				playNewSong = true;
			}

			// If we changed what event is active, we need to fade out
			// Wait for a bit to make sure we stay on a different event
			// Also only fade out if it's specifically defined we should stop/start in the songpack
			else if (thread.isPlaying() && currentEntry != null && newEntry.id != currentEntry.id
					&& waitForSwitchTicks > WAIT_FOR_SWITCH_DURATION
					&& (currentEntry.alwaysStop || newEntry.alwaysPlay)
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

				// TODO: uncomment
				//additionalSilence = rand.nextInt(2000, 5000);
			}

		}
		else {
			currentEntry = null;
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

		thread.setGainPercentage(1.0f);

		thread.play(song);
	}

}