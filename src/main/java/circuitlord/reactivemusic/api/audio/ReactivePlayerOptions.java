package circuitlord.reactivemusic.api.audio;

/** Builder-style options for creating RMPlayers. */
public final class ReactivePlayerOptions {
    // --- sensible defaults ---
    private String pluginNamespace = "default";
    private String group = "default";
    private boolean loop = false;
    private boolean autostart = true;
    private boolean linkToMinecraftVolumes = true;  // MASTER * MUSIC coupling
    private boolean quietWhenGamePaused = true;     // “quiet” layer when paused
    private int gainRefreshIntervalTicks = 10;      // 0/1 = every tick

    private float initialGainPercent = 1.0f;        // 0..1
    private float initialDuckPercent = 1.0f;        // 0..1
    private float initialFadePercent = 0.0f;        // 0..1

    private ReactivePlayerOptions() {}

    /** Start a new options object with defaults. */
    public static ReactivePlayerOptions create() { return new ReactivePlayerOptions(); }

    // --- fluent setters (all return this) ---
    public ReactivePlayerOptions namespace(String ns) { this.pluginNamespace = ns; return this; }
    public ReactivePlayerOptions group(String g) { this.group = g; return this; }
    public ReactivePlayerOptions loop(boolean v) { this.loop = v; return this; }
    public ReactivePlayerOptions autostart(boolean v) { this.autostart = v; return this; }

    public ReactivePlayerOptions linkToMinecraftVolumes(boolean v) { this.linkToMinecraftVolumes = v; return this; }
    public ReactivePlayerOptions quietWhenGamePaused(boolean v) { this.quietWhenGamePaused = v; return this; }
    public ReactivePlayerOptions gainRefreshIntervalTicks(int ticks) { this.gainRefreshIntervalTicks = Math.max(0, ticks); return this; }

    /** Initial volume [0..1]. */
    public ReactivePlayerOptions gain(float pct) { this.initialGainPercent = clamp01(pct); return this; }

    /** Initial per-player duck [0..1]. Multiplies with any group duck. */
    public ReactivePlayerOptions duck(float pct) { this.initialDuckPercent = clamp01(pct); return this; }
    public ReactivePlayerOptions fade(float pct) { this.initialFadePercent = clamp01(pct); return this; }

    // --- getters (used by the manager/impl) ---
    public String pluginNamespace() { return pluginNamespace; }
    public String group() { return group; }
    public boolean loop() { return loop; }
    public boolean autostart() { return autostart; }
    public boolean linkToMinecraftVolumes() { return linkToMinecraftVolumes; }
    public boolean quietWhenGamePaused() { return quietWhenGamePaused; }
    public int gainRefreshIntervalTicks() { return gainRefreshIntervalTicks; }
    public float initialGainPercent() { return initialGainPercent; }
    public float initialDuckPercent() { return initialDuckPercent; }
    public float initialFadePercent() { return initialFadePercent; }

    private static float clamp01(float f) { return (f < 0f) ? 0f : (f > 1f ? 1f : f); }
}
