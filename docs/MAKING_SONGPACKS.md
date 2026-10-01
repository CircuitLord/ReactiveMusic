
# Making Songpacks

Songpacks are constructed as folders with a yaml configuration file and a folder with mp3 music files.

They're loaded from the `resourcepacks` folder, although they're not actually resource packs and are instead selected from the configuration UI. This is purely to make including songpacks easier.

There's no bundled default songpack — the mod stays silent until you add one and select it from `/reactivemusic`.

**Download the Songpack Template [here](https://raw.githubusercontent.com/CircuitLord/ReactiveMusic/master/docs/ReactiveMusicSongpackTemplate-v6.zip)!**

You can find a video version of this wiki page [here](https://www.youtube.com/watch?v=YvEap8IUS-c)!
> Highly recommended to use VSCode with the YAML extension as shown in the video, or else you will have issues making properly formatted YAML files!


## Testing Songpacks

Open the songpack menu using `/reactivemusic`. Click on the button for your songpack name and it'll start playing.
This also works for reloading a songpack if you've made changes you want to try without restarting your game by clicking another songpack and back onto yours.

I also highly recommend turning debug mode on in the Debug section,
this will make songs switch whenever their events become valid and removes silence gaps entirely to make it easier to test.

Debug mode also makes the mod log what it's doing to the console/log file (song switches, biome/structure changes, etc.) — no separate logging commands needed, and it doesn't spam chat.

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
- events: [ "MENU" ]
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

**Some tags always force a quick fade-out once they become valid**, regardless
of `forceStopMusicOnValid`/`forceChance` or the songpack's `musicSwitchSpeed` —
these are all "something just happened, react now" moments: any combat tag
(`COMBAT`, `LOW`, `CRIT`, `WARDEN`, `NEAR_WARDEN`, `DRAGON`, `NEAR_DRAGON`,
`WITHER`, `NEAR_WITHER`, `GUARDIAN`, `RAID`), `END_PORTAL_LIT`, `MENU`,
`CREDITS`, `RAIN`, and `STORM`. This is automatic — you don't need to set
anything on the entry yourself, just use one of these tags anywhere in its
`events`.

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

You can combine tags using three operators:

- **AND** — separate array entries: `[ "DAY", "BIOME=MOUNTAIN" ]` requires both.
- **OR** — `||` within one entry: `[ "BIOME=ocean || SWIM" ]` requires either.
- **NOT** — a leading `!` on a tag: `[ "!NETHER" ]` requires it to *not* be true.

These combine freely, including on the shorthand tags below:

```
  - events: [ "DAY", "BIOME=MOUNTAIN" ]
    songs:
      - "ForTheKing"
      - "Freedom"

  - events: [ "BIOME=ocean || SWIM" ]
    songs:
      - "Freedom"

  - events: [ "!NETHER", "SWIM || VILLAGE" ]
    songs:
      - "WhereIBelong"
```

<br><br>

## Events (tags)

This lists all the available songpack tags you have available. Some are plain
flags; others (marked *shorthand*) expand into a combination of the more
specific tags/conditions further down this page (biome, dimension, entity,
block) — using the shorthand or spelling it out yourself works identically.

### Special
- `MENU`
- `CREDITS`
- `HOME` (within 45 blocks of bed)
- `END_PORTAL_LIT` — the moment an end portal is completed (last eye of ender
  placed), **anywhere in the world**, not just near you. Detected client-side
  off the same global sound cue the game itself plays for it (comparable to
  how the wither's spawn sound works) — no server-side support needed. This
  is a **one-shot pulse**: it reads true for a single tick right when it
  happens, then goes back to false, rather than staying true like every other
  tag here. Pair it with `forceStartMusicOnValid`/`forceStopMusicOnChanged`
  if you want a one-off musical sting rather than a sustained event.

### Dimension
- `OVERWORLD`, `NETHER`, `END`
- `DIM=dimname` — see [Dimension Events](#dimension-events) below

### Time
- `DAY` — true for everything that isn't `NIGHT`, which includes `SUNRISE`
  and `SUNSET` (they're not their own separate slice of the day — they're
  twilight windows inside `DAY`'s range). `DAY || SUNRISE` or
  `DAY || SUNSET` is therefore redundant with plain `DAY`.
- `NIGHT`
- `SUNRISE`
- `SUNSET`

### Weather
- `RAIN` — also true while it's snowing (vanilla only has one weather-active
  flag; snow is just how it renders in cold biomes, there's no separate
  "is snowing" state to check)
- `STORM`
- `CLEAR` — no rain or storm

### Actions
- `FISH` — fishing rod cast out
- `RIDE` — riding a saddled mount that's moving
- `RAIL` — riding a minecart that's moving
- `SAIL` — riding a boat that's moving
- `GLIDE` — flying with an elytra
- `SWIM` — underwater or swimming
- `AFK` — hasn't moved in the last minute

### Location
- `VILLAGE` (3+ nearby villagers — that's about where an actual village starts, rather than a stray villager or two)
- `ROOF` — *shorthand* for Nether, above y=128
- `CLOUDS` — *shorthand* for Overworld, above y=320
- `PEAKS` — *shorthand* for Overworld, above y=220
- `LEVEL` — *shorthand* for Overworld, above sea level
- `UNDER` — *shorthand* for Overworld, sky not visible
- `DEEP` — *shorthand* for Overworld, below y=0

`UNDER` checks for an actual solid block above you, not sky *light* — so
swimming in open ocean doesn't count as `UNDER` even though water attenuates
light (which would otherwise make it indistinguishable from a flooded cave).
A genuine cave roof, even one that's flooded, still counts.

**`UNDER` is restricted by default** — an entry that doesn't mention `UNDER`
anywhere (positively or with `!`) implicitly gets `!UNDER` added to it, so it
won't play while the player is underground unless you explicitly opt in with
`UNDER`. This only affects the Overworld (`UNDER` is always false elsewhere),
so Nether/End entries are unaffected. Add `UNDER` (or `!UNDER`, which is the
same as doing nothing) yourself to override this.

### Combat
- `BOSS` (whenever a boss bar is on-screen)
- `COMBAT` (recently damaged, or damaged by, a hostile mob or another player — not incidental damage like TNT, falling blocks, or thrown potions)
- `LOW` — health below 10hp
- `CRIT` — health below 4hp
- `WARDEN` — *shorthand* for `ATTACKER=warden`
- `NEAR_WARDEN` — *shorthand* for `ENTITY=warden,40`
- `DRAGON` — *shorthand* for `ATTACKER=ender_dragon`
- `NEAR_DRAGON` — *shorthand* for `ENTITY=ender_dragon,120` OR the dragon's boss bar is visible
- `WITHER` — *shorthand* for `ATTACKER=wither`
- `NEAR_WITHER` — *shorthand* for `ENTITY=wither,60` OR the wither's boss bar is visible
- `GUARDIAN` — *shorthand* for `ATTACKER=elder_guardian`
- `RAID` — a raid's boss bar is visible

Boss identification (`NEAR_DRAGON`/`NEAR_WITHER`/`RAID`'s boss bar checks) works
by the boss bar's translation key (`entity.minecraft.ender_dragon`,
`entity.minecraft.wither`, `event.minecraft.raid`) — the same identity signal
resource packs use to reskin boss bars, so it's locale-independent and specific
to the exact boss. It falls back to the boss bar's color (dragon = pink,
wither = purple, raid = red) only when there's no translation key to read,
which happens for a custom-named wither (a renamed wither's boss bar shows the
custom name instead). The dragon and raid boss bars can't be renamed, so they
always match by key.

### Other
- `GENERIC` (always true — matches everything, useful as a fallback entry)

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

Biome tags can be used by specifying `TAG=BIOME_TAG`. They're good for automatically having compatibility with modded biomes. You can find the full list of tags [here](https://maven.fabricmc.net/docs/fabric-api-0.100.3+1.21/net/fabricmc/fabric/api/tag/convention/v2/ConventionalBiomeTags.html)

NOTE: putting the "IS_" in front of the tag is optional ("TAG=IS_MOUNTAIN" and "TAG=MOUNTAIN" are both valid)

You can use either the field names from that list or the tag's actual id — they're
not always the same. `TAG=IS_COLD_OVERWORLD` (field name) and `TAG=IS_COLD/OVERWORLD`
(the real tag, `c:is_cold/overworld`) both work, as do `TAG=IS_VEGETATION_DENSE` and
`TAG=IS_DENSE_VEGETATION`. Tags from other mods/datapacks that aren't in that list are
matched by their id.

```
  - events: [ "DAY", "TAG=IS_HOT" ]
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


## Entity Events

You can detect specific nearby entity types (not just a generic "mobs nearby" flag) by using `ENTITY=entityname`, optionally with a radius in blocks: `ENTITY=entityname,radius`. The default radius is 20 blocks. Entities are only scanned up to 64 blocks horizontally and 40 blocks vertically, so radius values are effectively capped there. This can be the full entity id (e.g. `minecraft:warden`) or just part of it, similar to biome/dimension matching.

```
  # play boss music whenever a warden is within 40 blocks, regardless of combat state
  - events: [ "ENTITY=warden,40" ]
    forceStopMusicOnChanged: true
    songs:
      - "TheDeepDark"
```

You can also detect the specific entity that's currently attacking (or being attacked by) the player using `ATTACKER=entityname`. This is more specific than the generic `COMBAT` event above, and lets you have different music for different enemies.

```
  - events: [ "ATTACKER=zombie" ]
    songs:
      - "LastStand"

  - events: [ "ATTACKER=warden" ]
    forceStopMusicOnChanged: true
    songs:
      - "RunForYourLife"
```

Attacker detection sticks around for a few seconds after the last hit so the music doesn't cut in and out between swings.

<br><br>




## Structure Events

You can detect what structure the player is standing in with `STRUCT=structurename`, similar to biome/dimension matching (full id or a partial/soft-search match).

```
  - events: [ "STRUCT=stronghold" ]
    forceStopMusicOnChanged: true
    songs:
      - "EndPortalRoom"
```

There's also `STRUCTTAG=` for structure tags, same matching rules.

Shorthand tags are available for the common vanilla structures:

- `STRONGHOLD`, `FORTRESS`, `BASTION`, `MANSION`, `MONUMENT`, `MINESHAFT`, `ANCIENT_CITY`, `END_CITY`, `TRIAL`, `PYRAMID`, `TEMPLE`, `OUTPOST`, `SHIPWRECK`

**This requires the mod installed on the server too** (not just the client) — structure data isn't something the client can reliably know on its own, so it's synced from the server. If the server doesn't have the mod, `STRUCT=`/`STRUCTTAG=`/the shorthand tags above will simply never match, same as any other condition whose data isn't available — the rest of your songpack keeps working normally. You'll get one console error (not a crash, not a chat message) a few seconds after joining a server that doesn't have the mod at all, so it's clear why those tags aren't doing anything rather than leaving you guessing. A server admin disabling the gamerule (see below) doesn't log that error — the mod still confirms it's present right on join either way, so that case is distinguishable and intentional, not an error.

Server admins can disable structure syncing with `/gamerule reactivemusicStructureSync false`. It defaults to on. This is a real vanilla gamerule, not a separate config file, so it shows up in `/gamerule` like any other and can be set in a server's `server.properties`/datapack/command block setup like usual.

With debug mode on (see [Testing Songpacks](#testing-songpacks)), the mod logs structure enter/leave changes to the console — this works without any songpack loaded or selected, so it's the fastest way to confirm the server round-trip is actually working before you build `STRUCT=` conditions around it.

<br><br>


# Mod Configuration

## Making Sounds Mute Music

You'll need the sound event's path (e.g. `block.beacon.ambient`) to match against — check a resource pack's `sounds.json` or the Minecraft Wiki if you're not sure of the exact name.

In the ReactiveMusic.json5 config, you can edit the `soundsMuteMusic` field with the names/partial names of any sound events that you want to mute RM when they play. This respects positional sound distance. This requires a game restart to be re-loaded.

If a mod incorrectly tags non-positional music as positional, use `soundsMuteMusicIgnoreDistance` instead.

Example:
```
  # Mute RM when near a beacon block
	soundsMuteMusic: [
		"block.beacon.ambient",
		"othermod.xy"
	],

  # Mute RM whenever this sound plays, even if the sound position is wrong
	soundsMuteMusicIgnoreDistance: [
		"boss.theme",
		"partial-event-name"
	],
```

<br><br>

