package circuitlord.reactivemusic.api.audio;

public interface GainSupplier {
    float supplyComputedPercent();

    void setGainPercent(float p);
    float getGainPercent();

    void setFadePercent(float p);
    float getFadePercent();
    
    void setFadeTarget(float p);
    float getFadeTarget();

    void setFadeDuration(int tickDuration);
    int getFadeDuration();

    void clearFadeStart();
    float getFadeStart();

    boolean isFadingOut();
    boolean isFadingIn();
}
