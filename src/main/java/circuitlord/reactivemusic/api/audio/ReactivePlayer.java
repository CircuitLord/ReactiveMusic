package circuitlord.reactivemusic.api.audio;

public interface ReactivePlayer extends AutoCloseable {
    String id();                         // unique handle, e.g. "myplugin:ambient-1"
    boolean isPlaying();
    // boolean isPaused();
    boolean isIdle();
    boolean isFinished();

    void play();                         // (re)start from beginning
    void stop();                         // stop + release decoder
    // void pause();                        // pause without releasing resources
    // void resume();

    // Source
    void setSong(String songId);         // e.g. "music/ForestTheme" -> resolves to music/ForestTheme.mp3 in active songpack
    void setStream(java.util.function.Supplier<java.io.InputStream> streamSupplier); // custom source
    void setFile(String fileId);

    // Gain / routing
    void setGainPercent(float p);        // 0..1 user layer (your existing gainPercentage)
    void setDuckPercent(float p);        // 0..1 “duck” layer (like musicDiscDuckPercentage)
    void setMute(boolean v);
    float getRealGainDb();               // last applied dB to audio device
    
    // Grouping / coordination
    void setGroup(String group);         // e.g. "music", "ambient", "sfx"
    String getGroup();
    
    // Events
    void onComplete(Runnable r);         // fires when track completes
    void onError(java.util.function.Consumer<Throwable> c);
    
    void close();              // same as stop(); also unregister
    void reset();
    
    // More controls & accessors
    void fade(float target, int tickDuration);
    void setFadePercent(float p);
    float getFadePercent();
    float getFadeTarget();
    int getFadeDuration();
    
    // Fade OUT specific
    boolean isFadingOut();
    boolean stopOnFadeOut();
    boolean resetOnFadeOut();
    
    void isFadingOut(boolean v);
    void stopOnFadeOut(boolean v);
    void resetOnFadeOut(boolean v);
}