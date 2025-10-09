package rocamocha.reactivemusic.impl.audio;

import rocamocha.reactivemusic.api.audio.GainSupplier;

public class RMGainSupplier implements GainSupplier {

    //defaults
    private volatile float gainPercent = 1f;
    private volatile float fadePercent = 1f;
    private volatile float fadeTarget = 1f;
    private volatile float fadeStart = -1; // set on fade calls
    private volatile int fadeDuration = 60;

    public RMGainSupplier(float initialPercent) {
        this.gainPercent = initialPercent;
    }

    public float supplyComputedPercent() { return gainPercent * fadePercent;}

    // gain %
    public float getGainPercent() { return gainPercent; }
    public void setGainPercent(float p) { gainPercent = p; }

    // fade %
    public void setFadePercent(float p) { fadePercent = p; }
    public float getFadePercent() { return fadePercent; }

    // fade target
    public void setFadeTarget(float p) { 
        if (fadeTarget != p) {
            fadeStart = fadePercent; // where were we?
        }
        fadeTarget = p; 
    }
    public float getFadeTarget() { return fadeTarget; }

    // fade start %
    public float getFadeStart() { return fadeStart; }
    public void clearFadeStart() { fadeStart = -1; }

    // fade duration
    public void setFadeDuration(int tickDuration) {}
    public int getFadeDuration() { return fadeDuration; }

    //flags
    public boolean isFadingOut() { return (fadeTarget == 0 && fadeStart > 0); }
    public boolean isFadingIn() { return (fadeStart == 0 && fadeTarget > 0); }

    /**
     * Wrapper for hooked functions from RMPlayerManager
     * @see RMPlayerManager#tick()
     */
    public void tick() {
        
    }
}
