
# Making Songpacks

NOTE: You can find a video version of this wiki page [here](https://www.youtube.com/watch?v=YvEap8IUS-c)!

Songpacks are constructed as folders with a yaml configuration file and a folder with mp3 music files.

They're loaded from the `resourcepacks` folder, although they're not actually resource packs and are instead selected from the configuration UI. This is purely to make including songpacks easier.

**Download the Songpack Template [here](https://raw.githubusercontent.com/CircuitLord/ReactiveMusic/master/docs/ReactiveMusicSongpackTemplate-v6.zip)!**

<br>

## Testing Songpacks

Open the songpack menu using `/reactivemusic`. Click on the button for your songpack name and it'll start playing.
This also works for reloading a songpack if you've made changes you want to try without restarting your game by clicking another songpack and back onto yours.

I also highly recommend turning debug mode on in the Debug section,
this will make songs switch whenever their events become valid and removes silence gaps entirely to make it easier to test.

You can also run `/reactivemusic toggleLogging` to display what the mod is doing in real-time.

<br>

## Songpack Configuration

`ReactiveMusic.yaml` is where all the configuration for your songpack takes place. Let's go over all the properties available in a songpack config.

### Global configuration:
```
name: "My Awesome Songpack"
version: "1.0"
author: "CircuitLord"
description: "A really good songpack that I made"
credits: "Cool people here"

musicSwitchSpeed: NORMAL
musicDelayLength: NORMAL
```

- `(String) name` The unique name/identifier for your songpack.
-  `musicSwitchSpeed` (INSTANT, SHORT, NORMAL, LONG) Defines the default for how fast music will stop when it's event becomes invalid
-  `musicDelayLength` (NONE, SHORT, NORMAL, LONG) Defines the default for how much silence there should be before a new song starts playing again

<br><br>

## Songpack Entries:

Songpack entries define what songs play in specific events. You can have as many entries as you want, and they're prioritized internally based on what order you put them in.

Here's an example songpack entry:

```
- events: [ "MAIN_MENU" ]
  songs:
    - "DuNock-Street-TitleEdit"
```

- `(SongpackEventType) events` The specific events that need to be valid for this entry to play. (see details below)
- `(Boolean) allowFallback` (default false) If we've played all songs from this event, should  we "fallback" to other events that are also valid? (good for one-off events you don't want to play over and over)
- `(String[]) songs` The list of song files to pick from when this event plays. These are picked from the `music` sub-folder.

<br><br>

There are also a couple of other advanced params you can use:

`(Boolean) forceStopMusicOnChanged` (default false) Force stop the current music when this event becomes valid/invalid. Good for when you definitely want the music to switch from whatever's currently playing (boss, nether, etc)
- this also has variations as `forceStopMusicOnValid` and `forceStopMusicOnInvalid` if you want specific behavior

`(Boolean) forceStartMusicOnValid` (default false) If this event becomes valid, and the music stops naturally or because of forceStop, then play this event immediately.

`(float) forceChance` (default 1.0f) If forceStop/Start is enabled, what is the chance it happens? Good for events where you only want the music to switch sometimes.

<br><br>


### IMPORTANT: Spacing matters! Use tabs to properly indent the entries and their properties.
A proper entry looks like this!
```
(tab) - events: [ "DAY" ]
(tab)(tab)alwaysPlay: true
(tab)(tab)songs:
(tab)(tab)(tab) - "MyAwesomeSong"
(tab)(tab)(tab) - "MyOtherCoolSong"
```

<br><br>

You can also combine multiple events for more specific songs, or do an "OR" condition

```
  - events: [ "DAY", "BIOME=MOUNTAIN" ]
    songs:
      - "ForTheKing"
      - "Freedom"

  - events: [ "BIOME=ocean || UNDERWATER" ]
    songs:
      - "Freedom"
```

<br><br>

## Events (SongpackEventTypes)

This lists all the available songpack events you have available.

### Special
- `MAIN_MENU`
- `CREDITS`
- `HOME` (within 45 blocks of bed)

### Time
- `DAY`
- `NIGHT`
- `SUNRISE`
- `SUNSET`

### Weather
- `RAIN`
- `SNOW`
- `STORM`

### World Height
- `UNDERWATER`
- `UNDERGROUND`
- `DEEP_UNDERGROUND`
- `HIGH_UP`

### Entities
- `NEARBY_MOBS` (nearby >= 1 monster, may not work that well and may be replaced with combat events soon)
- `MINECART`
- `BOAT`
- `HORSE`
- `PIG`

### Actions
- `FISHING`
- `DYING`

### Location
- `VILLAGE` (nearby villagers)

### Combat
- `BOSS` (whenever a boss bar is on-screen)

<br><br>

## Biome Events

You can search for any biome by using `BIOME=biomename`. This can be the full biome name or just a part of it if you want to soft-search.

```
  # any biome with cherry in the name
  - events: [ "BIOME=cherry" ]
    songs:
      - "Storm"
```

<br><br>

Biome tags can be used by specifying `BIOMETAG=BIOME_TAG`. They're good for automatically having compatibility with modded biomes. You can find the full list of tags [here](https://maven.fabricmc.net/docs/fabric-api-0.100.3+1.21/net/fabricmc/fabric/api/tag/convention/v2/ConventionalBiomeTags.html)

NOTE: putting the "IS_" in front of the tag is optional ("BIOMETAG=IS_MOUNTAIN" and "BIOMETAG=MOUNTAIN" are both valid

```
  - events: [ "DAY", "BIOMETAG=IS_HOT" ]
    songs:
      - "ForTheKing"
```

<br><br>


## Dimension Events

Dimensions can be specified by doing `DIM=dimname`. Similar to biome events, this can be the fully typed name or just a subset of it.

```
  # we add forceStopMusicOnChanged here so that the song always changes upon going in/out of the nether, since that music is pretty different than anything else
  - events: [ "DIM=NETHER" ]
    forceStopMusicOnChanged: true
    songs:
      - "Storm"
```

<br><br>


## Block Events

In version 1.2.0 and above, you can detect nearby blocks in a square 25 block radius.

Use `/reactivemusic logBlockCounter` to print the nearby block counts to get an idea of what numbers to use.

Fortress detection example:
```
# 1000 nether bricks and 2 brick fences within 25 blocks
[ "DIM=NETHER", "BLOCK=nether_bricks,1000", BLOCK=nether_brick_fence,2" ]
```


<br><br>


# Mod Configuration

## Making Sounds Mute Music

In-game, do `/reactivemusic toggleSoundEventLogging` to start printing the nearby sound events happening into chat.

In the ReactiveMusic.json5 config, you can edit the `soundsMuteMusic` field with the names/partial names of any sound events that you want to mute RM when they play. This requires a game restart to be re-loaded.

Example:
```
  # Mute RM when near a beacon block
	soundsMuteMusic: [
		"block.beacon.ambient"
	],
```

<br><br>

