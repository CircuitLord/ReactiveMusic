package circuitlord.reactivemusic;


import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import circuitlord.reactivemusic.entries.predicate.RMPredicate;
import circuitlord.reactivemusic.mixin.BossBarHudAccessor;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.EquipmentSlot;

import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.*;

public final class SongPicker {



    public static Map<SongpackEventType, Boolean> songpackEventMap = new EnumMap<>(SongpackEventType.class);

    public static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();

    public static Map<Entity, Long> recentEntityDamageSources = new HashMap<>();

    public static List<NearbyEntityInfo> cachedNearbyEntities = new ArrayList<>();
    private static final Map<EntityType<?>, String> ENTITY_TYPE_ID_CACHE = new IdentityHashMap<>();

    // how far out we scan for nearby-entity/attacker conditions; per-condition radius is clamped to this
    private static final double NEARBY_ENTITY_SCAN_RADIUS_XZ = 64.0;
    private static final double NEARBY_ENTITY_SCAN_RADIUS_Y = 40.0;

    public static class NearbyEntityInfo {
        public String typeId;
        public double distance;
    }


    public static String currentBiomeName = "";
    public static String currentDimName = "";

    public static double currentPlayerY = 0;
    public static float currentHealth = 20f;
    public static boolean currentSkyHidden = false;
    public static int currentSeaLevel = 63;

    public static boolean currentDragonBossBar = false;
    public static boolean currentWitherBossBar = false;
    public static boolean currentRaidBossBar = false;

    // populated by the server-side structure sync payload (see fabric/RMStructureSyncServer) -
    // stays empty if the server doesn't run the mod or has the sync gamerule disabled
    public static Set<String> currentStructureIds = Set.of();
    public static Set<String> currentStructureTagIds = Set.of();

    // set by SoundManagerMixin the instant the global "end portal lit" sound plays; consumed (and
    // cleared) by the very next tickEventMap() call, so END_PORTAL_LIT reads true for exactly one
    // tick - it's a global one-shot event, not per-player sustained state like everything else here
    public static boolean queuedEndPortalLitPulse = false;

    private static String lastLoggedBiomeName = null;
    private static Set<String> previousActiveTags = Set.of();
    private static Set<String> previousActiveBossBars = Set.of();

    private static Vec3 lastMovedPos = null;
    private static long lastMovedTick = 0;
    private static final double AFK_MOVE_EPSILON_SQ = 0.0025; // ~0.05 blocks/tick
    private static final long AFK_TICKS = 1200; // 1 minute @ 20tps


    private static final Random rand = new Random();

    private static List<String> recentlyPickedSongs = new ArrayList<>();

    // longer than vanilla's own recent-damage-source memory, so attacker= conditions sustain through gaps between swings
    public static Long TIME_FOR_FORGET_DAMAGE_SOURCE = 300L;

    public static boolean wasSleeping = false;

    public static void registerBiomeTag(TagKey<Biome> tag) {
        if (!biomeTagEventMap.containsKey(tag)) {
            biomeTagEventMap.put(tag, false);
        }
    }

    public static void onStructureSyncReceived(Set<String> structureIds, Set<String> structureTagIds) {
        if (ReactiveMusic.config.debugModeEnabled) {
            logSetChange("Structures", currentStructureIds, structureIds);
        }

        currentStructureIds = structureIds;
        currentStructureTagIds = structureTagIds;
    }

    private static void logSetChange(String label, Set<String> oldValues, Set<String> newValues) {
        Set<String> entered = new HashSet<>(newValues);
        entered.removeAll(oldValues);

        Set<String> left = new HashSet<>(oldValues);
        left.removeAll(newValues);

        if (!entered.isEmpty()) ReactiveMusic.LOGGER.info("[ReactiveMusic]: " + label + " entered: " + String.join(", ", entered));
        if (!left.isEmpty()) ReactiveMusic.LOGGER.info("[ReactiveMusic]: " + label + " left: " + String.join(", ", left));
    }




