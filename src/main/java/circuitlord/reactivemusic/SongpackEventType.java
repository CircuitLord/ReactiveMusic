package circuitlord.reactivemusic;

public enum SongpackEventType {

    NONE,

    MAIN_MENU,
    CREDITS,


    HOME,


    // --- TIME ---
    DAY,
    NIGHT,
    SUNRISE,
    SUNSET,

    // --- Weather ---
    RAIN,
    SNOW,


    // Dimension

    // DEPRECATED -- use BiomeTagMap in SongPicker which pulls from ConventionalBiomeTags
    NETHER,
    END,
    OVERWORLD,

    // --- world height ---
    UNDERWATER,
    UNDERGROUND,
    DEEP_UNDERGROUND,
    HIGH_UP,

    // --- Entities ---
    MINECART,
    BOAT,
    HORSE,
    PIG,

    //Actions
    FISHING,
    DYING,


    // Biomes
    // DEPRECATED TO BE REMOVED
    FOREST,
    MOUNTAIN,
    DESERT,
    BEACH,


    // MOBS

    BOSS,
    VILLAGE,
    HOSTILE_MOBS,


    GENERIC

}

