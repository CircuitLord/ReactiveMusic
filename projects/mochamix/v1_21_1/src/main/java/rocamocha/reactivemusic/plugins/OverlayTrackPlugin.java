package rocamocha.reactivemusic.plugins;

import java.util.List;

import rocamocha.reactivemusic.ReactiveMusic;
import rocamocha.reactivemusic.ReactiveMusicState;
import rocamocha.reactivemusic.SongPicker;
import rocamocha.reactivemusic.api.*;
import rocamocha.reactivemusic.api.audio.ReactivePlayer;
import rocamocha.reactivemusic.api.audio.ReactivePlayerOptions;
import rocamocha.reactivemusic.api.songpack.RuntimeEntry;

public final class OverlayTrackPlugin extends ReactiveMusicPlugin {
    public OverlayTrackPlugin() {
        super("reactivemusic","overlay");
    }

    ReactivePlayer musicPlayer;
    ReactivePlayer overlayPlayer;
    public static RuntimeEntry currentEntry;
    private RuntimeEntry previousOverlayEntry = null;

    @Override public void init() {
        ReactiveMusicState.LOGGER.info("Initializing " + pluginId.getId() + " plugin");
        musicPlayer = ReactiveMusicAPI.audioManager().get("reactive:music");

        ReactiveMusicAPI.audioManager().create(
            "reactive:overlay",
            ReactivePlayerOptions.create()
            .namespace("reactive")
            .group("overlay")
            .loop(false)
            .gain(1.0f)
            .fade(0f)
            .quietWhenGamePaused(false)
            .linkToMinecraftVolumes(true)
        );
            
        
        overlayPlayer = ReactiveMusic.audio().get("reactive:overlay");
    }
    
    private boolean wasUsingOverlay = false;
    
    @Override public void newTick() {
        boolean usingOverlay = usingOverlay();
        
        // guard the call
        if (musicPlayer == null || overlayPlayer == null) { return; }
        
        // State transition: starting overlay
        if (usingOverlay && !wasUsingOverlay) {
            musicPlayer.stopOnFadeOut(false);
            musicPlayer.resetOnFadeOut(false);
        }
        
        // State transition: stopping overlay  
        if (!usingOverlay && wasUsingOverlay) {
            // Restore normal main player fade-out behavior for song switching
            musicPlayer.stopOnFadeOut(true);
            musicPlayer.resetOnFadeOut(true);
        }
        
        if (usingOverlay) {
            // Find the current overlay entry
            RuntimeEntry newOverlayEntry = null;
            for (RuntimeEntry entry : ReactiveMusicState.validEntries) {
                if (entry.shouldOverlay()) {
                    newOverlayEntry = entry;
                    break;
                }
            }
            
            // Check if overlay entry has changed
            boolean overlayEntryChanged = !java.util.Objects.equals(previousOverlayEntry, newOverlayEntry);
            
            // Update current entry
            currentEntry = newOverlayEntry;
            
            // If entry changed, stop current overlay and start new one
            if (overlayEntryChanged) {
                if (overlayPlayer.isPlaying()) {
                    overlayPlayer.stop();
                }
                if (currentEntry != null) {
                    List<String> songs = SongPicker.getSelectedSongs(currentEntry, ReactiveMusicState.validEntries);
                    overlayPlayer.setSong(ReactiveMusicUtils.pickRandomSong(songs));
                    overlayPlayer.getGainSuppliers().get("reactivemusic").setFadePercent(0f);
                    overlayPlayer.play();
                }
            } else {
                // Same entry - just ensure it's playing if needed
                if (!overlayPlayer.isPlaying() && currentEntry != null) {
                    // Don't reset fade percent - let it resume from current fade state
                    overlayPlayer.play();
                }
            }
            overlayPlayer.fade(1f, 140);
            musicPlayer.fade(0f, 70);
            
            // Update tracking
            previousOverlayEntry = newOverlayEntry;
        }
        if (!usingOverlay) {
            overlayPlayer.fade(0f, 70);
            // Don't stop or reset the overlay player - keep it playing at 0 volume for seamless re-entry
            overlayPlayer.stopOnFadeOut(false);
            overlayPlayer.resetOnFadeOut(false);
            // Don't clear entries yet - keep them for potential seamless re-entry
            
            // Ensure main player fades back in when overlay stops
            if (wasUsingOverlay) {
                musicPlayer.fade(1f, 140);
            }
        }
        
        wasUsingOverlay = usingOverlay;
    };
    
    /**
     * Calling this from <code>newTick()</code> for now since the event processing calls are broken...
     * Or is it? It's not logging, but sometimes the main player breaks.
     * @return
     */
    public static boolean usingOverlay() {
        // FIXME: Overlay should only activate is the entry is higher prio
        // ???: Should prio be checked here or in core logic?
        for (RuntimeEntry entry : ReactiveMusicState.validEntries) {
            if (entry.shouldOverlay()) {
                return true;
            }
        }
        return false;
    }
}
