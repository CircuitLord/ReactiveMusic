# __ReactiveMusicAPI__

**Packages:**

* `circuitlord.reactivemusic.api`
* `circuitlord.reactivemusic.api.audio`
* `circuitlord.reactivemusic.api.eventsys`
* `circuitlord.reactivemusic.api.songpack`


A static, utility-style API for Reactive Music. It exposes global state (current song/entry, selection history) and access to the audio subsystem manager.

_This class is `final` with a private constructor — it’s not intended to be instantiated or extended._

---

# __Key accessible type views__

* ## `SongpackZip`

  __Package:__ `circuitlord.reactivemusic.api.songpack`

  * `Path getPath()`
  * `String getErrorString()` - for custom parsing and logging.
  ---
  * `List<RuntimeEntry> getEntries()`<br>
  Meant to be used with `ReactiveMusicAPI.Songpack.getAvailable()` for plugins that will utilize the songpack config for asset selection.
<!--  -->



* ## `SongpackEvent`

  __Package:__ `circuitlord.reactivemusic.api.songpack`

  * `SongpackEvent get(String id)`
  * `SongpackEvent register(String id)`
  * `SongpackEvent[] values()`
  ---
  * `Map<String, ? extends SongpackEvent> getMap()`<br>
    Returns the songpack event map for the client. This is already passed in `gameTick()` of the final plugin class loaded by the service loader, so you shouldn't need this unless you're doing something that cannot be handled there.
<!--  -->

* ## `EventRecord`

  __Package:__ `circuitlord.reactivemusic.api.eventsys`

  * `String getEventId()`
  * `PluginIdentifier getPluginId()`
<!--  -->

* ## `PluginIdentifier`

  __Package:__ `circuitlord.reactivemusic.api.eventsys`

  * `String getNamespace()`
  * `String getPath()`
  * `String getId()` - Returns `namespace:path`
  * `void setTitle()` - For planned feature - plugin info.

* ## `RuntimeEntry`

  __Package:__ `circuitlord.reactivemusic.api.songpack`

  * `String getSongpack()`
  * `String getEventString()`
  * `String getErrorString()`
  * `List<String> getSongs()` - returns the list of the entry's songs.
  * `boolean fallbackAllowed()` - returns the value of `fallbackAllowed` for the entry.
  * `boolean shouldOverlay()` - returns the value of `useOverlay` for the entry.
  * `boolean shouldStopMusicOnValid()`
  * `boolean shouldStopMusicOnInvalid()`
  * `boolean shouldStartMusicOnValid()`
  * `float getForceChance`
  * `List<RMEntryCondition> getConditions()`
  * `boolean fallbackAllowed()` - returns the value of `allowFallback` for the entry.
  * `boolean shouldOverlay()` - returns the value of `useOverlay` for the entry.  
<!--  -->

* ## `GainSupplier`

  __Package:__ `circuitlord.reactivemusic.api.audio`

  * ### Setters  
    * `void setGainPercent(float p)`
    * `void setFadePercent(float p)`
    * `void setFadeTarget(float p)`
    * `void setFadeDuration(float tickDuration)`

  * ### Getters
    * `float getGainPercent()`
    * `float getFadePercent()`
    * `float getFadeTarget()`
    * `float getFadeStart()` - returns the `float p` of the last `setFadeTarget(float p)` call.
    * `int getFadeDuration()`
  ---
  * `void clearFadeStart()`
  * `float supplyComputedPercent` - used internally in the player implementation.
<!--  -->


* ## `ReactivePlayer`

  __Package:__ `circuitlord.reactivemusic.api.audio`

  * ### Playback Controls
    * `void play()` - make sure to set the source before calling.
    * `void stop()`

  * ### Source Controls
    * `void setSong(String songId)`<br>
       Will resolve to a file in the `music` directory of the songpack. Accepts strings with or without `"music/"` and appends it to the start if not already included.
    ---
    * `void setFile(String fileId)`<br>
      Source setter that does not auto-append `"music/"` to the start. Useful for plugin developers who want to accept separate assets that are not music still within the songpack files.
    ---
    * `void setStream(java.util.function.Supplier<java.io.InputStream> streamSupplier)`<br>
      Advanced custom source - only use if you know what you are doing.

  * ### Gain Controls
    * `ConcurrentHashMap<String, GainSupplier> getGainSuppliers()` - use `.put()` or `.computeIfAbsent()` to add a gain supplier.
    * `void requestGainRecompute()`
    * `void fade(float target, int tickDuration)`<br>
      Sets the fade target and duration values for the primary gain supplier (created by Reactive Music). Fading is handled by the Player Manager each tick.
    ---
    * `void setMute(boolean v)`
    ---

  * ### Player Groups
    * `void setGroup(String group)`
    * `String getGroup()`

  * ### Value Queries
    * `boolean isPlaying()`
    * `boolean isIdle()` - also checks if the player is queued to play.
    ---
    * `boolean stopOnFadeOut()`<br>
      Returns whether the manager will call `.stop()` once the fade percent has reached the target of `0`.
    --- 
    * `boolean resetOnFadeOut()`<br>
      Returns whether the manager will call `.reset()` once the fade percent has reached the target of `0`. 
    ---
    * `float getRealGainDb()`

  * ### Value Setters
    > ⚠️ The following controls are also used internally on the built-in players, and will affect the flow of the core logic if called from there. Please try to use them *only* on players *you* create.
    * `stopOnFadeOut(boolean v)`
    * `resetOnFadeOut(boolean v)`
