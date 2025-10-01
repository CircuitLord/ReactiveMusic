package circuitlord.reactivemusic.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import circuitlord.reactivemusic.ReactiveMusicState;

/**
 * One-file, plugin-facing utils for ReactiveMusic.
 * - Server-safe (no direct client class references in signatures)
 * - Client helpers are provided via a delegate you set from client code
 * - Minimal allocations; safe for per-tick use
 */
public final class ReactiveMusicUtils {

    private ReactiveMusicUtils() {}

    /* =========================================================
       XXX================  SONG SELECTION  ====================
       ========================================================= */  

    /**
     * TODO:
     * Song selection could be it's own class, with more features,
     * but for now this is sufficient.
     * 
     * Song selection as a class would allow things like multiple
     * independent recent-song lists (e.g., per-plugin),
     * or more advanced strategies (e.g., weighted random).
     */
    
    private static final Random rand = new Random();

    public static boolean hasSongNotPlayedRecently(List<String> songs) {
        for (String song : songs) {
            if (!ReactiveMusicState.recentlyPickedSongs.contains(song)) {
                return true;
            }
        }
        return false;
    }


    public static List<String> getNotRecentlyPlayedSongs(String[] songs) {
        List<String> notRecentlyPlayed = new ArrayList<>(Arrays.asList(songs));
        notRecentlyPlayed.removeAll(ReactiveMusicState.recentlyPickedSongs);
        return notRecentlyPlayed;
    }


    public static String pickRandomSong(List<String> songs) {

        if (songs.isEmpty()) {
            return null;
        }

        List<String> cleanedSongs = new ArrayList<>(songs);

        cleanedSongs.removeAll(ReactiveMusicState.recentlyPickedSongs);


        String picked;

        // If there's remaining songs, pick one of those
        if (!cleanedSongs.isEmpty()) {
            int randomIndex = rand.nextInt(cleanedSongs.size());
            picked = cleanedSongs.get(randomIndex);
        }

        // Else we've played all these recently so just pick a new random one
        else {
            int randomIndex = rand.nextInt(songs.size());
            picked = songs.get(randomIndex);
        }

        // only track the past X songs
        if (ReactiveMusicState.recentlyPickedSongs.size() >= 8) {
            ReactiveMusicState.recentlyPickedSongs.remove(0);
        }
        ReactiveMusicState.recentlyPickedSongs.add(picked);

        return picked;
    }


    public static String getSongName(String song) {
        return song == null ? "" : song.replaceAll("([^A-Z])([A-Z])", "$1 $2");
    }

    /* =========================================================
       XXX================  WORLD HELPERS  =====================
       ========================================================= */

    public static Box boxAround(Entity e, float radiusXZ, float radiusY) {
        double x = e.getX(), y = e.getY(), z = e.getZ();
        return new Box(
            x - radiusXZ, y - radiusY, z - radiusXZ,
            x + radiusXZ, y + radiusY, z + radiusXZ
        );
    }

    public static boolean isHighUp(BlockPos pos, int minY) { return pos.getY() >= minY; }
    public static boolean isUnderground(World w, BlockPos pos, int maxY) { return pos.getY() <= maxY && !w.isSkyVisible(pos); }
    public static boolean isDeepUnderground(World w, BlockPos pos, int maxY){ return pos.getY() <= maxY && !w.isSkyVisible(pos); }

    public static boolean isRainingAt(World w, BlockPos pos) { return w.isRaining() && w.getBiome(pos).value().getPrecipitation(pos) == Biome.Precipitation.RAIN; }
    public static boolean isSnowingAt(World w, BlockPos pos) { return w.isRaining() && w.getBiome(pos).value().getPrecipitation(pos) == Biome.Precipitation.SNOW; }
    public static boolean isStorm(World w) { return w.isThundering(); }

    /* =========================================================
       XXX============  SPHERICAL ENTITY QUERIES  ==============
       ========================================================= */

    /**
     * Broad-phase AABB + narrow-phase squared-distance sphere check.
     * Returns all entities of the given type within a true sphere around the player.
     * Uses Entity#squaredDistanceTo to avoid a sqrt() call, keeping it fast.
     *
     * @param type        entity class to search for (e.g., HostileEntity.class)
     * @param player      center of the sphere
     * @param radius      sphere radius in blocks
     * @param extraFilter optional additional filter (may be null)
     */
    public static <T extends Entity> List<T> getEntitiesInSphere(
            Class<T> type, PlayerEntity player, double radius, Predicate<? super T> extraFilter) {

        final double r2 = radius * radius;

        // Broad-phase: chunk-efficient AABB around the player
        Box box = player.getBoundingBox().expand(radius, radius, radius);

        // Narrow-phase: exact spherical test using squared distance (no sqrt)
        return player.getWorld().getEntitiesByClass(
                type,
                box,
                e -> e.isAlive()
                    && e.squaredDistanceTo(player) <= r2
                    && (extraFilter == null || extraFilter.test(e))
        );
    }

    /** Convenience: true if any entity of type is within the spherical radius. */
    public static <T extends Entity> boolean anyInSphere(
            Class<T> type, PlayerEntity player, double radius, Predicate<? super T> extraFilter) {
        final double r2 = radius * radius;
        Box box = player.getBoundingBox().expand(radius, radius, radius);
        // Use early-exit variant to avoid allocating a list
        return !player.getWorld().getEntitiesByClass(
                type,
                box,
                e -> e.isAlive()
                    && e.squaredDistanceTo(player) <= r2
                    && (extraFilter == null || extraFilter.test(e))
        ).isEmpty();
    }

    /** Ring/band query: entities between inner and outer radii (inclusive). */
    public static <T extends Entity> List<T> getEntitiesInSphericalBand(
            Class<T> type, PlayerEntity player, double innerRadius, double outerRadius,
            Predicate<? super T> extraFilter) {

        if (innerRadius < 0) innerRadius = 0;
        if (outerRadius < innerRadius) outerRadius = innerRadius;

        final double rMin2 = innerRadius * innerRadius;
        final double rMax2 = outerRadius * outerRadius;
        Box box = player.getBoundingBox().expand(outerRadius, outerRadius, outerRadius);

        return player.getWorld().getEntitiesByClass(
                type,
                box,
                e -> {
                    if (!e.isAlive()) return false;
                    double d2 = e.squaredDistanceTo(player);
                    return d2 >= rMin2
                        && d2 <= rMax2
                        && (extraFilter == null || extraFilter.test(e));
                }
        );
    }
}
