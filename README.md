Reactive Music trades Minecraft's default music for something dynamic, reactive, and ever-changing. Including a collection of fantasy and celtic tracks to give an air of wonder, a whisper of the unknown, and the call of adventure.

Reactive Music is based off a music pack I originally made for the Ambience mod back in 1.12. Now rebuilt to be a standalone mod for modern minecraft.

Discord: https://discord.gg/vJ6BCjPR3R

# ⚠️ This is an alpha experimental fork of Reactive Music.

New features were added, and the codebase was _**heavily**_ refactored with a lot of changes to internals. And also a whole lot of Javadocs comments for convenience and hopefully, a quicker adoption and improvement of this refreshed codebase that will allow a new ecosystem of **mods made for other mods** using the powerful new tools and systems available to developers through Reactive Music's Songpack & Event system.

Keep reading for a quick overview of the changes and additions.

<details>
    <summary>
        <h2>Code abstraction</h2>
    </summary>

The codebase of Reactive Music `v1.x.x` was monolithic in various places, making it difficult to alter the main flow of the code or add features. I have aimed to improve developer experience through the following changes:

- The built-in events were moved to the new plugin system, allowing logic to be worked on within a final plugin class - making it easier to expand on these features and add new ones.
- The core logic behind the songpack loading & selection systems have been extracted into various new classes, making it easier to reimplement utility methods or hook into their logic at various points in the flow.

</details>
<details>
    <summary>
        <h2>Audio subsystem</h2>
    </summary>

The `PlayerThread` class was a single instance of an audio player - which did it's job very well. But also with heavy restriction. Now, audio players and threads are created through `PlayerManager` classes, which handle tick-based fading, support making external calls, and more importantly - allow *multiple audio streams to exist.* These new instances are fully configurable, and allow for a deeper dive into Reactive Music not only as an event based *music* system for Minecraft, but basically an event based *sound engine*.

Some ideas that I have personally planned using this new functionality:

- Right clicking mobs with an empty hand plays an audio dialogue.
- Various actions the player may randomly trigger self-talk dialogue.
- Adding more immersive sounds to various objects.
- Fire gets louder the more things that are on fire.
- ^ in the same way - more immersive water ambience near specific biomes.

</details>
<details>
    <summary>
        <h2>API entrypoint</h2>
    </summary>

Having a single entry point into Reactive Music's systems means it's easier to modify functionality, or hook into. This also makes developing new plugins for Reactive Music fairly straightforward - with the goal of making it easy to create new functionality around the songpack and event system.

As this fork of the mod is in an experimental alpha state, the API may undergo breaking changes at any time. Be prepared for the possibility of having to refactor your code on new feature releases or API updates.

</details>
<details>
    <summary>
        <h2>ReactiveMusicPlugin.java</h2>
    </summary>

The main addition to this version of Reactive Music is the powerful plugin system. Using a service loader pattern, Reactive Music can now import external classes which follow the structure of the new `ReactiveMusicPlugin` class. To see examples of how this system works, take a look at the code for the built-in events now found in `plugins/`

</details>

---

# Changelog 💃 09.09.25

  Changes:
  
* New `class` based command handler structure for client commands.
* Added various new commands, useful for debugging or to aid in songpack creation.

---
<details>
  <summary>[ 09.09.25 ]</summary>

  Changes:
  
* New `class` based command handler structure for client commands.
* Added various new commands, useful for debugging or to aid in songpack creation.
 
</details>

<details>
  <summary>[ 09.08.25 ]</summary>

  Changes:
  
  * New `class` -> `RMGainSupplier` for usage in the map of gain suppliers in the player implementation.
  * New API interface `GainSupplier`.
  * Changed `requestGainRecompute()` in `RMPlayer` to use gain suppliers instead of hardcoded values.
  * Overlay built-in plugin and Minecraft jukebox ducking now uses gain suppliers in the primary audio player.
 
</details>

<details>
  <summary>[ 09.05.25 ]</summary>

  Changes:
  
  * Changed the final plugin class' API from an `interface` to a `class` to use `extends` instead of `implements`
  * New `class` -> `RMPluginIdentifier` instanced by plugins on construction
  * New `class` -> `RMEventRecord` which holds the registrar plugin's `RMPluginIdentifier`
  * New API interfaces `EventRecord` and `PluginIdentifier`
  * Changed the `Map` in `RMSongpackEvent` to take types `<EventRecord, Boolean>`
  * Code adjusted to use `EventRecord` instead of `SongpackEvent` where applicable.
 
</details>

<details>
  <summary>[ 09.01.25 ]</summary>

  Fixes:

  * Whoops! That was really broken, wasn't it?

</details>
<details>
  <summary>[ 08.31.25 ]</summary>

  Changes:

  * API handles have been modified.
  * Some redundant methods removed.
    
</details>
<details>
  <summary>[ 08.26.25 ]</summary>

  Fixes:
    
  * OverlayTrackPlugin (Built-in) now properly *doesn't* stop the primary music player, by keeping `currentEntry` and disabling parts of `ReactiveMusicCore`. It's a bit coupled for now, but the plan is to clean this up and use it as a base for more expandability through the API.

  Changes:

  * API overhaul, consumer vs internal boundary is clearer and cleaner.
  * Lots of filename changes because of the above point.

</details>
<details>
  <summary>[ 08.21.25 ]</summary>
  Fixes:

  * Implemented a workaround for a bug where the audio player's gain was not set correctly before playing. The audio player is now primed with a small bit of silence before receiving samples.

</details>
<details>
  <summary>[ 08.20.25 ]</summary>

  Fixes:
  
  * Fixed an issue that caused the core logic not to switch to a new song after a song had completed.

  Changes:

  * `currentSong` and `currentEntry` are now accessible through the API.
  * Added a `skip` command, which *should* force the core logic to move on to the next song.

</details>


