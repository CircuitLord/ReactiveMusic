/**
 * This file contains extracted code from ReactiveMusic.java that served as
 * the main logic for songpack loading & selection features.
 * 
 * It is now included in the API package so that plugin developers have convenient access to
 * some functions that relate to parsing the data in songpack entries during runtime.
 */
package circuitlord.reactivemusic;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import circuitlord.reactivemusic.ReactiveMusicDebug.ChangeLogger;
import circuitlord.reactivemusic.api.ReactiveMusicPlugin;
import circuitlord.reactivemusic.api.ReactiveMusicUtils;
import circuitlord.reactivemusic.api.audio.ReactivePlayer;
import circuitlord.reactivemusic.api.audio.ReactivePlayerManager;
import circuitlord.reactivemusic.api.songpack.RuntimeEntry;
import circuitlord.reactivemusic.api.songpack.SongpackZip;
import circuitlord.reactivemusic.config.MusicDelayLength;
import circuitlord.reactivemusic.config.MusicSwitchSpeed;
import circuitlord.reactivemusic.impl.songpack.RMSongpackZip;
import circuitlord.reactivemusic.plugins.OverlayTrackPlugin;

public final class ReactiveMusicCore {
    
    private static final ChangeLogger CHANGE_LOGGER = ReactiveMusic.debugTools.new ChangeLogger();
    private static final ChangeLogger ENTRY_LOGGER = ReactiveMusic.debugTools.new ChangeLogger();
    public static final int FADE_DURATION = 150;
	public static final int SILENCE_DURATION = 100;

    static boolean queuedToPlayMusic = false;
	static boolean queuedToStopMusic = false;
    static int waitForStopTicks = 0;
	static int waitForNewSongTicks = 99999;
	static int fadeOutTicks = 0;
	static int silenceTicks = 0;
    static Random rand = new Random();
    static float randomChance  = rand.nextFloat();

    /**
     * This is the built-in logic for Reactive Music's song switcher.
     * @param players Collection of audio players created by a PlayerManager.
     * @see ReactivePlayerManager
     */
    public static void newTick(Collection<ReactivePlayer> players) {
        RuntimeEntry newEntry = null;

		// Pick the highest priority one
		if (!ReactiveMusicState.validEntries.isEmpty()) {
            for (RuntimeEntry entry : ReactiveMusicState.validEntries) {
                if (entry == null) {
                    ENTRY_LOGGER.writeError("A NULL ENTRY HAS MADE IT INTO THE LIST OF VALID ENTRIES!!!", new UnexpectedException("How did this happen?"));
                    continue;
                }
                if (!entry.shouldOverlay()) { 
                    if (OverlayTrackPlugin.usingOverlay()) {
                        ENTRY_LOGGER.writeInfo("Keeping the current entry under the overlay...");
                        newEntry = ReactiveMusicState.currentEntry;
                        break;
                    }
                    ENTRY_LOGGER.writeInfo("Assigning new entry from valid entries...");
                    newEntry = entry;
                    break;
                }
            }
		}
        else {
            ENTRY_LOGGER.writeInfo("The list of valid entries is empty!");
        }

        for (ReactivePlayer player : players) {
            if (player.isFinished() && !OverlayTrackPlugin.usingOverlay()) {
                ENTRY_LOGGER.writeInfo("The player has finished. Clearing the current entry and song...");
                ReactiveMusicState.currentEntry = null;
                ReactiveMusicState.currentSong = null;
            }
        }


		if (ReactiveMusicState.currentDimBlacklisted)
			newEntry = null;


		if (newEntry != null) {

			List<String> selectedSongs = SongPicker.getSelectedSongs(newEntry, ReactiveMusicState.validEntries);

			// wants to switch if our current entry doesn't exist -- or is not the same as the new one
			boolean wantsToSwitch = !OverlayTrackPlugin.usingOverlay() && (ReactiveMusicState.currentEntry == null || !java.util.Objects.equals(ReactiveMusicState.currentEntry.getEventString(), newEntry.getEventString()));
            
            CHANGE_LOGGER.writeInfo(wantsToSwitch ? "Trying to switch the music." : "The music is no longer attempting to switch.");
			
            // if the new entry contains the same song as our current one, then do a "fake" swap to swap over to the new entry
			if (wantsToSwitch && ReactiveMusicState.currentSong != null && newEntry.getSongs().contains(ReactiveMusicState.currentSong) && !queuedToStopMusic) {
				ReactiveMusicDebug.LOGGER.info("doing fake swap to new event: " + newEntry.getEventString());
				// do a fake swap
				ReactiveMusicState.currentEntry = newEntry;
				wantsToSwitch = false;
				// if this happens, also clear the queued state since we essentially did a switch
				queuedToPlayMusic = false;
			}
                
            boolean isPlaying = false;
            for (ReactivePlayer player : players) {
                if (player.isPlaying()) {
                    isPlaying = true;
                    break;
                }
            }

			// ---- FADE OUT ----
			if ((wantsToSwitch || queuedToStopMusic) && isPlaying && !OverlayTrackPlugin.usingOverlay()) {
				waitForStopTicks++;
				boolean shouldFadeOutMusic = false;
				// handle fade-out if something's playing when a new event becomes valid
				if (waitForStopTicks > getMusicStopSpeed(ReactiveMusicState.currentSongpack)) {
					shouldFadeOutMusic = true;
				}
				// if we're queued to force stop the music, do so here
				if (queuedToStopMusic) {
					shouldFadeOutMusic = true;
				}

				if (shouldFadeOutMusic) {
                    for (ReactivePlayer player : players) {
                        player.stopOnFadeOut(true);
                        player.resetOnFadeOut(true);
                        player.fade(0, FADE_DURATION);
                    }
				}
			}
			else {
				waitForStopTicks = 0;
			}

			//  ---- SWITCH SONG ----
            // TODO: Refactor the overlay check to something expandable.
            // Also --> where else can that be done???
            // Potentially some really cool possibilities with more hooks like that.
            //
			if ((wantsToSwitch || queuedToPlayMusic) && !isPlaying && !OverlayTrackPlugin.usingOverlay()) {
				waitForNewSongTicks++;
				boolean shouldStartNewSong = false;
				if (waitForNewSongTicks > getMusicDelay(ReactiveMusicState.currentSongpack)) {
					shouldStartNewSong = true;
				}
				// if we're queued to start a new song and we're not playing anything, do it
				if (queuedToPlayMusic) {
					shouldStartNewSong = true;
				}
				if (shouldStartNewSong) {
					String picked = ReactiveMusicUtils.pickRandomSong(selectedSongs);
                    for (ReactivePlayer player : players) {
					    changeCurrentSong(picked, newEntry, player);
                    }
					waitForNewSongTicks = 0;
					queuedToPlayMusic = false;
				}
			}
			else {
				waitForNewSongTicks = 0;
			}
		}

		// no entries are valid, we shouldn't be playing any music!
		// this can happen if no entry is valid or the dimension is blacklisted
		else {
            CHANGE_LOGGER.writeInfo("There are no valid songpack entries!");
            for (ReactivePlayer player : players) {
                player.stopOnFadeOut(true);
                player.resetOnFadeOut(true);
			    player.fade(0, FADE_DURATION);
            }
		}
	}
    
