package circuitlord.reactivemusic.impl.audio;

import circuitlord.reactivemusic.ReactiveMusicState;
import circuitlord.reactivemusic.api.audio.ReactivePlayer;
import circuitlord.reactivemusic.api.audio.ReactivePlayerOptions;
import circuitlord.reactivemusic.impl.songpack.MusicPackResource;
import circuitlord.reactivemusic.impl.songpack.RMSongpackLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import rm_javazoom.jl.player.advanced.AdvancedPlayer;
import rm_javazoom.jl.player.AudioDevice;
import rm_javazoom.jl.player.JavaSoundAudioDevice;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RMPlayer implements ReactivePlayer, Closeable {

    public static final String MOD_ID = "reactive_music";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ----- identity / grouping -----
    private final String id;
    private final String namespace;
    private volatile String group;

    // ----- options / state -----
    private final boolean linkToMcVolumes;
    private final boolean quietWhenPaused;
    private volatile boolean loop;
    private volatile boolean mute;

    private volatile float gainPercent;              // user layer (your old gainPercentage)
    private volatile float duckPercent;              // per-player duck
    private final Supplier<Float> groupDuckSupplier; // from manager: returns 1.0f unless group ducked
    private volatile float fadePercent;
    private volatile float fadeTarget;
    private volatile int fadeDuration;
    private volatile boolean stopOnFadeOut = true;
    private volatile boolean resetOnFadeOut = true;
    private volatile boolean fadingOut = false;

    // ----- source -----
    private volatile String songId;                          // resolved via songpack (e.g., "music/Foo")
    private volatile Supplier<InputStream> streamSupplier;   // optional direct supplier
    private volatile String fileId;
    private MusicPackResource currentResource;

    // ----- thread & playback -----
    private volatile boolean kill;           // thread exit
    private volatile boolean queued;         // new source queued
    private volatile boolean queuedToStop;   // stop request
    private volatile boolean paused;         // soft pause flag
    private volatile boolean playing;        // simplified “is playing”
    private volatile boolean complete;       // set by AdvancedPlayer when finished
    private volatile float realGainDb;       // last applied dB

    private AdvancedPlayer player;           // JavaZoom player
    private AudioDevice audio;               // audio device for gain control
    private Thread worker;                   // daemon worker thread

    // callbacks
    private final CopyOnWriteArrayList<Runnable> completeHandlers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Throwable>> errorHandlers = new CopyOnWriteArrayList<>();

    // constants (match your thread’s range)
    private static final float MIN_POSSIBLE_GAIN = -80f;
    private static final float MIN_GAIN = -50f;
    private static final float MAX_GAIN = 0f;

    // This is included just in case we need it down the road somewhere
    @SuppressWarnings("unused")
    private static String normalizeSongFileName(String logicalId) {
        if (logicalId == null || logicalId.isBlank()) return null;
        String name = logicalId.replace('\\','/');     // windows-safe
        if (!name.contains("/")) name = "music/" + name;
        if (!name.endsWith(".mp3")) name = name + ".mp3";
        return name;
    }

    public RMPlayer(String id, ReactivePlayerOptions opts, Supplier<Float> groupDuckSupplier) {
        this.id = Objects.requireNonNull(id);
        this.namespace = opts.pluginNamespace() != null ? opts.pluginNamespace() : "core";
        this.group = opts.group() != null ? opts.group() : "music";
        this.linkToMcVolumes = opts.linkToMinecraftVolumes();
        this.quietWhenPaused = opts.quietWhenGamePaused();
        this.loop = opts.loop();
        this.gainPercent = opts.initialGainPercent();
        this.duckPercent = opts.initialDuckPercent();
        this.fadePercent = opts.initialFadePercent();
        this.fadeTarget = opts.initialFadePercent();
        this.groupDuckSupplier = groupDuckSupplier != null ? groupDuckSupplier : () -> 1.0f;

        this.worker = new Thread(this::runLoop, "ReactiveMusic Player [" + id + "]");
        this.worker.setDaemon(true);
        this.worker.start();

        if (opts.autostart() && (songId != null || streamSupplier != null)) {
            queued = true;
        }
    }

    //package helper - links to RMPlayerManagerImpl
    String getNamespace() { 
        return this.namespace; 
    }

    /** Nudge the player to recompute its effective gain immediately. */
    void recomputeGainNow() { 
        requestGainRecompute(); 
    }

    // ===== RMPlayer =====

    @Override public String id() { return id; }

    @Override public boolean isPlaying() { return playing && !complete; }

    // @Override public boolean isPaused() { return paused; }

    @Override public boolean isFinished() { return complete && !playing; }

    @Override public void setSong(String songId) {
        this.songId = songId;
        this.fileId = null;
        this.streamSupplier = null;
    }

    @Override public void setStream(Supplier<InputStream> stream) {
        this.songId = null;
        this.fileId = null;
        this.streamSupplier = stream;
    }

    @Override public void setFile(String fileName) {
        this.songId = null;
        this.fileId = fileName;
        this.streamSupplier = null;
    }

    @Override public void play() {
        // restart from beginning of current source
        queueStart();
    }

    @Override public void stop() {
        LOGGER.info("Stopping player...");
        if(player != null) {
            player.close();
            queuedToStop = true;
            complete = true;
            queued = false;
        }
		if (currentResource != null && currentResource.fileSystem != null) {
            try {
				currentResource.close();
                LOGGER.info("Resource closed!");
            } catch (Exception e) {
                LOGGER.error("Failed to close file system/input stream " + e.getMessage());
            }
        }
		currentResource = null;
    }

    @Override public void fade(float target, int tickDuration) {
        fadeTarget = target;
        fadeDuration = tickDuration;
    }

    @Override public float getFadeTarget() { return fadeTarget; }
    @Override public int getFadeDuration() { return fadeDuration; }
    @Override public float getFadePercent() { return fadePercent; }
    
    // XXX
    // I know this next pattern isn't idiomatic... but this feels like it's going to get bloated otherwise
    
    // getters
    @Override public boolean isFadingOut() { return fadingOut; }
    @Override public boolean stopOnFadeOut() { return stopOnFadeOut; }
    @Override public boolean resetOnFadeOut() { return resetOnFadeOut; }
    
    // setters
    @Override public void isFadingOut(boolean set) { fadingOut = set; }
    @Override public void stopOnFadeOut(boolean set) { stopOnFadeOut = set; }
    @Override public void resetOnFadeOut(boolean set) { resetOnFadeOut = set; }
    
    
    
    
    @Override public boolean isIdle() {
        // Idle when we have no active/queued playback work
        return !playing && !queued;
    }
    
    // TODO: Figure out how to implement pausing.
    // @Override public void pause() { paused = true; }
    // @Override public void resume() { paused = false; }
    
    @Override public void reset() {
        fadePercent = 1f;
        fadeTarget = 1f;
    }
    
    @Override public void setGainPercent(float p) { gainPercent = clamp01(p); requestGainRecompute(); }
    @Override public void setDuckPercent(float p) { duckPercent = clamp01(p); requestGainRecompute(); }
    @Override public void setFadePercent(float p) { fadePercent = clamp01(p); requestGainRecompute(); }
    
    @Override public void setMute(boolean v) { mute = v; requestGainRecompute(); }

    @Override public float getRealGainDb() { return realGainDb; }

    @Override public void setGroup(String group) { this.group = group; requestGainRecompute(); }
    @Override public String getGroup() { return group; }

    @Override public void onComplete(Runnable r) { if (r != null) completeHandlers.add(r); }
    @Override public void onError(Consumer<Throwable> c) { if (c != null) errorHandlers.add(c); }

    @Override public void close() {
        stop();
        kill = true;
        if (worker != null) worker.interrupt();
        closeQuiet(player);
        player = null;
        audio = null;
    }

    // ===== internal =====

    private void queueStart() {
        this.queuedToStop = false;
        this.complete = false;
        this.queued = true;       // worker will open & play
        this.paused = false;
    }

    private void runLoop() {
        while (!kill) {
            try {
                if (queued) {
                    InputStream in = null;
                    try {
                        if (streamSupplier != null) {
                            in = streamSupplier.get();
                            currentResource = null; // external stream, nothing to close here
                        } else if (fileId != null) {
                            LOGGER.info(this.id + " -> playing from custom resource: " + fileId);
                            currentResource = openFromFile(fileId); // use a custom file found in the songpack
                            if (currentResource == null || currentResource.inputStream == null) {
                                queued = false;
                                continue;
                            }
                        } else {
                            currentResource = openFromSongpack(songId);
                            if (currentResource == null || currentResource.inputStream == null) {
                                queued = false;
                                continue;
                            }
                            in = currentResource.inputStream; // like your original PlayerThread
                        }

                        audio = new FirstWritePrimerAudioDevice(250, () -> requestGainRecompute());
                        player = new AdvancedPlayer(in, audio);
                        
                        
                        queued = false;
                        playing = true;
                        complete = false;


                        if (player.getAudioDevice() != null && !queuedToStop) {
                            player.play();
                        }
                    } finally {
                        // Cleanup player & audio
                        LOGGER.info("[runLoop]: Closing player: " + this.namespace + ":" + this.group);
                        closeQuiet(player);
                        player = null;
                        audio = null;
                        playing = false;
                        complete = true;

                        // Close MusicPackResource like your old resetPlayer() did
                        if (currentResource != null) {
                            try { currentResource.close(); } catch (Exception ignored) {}
                            currentResource = null;
                        }
                    }

                    if (complete && !queuedToStop) {
                        completeHandlers.forEach(RMPlayer::safeRun);
                        if (loop && !kill) queued = true;
                    }
                    queuedToStop = false;
                }

                Thread.sleep(5);
            } catch (Throwable t) {
                for (Consumer<Throwable> c : errorHandlers) safeRun(() -> c.accept(t));
                // reset on error
                closeQuiet(player);
                player = null;
                audio = null;
                playing = false;
                queuedToStop = false;
                queued = false;
                complete = true;
            }
        }
    }

    private static void closeQuiet(AdvancedPlayer p) {
        try { if (p != null) p.close(); } catch (Throwable ignored) {}
    }

    private static void safeRun(Runnable r) {
        try { r.run(); } catch (Throwable ignored) {}
    }

    private MusicPackResource openFromSongpack(String logicalId) {
        if (logicalId == null) return null;

        // Accept "Foo", "music/Foo", or full "music/Foo.mp3"
        String fileName;
        if (logicalId.endsWith(".mp3")) {
            fileName = logicalId;
        } else if (logicalId.startsWith("music/")) {
            fileName = logicalId + ".mp3";
        } else {
            fileName = "music/" + logicalId + ".mp3";
        }

        LOGGER.info("[openFromSongpack]:" + fileName);

        return RMSongpackLoader.getInputStream(
            ReactiveMusicState.currentSongpack.getPath(),
            fileName,
            ReactiveMusicState.currentSongpack.isEmbedded()
        ); // loader returns MusicPackResource{ inputStream, fileSystem? }.
    }

    private MusicPackResource openFromFile(String fileId) {
        String fileName;
        if (fileId == null) return null;
        if (fileId.endsWith(".mp3")) {
            fileName = fileId;
        } else {
            fileName = fileId + ".mp3";
        }

        LOGGER.info("[openFromFile]: " + fileName);

        return RMSongpackLoader.getInputStream(
            ReactiveMusicState.currentSongpack.getPath(),
            fileName,
            ReactiveMusicState.currentSongpack.isEmbedded()
        );
    }

    /**
     * Force a recompute of real dB gain using your existing math.
     * TODO: Implement a hashmap of gain suppliers, that can be registered.
     * This will allow cleaner gain staging by plugins since they can create their
     * own value to affect rather than sharing the built-ins.
     */
    public float requestGainRecompute() {
        if (audio == null) return 0f;
        float minecraftGain = 1.0f;

        if (linkToMcVolumes) {
            // MASTER * MUSIC from Options (same source you used previously)
            minecraftGain = getMasterMusicProduct();          // extract from GameOptions
            // your “less drastic” curve (same intent as your code)
            minecraftGain = (float)Math.pow(minecraftGain, 0.85);
        }

        float quietPct = 1f;
        if (quietWhenPaused && isInGamePausedAndNotOnSoundScreen()) {
            // you targeted ~70% with a gentle lerp; we keep the target value here
            quietPct = 0.7f; 
        }

        float effective = (mute ? 0f : gainPercent) * duckPercent * fadePercent * groupDuckSupplier.get() * quietPct * minecraftGain;
        float db = (minecraftGain == 0f || effective == 0f)
                ? MIN_POSSIBLE_GAIN
                : (MIN_GAIN + (MAX_GAIN - MIN_GAIN) * clamp01(effective));

        // LOGGER.info(String.format(
        //     "RM gain: mute=%s gain=%.2f duck=%.2f group=%.2f quiet=%.2f mc=%.2f -> dB=%.1f",
        //     mute, gainPercent, duckPercent, groupDuckSupplier.get(), 
        //     quietPct, minecraftGain, db
        // ));

        try {
            ((JavaSoundAudioDevice) audio).setGain(db);
            realGainDb = db;
        } catch (Throwable ignored) {}

        return db;
    }

    private static float clamp01(float f) { return f < 0 ? 0 : Math.min(f, 1); }

    // ==== helpers copied from your thread’s logic ====

    private static boolean isInGamePausedAndNotOnSoundScreen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        Screen s = mc.currentScreen;
        if (s == null) return false;
        // You previously compared the translated title to "options.sounds.title" to avoid quieting on that screen
        Text t = s.getTitle();
        if (t == null) return true;
        String lower = t.getString().toLowerCase();
        // crude but effective: don’t “quiet” while on the sound options screen
        boolean onSoundScreen = lower.contains("sound"); // adapt if you kept the exact key match
        return !onSoundScreen;
    }

    private static float getMasterMusicProduct() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return 1f;
        // Replace with exact getters from 1.21.1 GameOptions
        float master = (float) mc.options.getSoundVolume(net.minecraft.sound.SoundCategory.MASTER);
        float music  = (float) mc.options.getSoundVolume(net.minecraft.sound.SoundCategory.MUSIC);
        return master * music;
    }



    /**
     * XXX
     * Full disclosure I have no f***ing idea how this next part works, but it fixes the bug where the audio was
     * blasting for the first bit since gain wasn't getting set before the audio device recieved samples,
     * especially when running a lot of mods.
     * 
     * Thanks, AI. 
     */

    // -------- DROP-IN: put this inside RMPlayerImpl --------
    private final class FirstWritePrimerAudioDevice extends rm_javazoom.jl.player.JavaSoundAudioDevice {
        private final int primeMs;
        private final java.util.function.Supplier<Float> initialDbSupplier;

        private volatile boolean opened = false;
        private volatile boolean primed = false;
        private volatile boolean hwGainApplied = false;

        private javax.sound.sampled.AudioFormat fmt;

        // software gain fallback
        private boolean swGainEnabled = false;
        private float swGainScalar = 1.0f; // multiply samples by this if enabled

        FirstWritePrimerAudioDevice(int primeMs, java.util.function.Supplier<Float> initialDbSupplier) {
            this.primeMs = Math.max(0, primeMs);
            this.initialDbSupplier = initialDbSupplier;
        }

        @Override
        public void open(javax.sound.sampled.AudioFormat format)
                throws rm_javazoom.jl.decoder.JavaLayerException {
            super.open(format);
            this.fmt = format;
            this.opened = true;
            System.err.println("[RMPlayer] open(): fmt=" + format + ", primeMs=" + primeMs);

            // Try to apply initial HW gain now that the line exists
            applyInitialGainOrEnableSoftwareFallback();
        }

        @Override
        public void write(short[] samples, int offs, int len)
                throws rm_javazoom.jl.decoder.JavaLayerException {
            // If mixer didn't call open(AudioFormat) before first write (some forks do this),
            // do best-effort: synthesize a sensible format just for primer sizing.
            if (!opened && fmt == null) {
                fmt = new javax.sound.sampled.AudioFormat(44100f, 16, 2, true, false);
            }

            // Inject primer BEFORE forwarding the very first audible samples.
            if (!primed && primeMs > 0) {
                primed = true;
                int channels = fmt != null ? Math.max(1, fmt.getChannels()) : 2;
                float rate   = fmt != null ? Math.max(8000f, fmt.getSampleRate()) : 44100f;
                int totalSamples = Math.max(channels, Math.round((primeMs / 1000f) * rate) * channels);

                final int CHUNK = 4096;
                short[] zeros = new short[Math.min(totalSamples, CHUNK)];
                int remain = totalSamples;
                System.err.println("[RMPlayer] primer: injecting " + primeMs + "ms silence (" + totalSamples + " samples)");
                while (remain > 0) {
                    int n = Math.min(remain, zeros.length);
                    super.write(zeros, 0, n);
                    remain -= n;
                }

                // If we somehow reached here before open(), try gain now as well.
                if (!hwGainApplied) {
                    applyInitialGainOrEnableSoftwareFallback();
                }
            }

            if (len <= 0) return;

            if (swGainEnabled) {
                // Software-attenuate the buffer on the way out (don’t mutate caller’s array)
                short[] tmp = new short[len];
                for (int i = 0; i < len; i++) {
                    float v = samples[offs + i] * swGainScalar;
                    // clamp to 16-bit
                    if (v > 32767f) v = 32767f;
                    if (v < -32768f) v = -32768f;
                    tmp[i] = (short) v;
                }
                super.write(tmp, 0, len);
            } else {
                super.write(samples, offs, len);
            }
        }

        private void applyInitialGainOrEnableSoftwareFallback() {
            if (hwGainApplied) return;
            Float db = null;
            try {
                db = (initialDbSupplier != null) ? initialDbSupplier.get() : null;
                if (db != null) {
                    // Try hardware gain
                    this.setGain(db);
                    hwGainApplied = true;
                    swGainEnabled = false; // no need for SW gain
                    // reflect to outer field to keep your UI/state in sync
                    try { RMPlayer.this.realGainDb = db; } catch (Throwable ignored) {}
                    System.err.println("[RMPlayer] HW gain applied: " + db + " dB");
                    return;
                }
            } catch (Throwable t) {
                // Hardware control missing or mixer refused it. Fall back to SW.
                System.err.println("[RMPlayer] HW gain failed, enabling SW gain. Reason: " + t);
            }

            // If we get here, enable SW attenuation only if we actually need attenuation
            // (db < 0). If db is null or >= 0, we don’t attenuate in software.
            if (db != null && db < 0f) {
                swGainEnabled = true;
                swGainScalar = (float) Math.pow(10.0, db / 20.0); // dB -> linear
                System.err.println("[RMPlayer] SW gain enabled: " + db + " dB (scalar=" + swGainScalar + ")");
            } else {
                swGainEnabled = false;
            }
        }
    }
    // -------- END DROP-IN --------
}