<!--  -->



* ## `ReactivePlayerManager`

  __Package:__ `circuitlord.reactivemusic.api.audio`

  > ⚠️ Unless you are doing something very complicated, you should not need to instance a *new* manager. Use the following controls from the main player manager.
  * `ReactivePlayer create(String id, ReactivePlayerOptions opts)`
  * `ReactivePlayer get(String id)`
  * `Collection<ReactivePlayer> getAll()`
  * `Collection<ReactivePlayer> getByGroup(String group)`
  * `void setGroupDuck(String group, float percent)`
  * `float getGroupDuck(String group)`
  * `void closeAllForPlugin(String pluginNamespace)`
  * `void closeAll()`
  ---
  > ⚠️ This will only be needed if you instance a new manager. Currently, it handles setting the gain stage for fading.
  * `void tick()`
<!--  -->


  
* ## `ReactivePlayerOptions`

  __Package:__ `circuitlord.reactivemusic.api.audio`

  * `ReactivePlayerOptions namespace(String ns)`
  * `ReactivePlayerOptions group(String g)`
  * `ReactivePlayerOptions loop(boolean v)`
  * `ReactivePlayerOptions autostart(boolean v)`
  * `ReactivePlayerOptions linkToMinecraftVolumes(boolean v)`
  * `ReactivePlayerOptions quietWhenGamePaused(boolean v)`
  * `ReactivePlayerOptions gainRefreshIntervalTicks(int ticks)`
  ---
  * ### Initial Setters
    * `ReactivePlayerOptions gain(float pct)`
    * `ReactivePlayerOptions duck(float pct)`
    * `ReactivePlayerOptions fade(float pct)`

<!--  -->




<br>
<br>
<br>
<br>
<br>
<br>

# __Interfaces__
Accessible through dot chaining.
* `ReactiveMusicAPI.ModConfig`
* `ReactiveMusicAPI.EventSys`
* `ReactiveMusicAPI.Songpack`

<br>
<br>
<br>

## `.ModConfig`
Access configuration settings for the mod, which are set by the player via the UI provided by YetAnotherConfigLib.
### Methods
* `static boolean debugModeEnabled()`

## `.EventSys`
Access values set by the core logic of the __Event System__.
### Methods
* None yet - this interface will mainly be convenience functions for developers working with the event system in a more complex manner.

## `.Songpack`
Access data of imported songpacks, and access values set by the built-in music switching logic. 
### Methods
* `static SongpackZip getCurrent()`
* `static List<SongpackZip> getAvailable()`
* `static RuntimeEntry currentEntry()`
* `static String currentSong()`
* `static List<String> recentSongs()`
* `static List<RuntimeEntry> validEntries()`
* `static List<RuntimeEntry> loadedEntries()`
* `static List<RuntimeEntry> previousValidEntries()`


<br>
<br>
<br>
<br>
<br>
<br>

# __Standalone Methods__

### `static ReactivePlayerManager audioManager()`

Returns the audio subsystem manager (backed by a singleton `RMPlayerManager`). Use this to create players, group them, control ducking, and enumerate active players via `audio().getAll()`.

<br>
<br>
<br>
<br>
<br>
<br>


# __Usage Examples__
This is the built-in actions plugin. In `init()`, we register new events that can be declared in a songpack entry. During `gameTick()` we check if the player is doing something that would trigger the event, and if so we place a `boolean` into the value of the map under the event's key.

```java
package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.eventsys.songpack.SongpackEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class ActionsPlugin extends ReactiveMusicPlugin {
    
    public ActionsPlugin() {
        super("reactivemusic", "actions");
    }

    private static EventRecord FISHING, MINECART, BOAT, HORSE, PIG;

    @Override
    public void init() {
        registerSongpackEvents("FISHING","MINECART","BOAT","HORSE","PIG");
        
        FISHING = SongpackEvent.get("FISHING");
        MINECART = SongpackEvent.get("MINECART");
        BOAT = SongpackEvent.get("BOAT");
        HORSE = SongpackEvent.get("HORSEING");
        PIG = SongpackEvent.get("PIG");
    }
    

    @Override
    public void gameTick(PlayerEntity player, World world, Map<EventRecord, Boolean> eventMap) {
        if (player == null) return;

        eventMap.put(FISHING, player.fishHook != null);

        Entity v = player.getVehicle();
        eventMap.put(MINECART, v instanceof MinecartEntity);
        eventMap.put(BOAT,     v instanceof BoatEntity);
        eventMap.put(HORSE,    v instanceof HorseEntity);
        eventMap.put(PIG,      v instanceof PigEntity);
    }
}
```
---
Here, we are creating a new audio player for the built in Overlay Track feature, which uses the `useOverlay` option from the songpack config.
```java
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
````