    public static List<RuntimeEntry> getValidEntries() {
        List<RuntimeEntry> validEntries = new ArrayList<>();
    
        for (RuntimeEntry loadedEntry : ReactiveMusicState.loadedEntries) {
    
            boolean isValid = SongPicker.isEntryValid(loadedEntry);
    
            if (isValid) {
                validEntries.add(loadedEntry);
            }
        }
    
        return validEntries;
    }
    
    public final static void processValidEvents(List<RuntimeEntry> validEntries, List<RuntimeEntry> previousValidEntries) {

        for (var entry : previousValidEntries) {
            // if this event was valid before and is invalid now
            if (validEntries.stream().noneMatch(e -> java.util.Objects.equals(e.getEventString(), entry.getEventString()))) {
                
                ReactiveMusicDebug.LOGGER.info("Triggering onInvalid() for songpack event plugins");
                for (ReactiveMusicPlugin plugin : ReactiveMusic.PLUGINS) plugin.onInvalid(entry);
    
                if (entry.shouldStopMusicOnInvalid()) {
                    ReactiveMusicDebug.LOGGER.info("trying forceStopMusicOnInvalid: " + entry.getEventString());
                    if (randomChance  <= entry.getForceChance()) {
                        ReactiveMusicDebug.LOGGER.info("doing forceStopMusicOnInvalid: " + entry.getEventString());
                        queuedToStopMusic = true;
                    }
                    break;
                }
            }
        }
        
        for (var entry : validEntries) {
    
            if (previousValidEntries.stream().noneMatch(e -> java.util.Objects.equals(e.getEventString(), entry.getEventString()))) {
                // use the same random chance for all so they always happen together
                boolean randSuccess = randomChance <= entry.getForceChance();
    
                // if this event wasn't valid before and is now
                ReactiveMusicDebug.LOGGER.info("Triggering onValid() for songpack event plugins");
                for (ReactiveMusicPlugin plugin : ReactiveMusic.PLUGINS) plugin.onValid(entry);
    
                if (entry.shouldStopMusicOnValid()) {
                    ReactiveMusicDebug.LOGGER.info("trying forceStopMusicOnValid: " + entry.getEventString());
                    if (randSuccess) {
                        ReactiveMusicDebug.LOGGER.info("doing forceStopMusicOnValid: " + entry.getEventString());
                        queuedToStopMusic = true;
                    }
                }
                if (entry.shouldStartMusicOnValid()) {
                    ReactiveMusicDebug.LOGGER.info("trying forceStartMusicOnValid: " + entry.getEventString());
                    if (randSuccess) {
                        ReactiveMusicDebug.LOGGER.info("doing forceStartMusicOnValid: " + entry.getEventString());
                        queuedToPlayMusic = true;
                    }
                }
            }
        }
    }
    
    
    public static void changeCurrentSong(String song, RuntimeEntry newEntry, ReactivePlayer player) {
        // No change? Do nothing.
        if (java.util.Objects.equals(ReactiveMusicState.currentSong, song)) {
            queuedToPlayMusic = false;
            return;
        }
    
        // Stop only if we’re switching tracks (not just metadata)
        final boolean switchingTrack = !java.util.Objects.equals(ReactiveMusicState.currentSong, song);
        if (switchingTrack && player != null && player.isPlaying()) {
            player.stop(); // RMPlayerImpl stops underlying AdvancedPlayer.play()
        }
    
        ReactiveMusicState.currentSong = song;
        ReactiveMusicState.currentEntry = newEntry;
    
        if (player != null && song != null) {
            
            // if you do a fade-in elsewhere, set 0 here; otherwise set 1
            player.getGainSuppliers().get("reactivemusic").setGainPercent(1f);
            player.getGainSuppliers().get("reactivemusic").setFadePercent(1f);
            player.getGainSuppliers().get("reactivemusic").setFadeTarget(1f);
            
            player.getGainSuppliers().get("reactivemusic-duck").setGainPercent(1f);
            player.getGainSuppliers().get("reactivemusic-duck").setFadePercent(1f);
            player.getGainSuppliers().get("reactivemusic-duck").setFadeTarget(1f);
            
            player.requestGainRecompute();
            player.setSong(song);   // resolves to music/<song>.mp3 inside RMPlayerImpl
            player.play();          // worker thread runs blocking play() internally
        }
    
        queuedToPlayMusic = false;
    }
    
    
    