    public static void tickEventMap() {

        currentBiomeName = "";
        currentDimName = "";

        Minecraft mc = Minecraft.getInstance();
        if (mc == null)
            return;

        LocalPlayer player = mc.player;
        Level world = mc.level;


        songpackEventMap.put(SongpackEventType.MENU, player == null || world == null);
        songpackEventMap.put(SongpackEventType.CREDITS, mc.screen instanceof WinScreen);

        songpackEventMap.put(SongpackEventType.END_PORTAL_LIT, queuedEndPortalLitPulse);
        queuedEndPortalLitPulse = false;

        // Early out if not in-game
        if (player == null || world == null) return;

        // World processing
        BlockPos playerPos = player.blockPosition();
        var biome = world.getBiome(playerPos);

        // Copied logic out from getIdAsString
        currentBiomeName = (String) biome.unwrapKey().map((key) -> {
            return key.identifier().toString();
        }).orElse("[unregistered]");

        if (ReactiveMusic.config.debugModeEnabled && !currentBiomeName.equals(lastLoggedBiomeName)) {
            if (lastLoggedBiomeName != null) {
                ReactiveMusic.LOGGER.info("[ReactiveMusic]: Biome changed: " + lastLoggedBiomeName + " -> " + currentBiomeName);
            }
            lastLoggedBiomeName = currentBiomeName;
        }

        // NOT world.canSeeSky() here deliberately - that's a light-level check (sky light >= 15),
        // and water attenuates skylight, so it reads "no sky" while swimming in open ocean at any
        // real depth, indistinguishable from a flooded cave. This scans for an actual solid block
        // above instead, ignoring fluids, so open ocean (sky, then water, then you) correctly
        // counts as sky visible while a flooded cave (solid roof above) still doesn't.
        boolean underground = hasSolidRoofAbove(world, playerPos);
        var indimension = world.dimension();

        currentDimName = indimension.identifier().toString();

        Entity riding = VersionHelper.GetRidingEntity(player);

        long time = world.getDefaultClockTime() % 24000;
        boolean night = time >= 13000 && time < 23000;
        boolean sunset = time >= 12000 && time < 13000;
        boolean sunrise = time >= 23000;


        // TODO: someone help me I have no idea how to get the name of the world/server but if you know how then put it instead of "saved"
        if (!wasSleeping && player.isSleeping()) {
            ReactiveMusic.config.savedHomePositions.put("saved", player.position());

            ModConfig.saveConfig();
        }

        wasSleeping = player.isSleeping();


        // special

        if (ReactiveMusic.config.savedHomePositions.containsKey("saved")) {

            Vec3 dist = player.position().subtract(ReactiveMusic.config.savedHomePositions.get("saved"));

            songpackEventMap.put(SongpackEventType.HOME, dist.length() < 45.0f);
        }
        else {
            songpackEventMap.put(SongpackEventType.HOME, false);
        }



        // Time
        songpackEventMap.put(SongpackEventType.DAY, !night);
        songpackEventMap.put(SongpackEventType.NIGHT, night);
        songpackEventMap.put(SongpackEventType.SUNSET, sunset);
        songpackEventMap.put(SongpackEventType.SUNRISE, sunrise);


        // Live state used by height/health tags (RMTagRegistry)
        currentPlayerY = playerPos.getY();
        currentHealth = player.getHealth();
        currentSkyHidden = underground;

        // Actions

        songpackEventMap.put(SongpackEventType.FISH, player.fishing != null);

        boolean ridingIsMoving = riding != null && isMoving(riding);
        songpackEventMap.put(SongpackEventType.RAIL, riding instanceof Minecart && ridingIsMoving);
        songpackEventMap.put(SongpackEventType.SAIL, riding instanceof Boat && ridingIsMoving);
        songpackEventMap.put(SongpackEventType.RIDE, riding instanceof LivingEntity livingRiding
                && !livingRiding.getItemBySlot(EquipmentSlot.SADDLE).isEmpty() && ridingIsMoving);

        songpackEventMap.put(SongpackEventType.GLIDE, player.isFallFlying());
        songpackEventMap.put(SongpackEventType.SWIM, player.isUnderWater() || player.isSwimming());

        Vec3 currentPos = player.position();
        if (lastMovedPos == null || currentPos.distanceToSqr(lastMovedPos) > AFK_MOVE_EPSILON_SQ) {
            lastMovedPos = currentPos;
            lastMovedTick = world.getGameTime();
        }
        songpackEventMap.put(SongpackEventType.AFK, world.getGameTime() - lastMovedTick > AFK_TICKS);


        songpackEventMap.put(SongpackEventType.OVERWORLD, indimension == Level.OVERWORLD);
        songpackEventMap.put(SongpackEventType.NETHER, indimension == Level.NETHER);
        songpackEventMap.put(SongpackEventType.END, indimension == Level.END);

        // Weather
        currentSeaLevel = world.getSeaLevel();
        // vanilla only has one weather-active flag; snow is just how it renders in cold biomes,
        // there's no separate "isSnowing" state, so RAIN covers both
        songpackEventMap.put(SongpackEventType.RAIN, world.isRaining());

        songpackEventMap.put(SongpackEventType.STORM, world.isThundering());


        var currentTags = biome.tags().toList();

        // Update all registered biome tags
        for (TagKey<Biome> tag : biomeTagEventMap.keySet()) {
            boolean found = false;

            for (TagKey<Biome> curTag : currentTags) {
                if (curTag.location().equals(tag.location())) {
                    found = true;
                    break;
                }
            }

            biomeTagEventMap.put(tag, found);
        }


        // process recent damage sources

        // remove past sources
        recentEntityDamageSources.entrySet().removeIf(entry -> entry.getKey() == null || !entry.getKey().isAlive() || world.getGameTime() - entry.getValue() > TIME_FOR_FORGET_DAMAGE_SOURCE);

        // add new damage sources - getEntity() (not getDirectEntity()) so a ranged attack
        // (skeleton's arrow, etc.) attributes to the actual attacker, not the projectile
        var recentDamage = player.getLastDamageSource();
        var attacker = recentDamage == null ? null : recentDamage.getEntity();

        // only hostile mobs and other players count as "combat" - excludes incidental
        // entity-sourced damage like TNT, falling blocks, or thrown potions
        if (attacker instanceof Enemy || attacker instanceof Player) {
            recentEntityDamageSources.put(attacker, world.getGameTime());
        }

        songpackEventMap.put(SongpackEventType.COMBAT, !recentEntityDamageSources.isEmpty());


        // Scan for nearby entities by type, for entity=/attacker= conditions
        {
            cachedNearbyEntities.clear();

            AABB entityScanBox = new AABB(
                    player.getX() - NEARBY_ENTITY_SCAN_RADIUS_XZ, player.getY() - NEARBY_ENTITY_SCAN_RADIUS_Y, player.getZ() - NEARBY_ENTITY_SCAN_RADIUS_XZ,
                    player.getX() + NEARBY_ENTITY_SCAN_RADIUS_XZ, player.getY() + NEARBY_ENTITY_SCAN_RADIUS_Y, player.getZ() + NEARBY_ENTITY_SCAN_RADIUS_XZ);

            List<LivingEntity> nearbyLiving = world.getEntitiesOfClass(LivingEntity.class, entityScanBox,
                    entity -> entity != null && entity != player);

            for (LivingEntity living : nearbyLiving) {
                NearbyEntityInfo info = new NearbyEntityInfo();
                info.typeId = getEntityTypeId(living.getType());
                info.distance = player.distanceTo(living);

                cachedNearbyEntities.add(info);
            }
        }


        // Search for nearby entities that could be relevant to music

        {
            int villagerCount = 0;

            double radiusXZ = 30.0;
            double radiusY = 15.0;

            AABB box = new AABB(player.getX() - radiusXZ, player.getY() - radiusY, player.getZ() - radiusXZ,
                    player.getX() + radiusXZ, player.getY() + radiusY, player.getZ() + radiusXZ);

            List<Villager> nearbyVillagerCheck = world.getEntitiesOfClass(Villager.class, box, entity -> entity != null);

            for (Villager villagerEntity : nearbyVillagerCheck) {
                villagerCount++;
            }

            songpackEventMap.put(SongpackEventType.VILLAGE, villagerCount >= 3);

        }


        // try to get boss bars
        boolean bossBarActive = false;
        currentDragonBossBar = false;
        currentWitherBossBar = false;
        currentRaidBossBar = false;

        if (mc.gui != null && mc.gui.getBossOverlay() != null) {
            try {

                var bossBars = ((BossBarHudAccessor) mc.gui.getBossOverlay()).getBossBars();

                if (!bossBars.isEmpty()) {
                    bossBarActive = true;
                }

                // Identify by the boss bar's translation key first - locale-independent and, unlike
                // color, specific to the exact boss (verified against the real 26.1.2 jar: dragon and
                // raid always use "entity.minecraft.ender_dragon"/"event.minecraft.raid", fixed, never
                // overridable). Falls back to the boss bar's color (vanilla hardcodes one per boss
                // type) only when there's no translation key - e.g. a custom-named wither, since a
                // renamed wither's boss bar shows the custom name instead of its translation key.
                for (var bossBar : bossBars.values()) {
                    String key = bossBar.getName().getContents() instanceof TranslatableContents translatable
                            ? translatable.getKey() : null;

                    if (key != null) {
                        if (key.equals("entity.minecraft.ender_dragon")) currentDragonBossBar = true;
                        else if (key.equals("entity.minecraft.wither")) currentWitherBossBar = true;
                        else if (key.equals("event.minecraft.raid")) currentRaidBossBar = true;
                    } else {
                        BossEvent.BossBarColor color = bossBar.getColor();
                        if (color == BossEvent.BossBarColor.PINK) currentDragonBossBar = true;
                        else if (color == BossEvent.BossBarColor.PURPLE) currentWitherBossBar = true;
                        else if (color == BossEvent.BossBarColor.RED) currentRaidBossBar = true;
                    }
                }
            } catch (Exception e) {
            }
        }


        songpackEventMap.put(SongpackEventType.BOSS, bossBarActive);


        songpackEventMap.put(SongpackEventType.GENERIC, true);

        if (ReactiveMusic.config.debugModeEnabled) {
            Set<String> activeTags = new TreeSet<>();
            for (var mapEntry : songpackEventMap.entrySet()) {
                if (mapEntry.getKey() != SongpackEventType.GENERIC && Boolean.TRUE.equals(mapEntry.getValue())) {
                    activeTags.add(mapEntry.getKey().name());
                }
            }
            logSetChange("Tags", previousActiveTags, activeTags);
            previousActiveTags = activeTags;

            Set<String> activeBossBars = new TreeSet<>();
            if (currentDragonBossBar) activeBossBars.add("dragon");
            if (currentWitherBossBar) activeBossBars.add("wither");
            if (currentRaidBossBar) activeBossBars.add("raid");
            logSetChange("Boss bars", previousActiveBossBars, activeBossBars);
            previousActiveBossBars = activeBossBars;
        }
    }



