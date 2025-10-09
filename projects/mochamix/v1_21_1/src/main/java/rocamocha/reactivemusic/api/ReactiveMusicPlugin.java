package rocamocha.reactivemusic.api;

import rocamocha.reactivemusic.ReactiveMusicState;
import rocamocha.reactivemusic.api.eventsys.EventRecord;
import rocamocha.reactivemusic.api.eventsys.PluginIdentifier;
import rocamocha.reactivemusic.api.songpack.RuntimeEntry;
import rocamocha.reactivemusic.api.songpack.SongpackEvent;
import rocamocha.reactivemusic.impl.eventsys.RMEventRecord;
import rocamocha.reactivemusic.impl.eventsys.RMPluginIdentifier;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

import java.util.HashMap;
import java.util.Map;

/**
 * Your plugin class should implement this interface, as it hooks into the flow of Reactive Music's core programming.
 * For your plugin to be recognized and loaded by Reactive Music, create a plaintext file with the class' full
 * package path (ex. <code>rocamocha.reactivemusic.plugins.WeatherAltitudePlugin</code>) on it's own line in
 * <code>resources/META-INF/services</code> 
 * 
 * It is heavily discouraged to import any net.minecraft mappings, unless you are okay with your plugin breaking should mojang
 * make breaking changes to what you implement. If you stick to our stable API, you can build once and forget with version compatibility
 * across any minecraft version that Reactive Music has a stable release for.
 */
public abstract class ReactiveMusicPlugin {

    public static final Map<String, RMPluginIdentifier> pluginMap = new HashMap<>();

    public final RMPluginIdentifier pluginId;
    private int tickInterval = 20;
    /**
     * Constructor ensures developers do not skip initialization.
     * @param namespace
     * @param path
     */
    protected ReactiveMusicPlugin(String namespace, String path) {
        this.pluginId = new RMPluginIdentifier(namespace, path);
    }
    

    /**
     * If your plugin should provide new events, this is where they are declared. 
     * @param eventNames
     */
    public void registerSongpackEvents(String... eventNames) {
        for (String e : eventNames) {
            SongpackEvent.register(new RMEventRecord(e.toUpperCase(), this.pluginId));
        }
    }

    public final void freeze(PluginIdentifier pluginId) { ReactiveMusicState.logicFreeze.put(this.pluginId, true); }
    public final void unfreeze(PluginIdentifier pluginId) { ReactiveMusicState.logicFreeze.put(this.pluginId, false); }

    /**
     * Called during ModInitialize()
     * <p>Use this method to register your new events to the Reactive Music event system.
     * Songpack creators can use these events in their YAML files, it is up to the logic in
     * the overrideable tick methods to set the event states.</p>
     * 
     * @see #tickSchedule()
     * @see #gameTick(PlayerEntity, World, Map)
     * @see #newTick()
     * @see #onValid(RMRuntimeEntry)
     * @see #onInvalid(RMRuntimeEntry)
     */
    public void init() {};
    
    /**
     * Override this method to set a different schedule, or to schedule dynamically.
     * @return The number of ticks that must pass before gameTick() is called each loop.
     */
    public int tickSchedule() { return this.tickInterval; } // per-plugin configurable tick throttling

    /**
     * Called when scheduled. Default schedule is 20 ticks, and can be configured.
     * Provides player, world, and Reactive Music's eventMap for convenience.
     * The player and world are safely mapped through an interface implementation pattern using mixins.
     * @param apiPlayer
     * @param apiWorld
     * @param eventMap
     * @see #tickSchedule()
     */
    public void gameTick(MinecraftPlayer apiPlayer, MinecraftWorld apiWorld, Map<EventRecord, Boolean> eventMap) {};

    /** 
     * Called every tick.
     */
    public void newTick() {};

    /**
     * FIXME: Why isn't this getting called??? Help!
     * Calls when <code>entry</code> flips from invalid -> valid.
     * @param entry
     */
    public void onValid(RuntimeEntry entry) {}
    
    /**
     * FIXME: Why isn't this getting called??? Help!
     * Calls when <code>entry</code> flips from valid -> invalid.
     * @param entry
     */
    public void onInvalid(RuntimeEntry entry) {}
}