    public static final void setActiveSongpack(RMSongpackZip songpackZip) {
    
        // TODO: Support more than one songpack?
        if (ReactiveMusicState.currentSongpack != null) {
            deactivateSongpack(ReactiveMusicState.currentSongpack);
        }
    
        for (ReactivePlayer player : ReactiveMusic.audio().getAll()) {
            resetPlayer(player);
        }

        ReactiveMusicState.currentEntry = null;
        ReactiveMusicState.currentSong = null;
    
        ReactiveMusicState.currentSongpack = songpackZip;
    
        ReactiveMusicState.loadedEntries = songpackZip.runtimeEntries;
    
        // always start new music immediately
        queuedToPlayMusic = true;
    
    }
    
    public static final void deactivateSongpack(SongpackZip songpackZip) {
    
        // remove all entries that match that name
        for (int i = ReactiveMusicState.loadedEntries.size() - 1; i >= 0; i--) {
            if (ReactiveMusicState.loadedEntries.get(i).getSongpack() == songpackZip.getConfig().name) {
                ReactiveMusicState.loadedEntries.remove(i);
            }
        }
    
    }
    
    public final static int getMusicStopSpeed(SongpackZip songpack) {
    
        MusicSwitchSpeed speed = ReactiveMusic.modConfig.musicSwitchSpeed2;
    
        if (ReactiveMusic.modConfig.musicSwitchSpeed2 == MusicSwitchSpeed.SONGPACK_DEFAULT) {
            speed = songpack.getConfig().musicSwitchSpeed;
        }
    
        if (ReactiveMusic.modConfig.debugModeEnabled) {
            speed = MusicSwitchSpeed.INSTANT;
        }
    
        switch (speed) {
            case INSTANT:
                return 100;
            case SHORT:
                return 250;
            case NORMAL:
                return 900;
            case LONG:
                return 2400;
            default:
                break;
        }
    
        return 100;
    
    }
    
    public final static int getMusicDelay(SongpackZip songpack) {
    
        MusicDelayLength delay = ReactiveMusic.modConfig.musicDelayLength2;
    
        if (ReactiveMusic.modConfig.musicDelayLength2 == MusicDelayLength.SONGPACK_DEFAULT) {
            delay = songpack.getConfig().musicDelayLength;
        }
    
        if (ReactiveMusic.modConfig.debugModeEnabled) {
            delay = MusicDelayLength.NONE;
        }
    
        switch (delay) {
            case NONE:
                return 0;
            case SHORT:
                return 250;
            case NORMAL:
                return 900;
            case LONG:
                return 2400;
            default:
                break;
        }
    
        return 100;
    
    }
    
    public static final void resetPlayer(ReactivePlayer player) {
        if (player != null && player.isPlaying()) {
            player.stop();
            player.reset();
        }
    }

}
