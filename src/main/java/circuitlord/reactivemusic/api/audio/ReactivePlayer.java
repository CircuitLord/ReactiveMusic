package circuitlord.reactivemusic.api.audio;

import java.util.concurrent.ConcurrentHashMap;

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
    float requestGainRecompute();
    ConcurrentHashMap<String, GainSupplier> getGainSuppliers();
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
    void fade(String gainSupplierId, float target, int tickDuration);
    
    // Fade OUT specific
    boolean stopOnFadeOut();
    boolean resetOnFadeOut();
    
    void stopOnFadeOut(boolean v);
    void resetOnFadeOut(boolean v);
}