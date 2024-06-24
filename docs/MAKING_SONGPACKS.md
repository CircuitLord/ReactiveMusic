
# Making Songpacks

Songpacks are constructed as folders with a yaml configuration file and a folder with mp3 music files.

They're loaded from the `resourcepacks` folder, although they're not actually resource packs and are instead selected from the configuration UI. This is purely to make including songpacks easier.

I HIGHLY recommend downloading the songpack template and using it as a base to make your pack so you understand the structure.
Download [here](https://raw.githubusercontent.com/CircuitLord/ReactiveMusic/master/docs/ReactiveMusicSongpackTemplate.zip)!

IMPORTANT: Songpacks can be worked on while unzipped, but when distributing a zipped songpack, it MUST be zipped as uncompressed, (store files). If it is compressed it will not load correctly.



## YAML Configuration

`ReactiveMusic.yaml` is where all the configuration for your songpack takes place. Let's go over all the properties available in a songpack config.

### Global configuration:
```
name: "My Awesome Songpack"
version: "1.0"
author: "CircuitLord"
description: "A really good songpack that I made"
credits: "Cool people here"
```

- `(String) name` The unique name/identifier for your songpack.
- `(String) version` Current version of the songpack.
- `(String) author` Hey, it's you!
- `(String) description` Details on the songpack.
- `(String) credits` Any credits for music creators included in your pack.


### Songpack Entries:

Songpack entries define what songs play in specific events. You can have as many entries as you want, and they're prioritized internally based on what order you put them in.

Here's an example songpack entry:

```
- events: [ "MAIN_MENU" ]
  alwaysPlay: true
  alwaysStop: true
  songs:
    - "DuNock-Street-TitleEdit"
```

- `(SongpackEventType) events` The specific events that need to be valid for this entry to play. (see details below)
- `(Boolean) alwaysStop` (default false) Does this event always stop when it's events are no longer valid?
- `(Boolean) alwaysPlay` (default false) Does this event always immediately start when it's events become valid?
- `(String[]) songs` The list of song files to pick from when this event plays. These are picked from the `music` sub-folder.

You can also combine multiple events for more specific songs.

```
  - events: [ "DAY", "MOUNTAIN" ]
    songs:
      - "ForTheKing"
      - "Freedom"
```


## Events (SongpackEventTypes)

This lists all the available songpack events you have available.

### Special
- `MAIN_MENU`
- `CREDITS` (TODO)
- `HOME` (TODO)

### Time
- `DAY`
- `NIGHT`
- `SUNRISE`
- `SUNSET`

### Weather
- `RAIN`
- `SNOW`

### Dimension
- `NETHER`
- `END`
- `OVERWORLD`

### World Height
- `UNDERWATER`
- `UNDERGROUND`
- `DEEP_UNDERGROUND`
- `HIGH_UP`

### Entities
- `MINECART`
- `BOAT`
- `HORSE`
- `PIG`

### Actions
- `FISHING`
- `DYING` (TODO)


### Biomes
- `FOREST`
- `MOUNTAIN`
- `DESERT` (TODO)
- `BEACH`

