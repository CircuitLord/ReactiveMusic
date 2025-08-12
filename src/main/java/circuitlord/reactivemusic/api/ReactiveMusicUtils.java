package circuitlord.reactivemusic.api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import circuitlord.reactivemusic.ReactiveMusicState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * One-file, plugin-facing utils for ReactiveMusic.
 * - Server-safe (no direct client class references in signatures)
 * - Client helpers are provided via a delegate you set from client code
 * - Minimal allocations; safe for per-tick use
 */
public final class ReactiveMusicUtils {

    private ReactiveMusicUtils() {}

    /* =========================================================
       XXX============  ENV & CLIENT BRIDGE  ===================
       ========================================================= */

    /** True if running in a client environment. */
    public static boolean isClientEnv() {
        return FabricLoader.getInstance().getEnvironmentType().name().equals("CLIENT");
    }

    /**
     * Client-only delegate. Set this from your CLIENT initializer (or mixin)
     * so plugins can ask for client data without linking client classes here.
     * 
     * TODO: The client delegate is currently unimplemented for the most part.
     * Its purpose is to make development for server-side bridging plugins easier in the future.
     * This way, Reactive Music can stay client-side only, while any multiplayer functionality
     * or server based operation can be implemented by a plugin, encouraging a less restricted
     * ecosystem of plugins for the base mod.  
     *
     * <p>Example (client init):
     * <pre>
     * ReactiveMusicReactiveMusicUtils.setClientDelegate(new ReactiveMusicReactiveMusicUtils.ClientDelegate() {
     *   public boolean isMainMenu() { return MinecraftClient.getInstance().player == null; }
     *   public boolean isCredits()  { return MinecraftClient.getInstance().currentScreen instanceof CreditsScreen; }
     * });
     * </pre>
     * </p>
     */
    public interface ClientDelegate {
        boolean isMainMenu();
        boolean isCredits();
        boolean isBossBarActive();
    }

    private static volatile ClientDelegate clientDelegate;

    /** Install client delegate from client code. No-op on server. */
    public static void setClientDelegate(ClientDelegate delegate) {
        clientDelegate = delegate;
    }

    public static boolean isMainMenu(){ ClientDelegate d = clientDelegate; return d != null && d.isMainMenu(); }
    public static boolean isCredits(){ ClientDelegate d = clientDelegate; return d != null && d.isCredits(); }
    public static boolean isBossBarActive(){ ClientDelegate d = clientDelegate; return d != null && d.isBossBarActive(); }

    /* =========================================================
       XXX================  SONG SELECTION  ====================
       ========================================================= */  

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
       XXX==============  DAMAGE TRACKING  =====================
       ========================================================= */

    /**
     * Lightweight "recent damage" tracker keyed by entity UUID.
     * Call recordDamage(...) when your event/mixin sees damage,
     * then query wasDamagedRecently(...) in ticks.
     */
    public static final class Damage {
        // last damage tick per entity
        private static final Map<java.util.UUID, Long> LAST_DAMAGE_TICK = new ConcurrentHashMap<>();

        /** Record that an entity took damage at worldTime. */
        public static void recordDamage(LivingEntity entity, long worldTime) {
            if (entity != null) LAST_DAMAGE_TICK.put(entity.getUuid(), worldTime);
        }

        /** True if entity was damaged within windowTicks prior to current worldTime. */
        public static boolean wasDamagedRecently(LivingEntity entity, long worldTime, long windowTicks) {
            if (entity == null) return false;
            Long t = LAST_DAMAGE_TICK.get(entity.getUuid());
            return t != null && (worldTime - t) <= windowTicks;
        }

        /** Optional cleanup to prevent unbounded growth. */
        public static void purgeOlderThan(long worldTime, long windowTicks) {
            long cutoff = worldTime - windowTicks;
            LAST_DAMAGE_TICK.entrySet().removeIf(e -> e.getValue() < cutoff);
        }
    }

    /* =========================================================
       XXX===========  ENTITY / VEHICLE HELPERS  ===============
       ========================================================= */

    /** Root vehicle (handles seat entities); returns null if player is not mounted on another entity. */
    public static Entity rootVehicleOrNull(PlayerEntity player) {
        if (player == null) return null;
        Entity root = player.getRootVehicle();
        return (root == null || root == player) ? null : root;
    }

    /** True if the entity type's registry namespace equals the given modid. */
    public static boolean isFromNamespace(Entity e, String namespace) {
        if (e == null || namespace == null) return false;
        Identifier id = e.getType().getRegistryEntry().registryKey().getValue();
        return id != null && namespace.equals(id.getNamespace());
    }

    /** True if registry path contains the token (normalized). */
    public static boolean idPathContains(Entity e, String tokenLower) {
        if (e == null || tokenLower == null) return false;
        Identifier id = e.getType().getRegistryEntry().registryKey().getValue();
        return id != null && id.getPath().toLowerCase(Locale.ROOT).contains(tokenLower);
    }

    /* =========================================================
       XXX============  SPHERICAL ENTITY QUERIES  ==============
       ========================================================= */

    /**
     * Broad-phase AABB + narrow-phase squared-distance sphere check.
     * Returns all entities of the given type within a true sphere around the player.
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


    /* =========================================================
       XXX==================  GUARDS  ==========================
       ========================================================= */

    /** Run a supplier and swallow exceptions; useful around plugin hooks. */
    public static boolean safe(BooleanSupplier s) {
        try { return s.getAsBoolean(); } catch (Throwable t) { return false; }
    }

    /** Null-safe equals. */
    public static boolean eq(Object a, Object b) { return Objects.equals(a, b); }
}