    private static boolean isMoving(Entity entity) {
        Vec3 delta = entity.getDeltaMovement();
        return (delta.x * delta.x + delta.z * delta.z) > 0.0025; // ~0.05 blocks/tick horizontally
    }

    // scans straight up from pos, ignoring air and fluids, for an actual solid block - unlike
    // canSeeSky() (a light-level check), this doesn't count water itself as a roof
    private static boolean hasSolidRoofAbove(Level world, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = pos.getY() + 1; y <= world.getMaxY(); y++) {
            cursor.set(pos.getX(), y, pos.getZ());

            var state = world.getBlockState(cursor);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;

            return true;
        }
        return false;
    }


    public static String getEntityTypeId(EntityType<?> type) {
        String id = ENTITY_TYPE_ID_CACHE.get(type);
        if (id != null) return id;

        id = EntityType.getKey(type).toString();
        ENTITY_TYPE_ID_CACHE.put(type, id);
        return id;
    }


    public static void initialize() {

        songpackEventMap.clear();
        biomeTagEventMap.clear();

        for (SongpackEventType eventType : SongpackEventType.values()) {
            songpackEventMap.put(eventType, false);
        }
    }







    static boolean hasSongNotPlayedRecently(List<String> songs) {
        for (String song : songs) {
            if (!recentlyPickedSongs.contains(song)) {
                return true;
            }
        }
        return false;
    }


    static List<String> getNotRecentlyPlayedSongs(String[] songs) {
        List<String> notRecentlyPlayed = new ArrayList<>(Arrays.asList(songs));
        notRecentlyPlayed.removeAll(recentlyPickedSongs);
        return notRecentlyPlayed;
    }


    static String pickRandomSong(List<String> songs) {

        if (songs.isEmpty()) {
            return null;
        }

        List<String> cleanedSongs = new ArrayList<>(songs);

        cleanedSongs.removeAll(recentlyPickedSongs);


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
        if (recentlyPickedSongs.size() >= 8) {
            recentlyPickedSongs.remove(0);
        }

        recentlyPickedSongs.add(picked);


        return picked;
    }


    public static String getSongName(String song) {
        return song == null ? "" : song.replaceAll("([^A-Z])([A-Z])", "$1 $2");
    }

    
    public static boolean isEntryValid(RMRuntimeEntry entry) {
        // each condition is AND'd; NOT/OR are already baked into the predicate tree at parse time
        return entry.conditions.stream().allMatch(RMPredicate::test);
    }









}
