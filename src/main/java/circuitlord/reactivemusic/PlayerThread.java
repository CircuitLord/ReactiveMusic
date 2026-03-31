package circuitlord.reactivemusic;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.sound.SoundCategory;
import rm_javazoom.jl.player.AudioDevice;
import rm_javazoom.jl.player.JavaSoundAudioDevice;
import rm_javazoom.jl.player.advanced.AdvancedPlayer;
import net.minecraft.text.TranslatableTextContent;

import java.io.IOException;
import java.io.InputStream;

public class PlayerThread extends Thread {

	public static final float MIN_POSSIBLE_GAIN = -80F;
	public static final float MIN_GAIN = -50F;
	public static final float MAX_GAIN = 0F;

	//public static float[] fadeGains;
	
	static {
/*		fadeGains = new float[ReactiveMusic.FADE_DURATION];
		float totaldiff = MIN_GAIN - MAX_GAIN;
		float diff = totaldiff / fadeGains.length;
		for(int i = 0; i < fadeGains.length; i++)
			fadeGains[i] = MAX_GAIN + diff * i;*/

		// Invert because we have fade ticks counting up now
		//for (int i = fadeGains.length - 1; i >= 0; i--) {
		//	fadeGains[i] = MAX_GAIN + diff * (fadeGains.length - 1 - i);
		//}
	}
	
	public volatile static float gainPercentage = 1.0f;
	public volatile static float musicDiscDuckPercentage = 1.0f;

	public static final float QUIET_VOLUME_PERCENTAGE = 0.7f;
	public static final float QUIET_VOLUME_LERP_RATE = 0.02f;
	public static float quietPercentage = 1.0f;

	public volatile static float realGain = 0;

	public volatile static String currentSong = null;
	public volatile static String currentSongChoices = null;

	public volatile MusicPackResource currentSongResource = null;
	
	AdvancedPlayer player;

	private volatile boolean queued = false;

	private volatile boolean kill = false;
	private volatile boolean playing = false;


	boolean notQueuedOrPlaying() {
		return !(queued || isPlaying());
	}

	boolean isPlaying() {
		return playing && !player.getComplete();
	}
	
	public PlayerThread() {
		setDaemon(true);
		setName("ReactiveMusic Player Thread");
		start();
	}

	@Override
	public void run() {
		try {
			while(!kill) {

				if(queued && currentSong != null) {

					currentSongResource = RMSongpackLoader.getInputStream(ReactiveMusic.currentSongpack.path, "music/" + currentSong + ".mp3", ReactiveMusic.currentSongpack.embedded);
					if(currentSongResource == null || currentSongResource.inputStream == null)
						continue;

					player = new AdvancedPlayer(currentSongResource.inputStream);
					queued = false;

				}


				if(player != null && player.getAudioDevice() != null) {

					// go to full volume
					setGainPercentage(1.0f);
					processRealGain();

					ReactiveMusic.LOGGER.info("Playing " + currentSong);
					playing = true;
					player.play();

				}

			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}



	public void resetPlayer() {
		playing = false;

		if(player != null)
			player.queuedToStop = true;

		queued = false;
		currentSong = null;

		if (currentSongResource != null && currentSongResource.fileSystem != null) {
            try {
				currentSongResource.close();
            } catch (Exception e) {
                ReactiveMusic.LOGGER.error("Failed to close file system/input stream " + e.getMessage());
            }
        }

		currentSongResource = null;
	}

	public void play(String song) {
		resetPlayer();

		currentSong = song;
		queued = true;
	}
	
/*	public float getGain() {
		if(player == null)
			return gain;
		
		AudioDevice device = player.getAudioDevice();
		if(device != null && device instanceof JavaSoundAudioDevice)
			return ((JavaSoundAudioDevice) device).getGain();
		return gain;
	}*/
	
/*	public void addGain(float gain) {
		setGain(getGain() + gain);
	}*/
	
	public void setGainPercentage(float newGain) {
		gainPercentage = Math.min(1.0f, Math.max(0.0f, newGain));
	}

	public void setMusicDiscDuckPercentage(float newGain) {
		musicDiscDuckPercentage = newGain;
	}
	
	public void processRealGain() {

		var client = MinecraftClient.getInstance();

		GameOptions options = MinecraftClient.getInstance().options;

		boolean musicOptionsOpen = false;

		// Try to find the music options menu
		TranslatableTextContent ScreenTitleContent = null;
		if (client.currentScreen != null && client.currentScreen.getTitle() != null && client.currentScreen.getTitle().getContent() != null
			&& client.currentScreen.getTitle().getContent() instanceof TranslatableTextContent) {

			ScreenTitleContent = (TranslatableTextContent) client.currentScreen.getTitle().getContent();

			if (ScreenTitleContent != null) {
				musicOptionsOpen = ScreenTitleContent.getKey().equals("options.sounds.title");
			}
		}


		boolean doQuietMusic =  client.isPaused()
				&& client.world != null
				&& !musicOptionsOpen;


		float targetQuietMusicPercentage = doQuietMusic ? QUIET_VOLUME_PERCENTAGE : 1.0f;
        quietPercentage = MyMath.lerpConstant(quietPercentage, targetQuietMusicPercentage, QUIET_VOLUME_LERP_RATE);

		
		float minecraftGain = options.getSoundVolume(SoundCategory.MUSIC) * options.getSoundVolume(SoundCategory.MASTER);

		// my jank way of changing the volume curve to be less drastic
		float minecraftDistFromMax = 1.0f - minecraftGain;
		float minecraftGainAddScalar = (minecraftDistFromMax * 1.0f) * minecraftGain;
		// cap to 1.0
		minecraftGain = Math.min(minecraftGain + minecraftGainAddScalar, 1.0f);

		//ReactiveMusic.LOGGER.info("minecraft fake gain: " + minecraftGain);


		float newRealGain = MIN_GAIN + (MAX_GAIN - MIN_GAIN) * minecraftGain * gainPercentage * quietPercentage * musicDiscDuckPercentage;

		// Force to basically off if the user sets their volume off
		if (minecraftGain <= 0) {
			newRealGain = MIN_POSSIBLE_GAIN;
		}

		//ReactiveMusic.LOGGER.info("Current gain: " + newRealGain);

		realGain = newRealGain;
		if(player != null) {
			AudioDevice device = player.getAudioDevice();
			if(device != null && device instanceof JavaSoundAudioDevice) {
				try {
					((JavaSoundAudioDevice) device).setGain(newRealGain);
				} catch(IllegalArgumentException e) {
					ReactiveMusic.LOGGER.error(e.toString());
				}
			}
		}
		
		//if(musicGain == 0)
		//	play(null);
	}

	
/*	public float getRelativeVolume() {
		return getRelativeVolume(getGain());
	}*/
	
/*	public float getRelativeVolume(float gain) {
		float width = MAX_GAIN - MIN_GAIN;
		float rel = Math.abs(gain - MIN_GAIN);
		return rel / Math.abs(width);
	}*/

/*	public int getFramesPlayed() {
		return player == null ? 0 : player.getFrames();
	}*/
@SuppressWarnings("removal")
	public void forceKill() {
		try {
			resetPlayer();
			interrupt();

			finalize();
			kill = true;
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
}
