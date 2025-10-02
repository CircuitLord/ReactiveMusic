package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.ReactiveMusicState;
import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.audio.ReactivePlayer;
import circuitlord.reactivemusic.api.audio.ReactivePlayerOptions;
import circuitlord.reactivemusic.api.songpack.RuntimeEntry;

public final class OverlayTrackPlugin extends ReactiveMusicPlugin {
    public OverlayTrackPlugin() {
        super("reactivemusic","overlay");
    }

    ReactivePlayer musicPlayer;
    ReactivePlayer overlayPlayer;

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
            // Only set these flags once when transitioning out of overlay
            musicPlayer.stopOnFadeOut(true);
            musicPlayer.resetOnFadeOut(true);
        }
        
        if (usingOverlay) {
            if (!overlayPlayer.isPlaying()) {
                if (!ReactiveMusicState.validEntries.isEmpty()) {
                    overlayPlayer.setSong(ReactiveMusicUtils.pickRandomSong(SongPicker.getSelectedSongs(ReactiveMusicState.validEntries.get(0), ReactiveMusicState.validEntries)));
                }
                overlayPlayer.getGainSuppliers().get("reactivemusic").setFadePercent(0f);
                overlayPlayer.play();
            }
            overlayPlayer.fade(1f, 140);
            musicPlayer.fade(0f, 70);
            
        }
        if (!usingOverlay) {
            overlayPlayer.fade(0f, 70);
            overlayPlayer.stopOnFadeOut(true);
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
