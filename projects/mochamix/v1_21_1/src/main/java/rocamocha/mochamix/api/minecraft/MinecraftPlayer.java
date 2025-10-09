package rocamocha.mochamix.api.minecraft;

import java.util.List;
import java.util.UUID;

import rocamocha.mochamix.impl.NativeAccess;
import rocamocha.mochamix.api.minecraft.MinecraftEntity.MinecraftLivingEntity;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;

/**
 * A view of a player in Minecraft, providing access to various properties and states.
 * Implementations should surface their information through specialized adapters
 * so API consumers get clean autocomplete groupings for each concern.
 *
 * @see PlayerSocket
 */
public interface MinecraftPlayer extends NativeAccess {

    // composite views
    Hunger hunger();
    Experience experience();
    Gamemode gamemode();
    Location location();
    Motion motion();
    Activity activity();
    Surroundings surroundings();
    Mount mount();
    Inventory inventory();

    // identity
    UUID uuid();
    String name();
    String team();

    // status
    float health();
    float maxHealth();

    interface Hunger {
        int foodLevel();
        float saturation();
        boolean isNotFull();
    }

    interface Experience {
        int level();
        float progress();
        int total();
    }

    interface Gamemode {
        boolean isSurvival();
        boolean isCreative();
        boolean isSpectator();
        boolean isAdventure();
    }

    interface Motion {
        boolean sneaking();
        boolean sprinting();
        boolean crawling();
        boolean swimming();
        boolean gliding();
    }

    interface Activity {
        boolean sleeping();
        boolean fishing();
    }

    interface Surroundings {
        boolean underwater();
        boolean inRain();
        boolean inPowderedSnow();

        List<MinecraftEntity> nearbyEntities(double radius);
        List<MinecraftLivingEntity> nearbyLivingEntities(double radius);
        List<MinecraftLivingEntity> nearbyHostileEntities(double radius);
    }

    interface Mount {
        boolean riding();
        MinecraftEntity vehicle();
    }

    interface Location {
        MinecraftVector3 pos();
        MinecraftVector3 blockPos();
        MinecraftWorld world();
        String dimension();
        String biome();
    }

    interface Inventory {
        MinecraftItemStack mainhand();
        MinecraftItemStack offhand();
        MinecraftItemStack slot(int slot);
    }

    // convenience delegates for direct access
    default boolean sleeping() { return activity().sleeping(); }
    default boolean fishing() { return activity().fishing(); }
    default boolean sneaking() { return motion().sneaking(); }
    default boolean sprinting() { return motion().sprinting(); }
    default boolean crawling() { return motion().crawling(); }
    default boolean swimming() { return motion().swimming(); }
    default boolean gliding() { return motion().gliding(); }
    default boolean underwater() { return surroundings().underwater(); }
    default boolean riding() { return mount().riding(); }
    default MinecraftEntity vehicle() { return mount().vehicle(); }
}
