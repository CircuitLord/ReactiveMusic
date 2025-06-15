package circuitlord.reactivemusic;


import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.mixin.BossBarHudAccessor;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;

//import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.lang.reflect.Field;
import java.util.*;

public final class SongPicker {


    public static Map<SongpackEventType, Boolean> songpackEventMap = new EnumMap<>(SongpackEventType.class);

    public static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();

    public static Map<Entity, Long> recentEntityDamageSources = new HashMap<>();

    public static String currentBiomeName = "";
    public static String currentDimName = "";


    private static final Random rand = new Random();

    private static List<String> recentlyPickedSongs = new ArrayList<>();

    public static final Field[] BIOME_TAG_FIELDS = ConventionalBiomeTags.class.getDeclaredFields();
    public static final List<TagKey<Biome>> BIOME_TAGS = new ArrayList<>();

    public static Long TIME_FOR_FORGET_DAMAGE_SOURCE = 200L;

    public static boolean wasSleeping = false;

    static {

        for (Field field : BIOME_TAG_FIELDS) {
            TagKey<Biome> biomeTag = getBiomeTagFromField(field);

            BIOME_TAGS.add(biomeTag);
            biomeTagEventMap.put(biomeTag, false);
        }
    }

    public static TagKey<Biome> getBiomeTagFromField(Field field) {
        if (field.getType() == TagKey.class) {
            try {
                @SuppressWarnings("unchecked")
                TagKey<Biome> tag = (TagKey<Biome>) field.get(null);
                return tag;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }




    public static void tickEventMap() {

        currentBiomeName = "";
        currentDimName = "";

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null)
            return;

        ClientPlayerEntity player = mc.player;
        World world = mc.world;


        songpackEventMap.put(SongpackEventType.MAIN_MENU, player == null || world == null);
        songpackEventMap.put(SongpackEventType.CREDITS, mc.currentScreen instanceof CreditsScreen);

        // Early out if not in-game
        if (player == null || world == null) return;

        // World processing
        BlockPos pos = new BlockPos(player.getBlockPos());
        var biome = world.getBiome(pos);

        // Copied logic out from getIdAsString
        currentBiomeName = (String)biome.getKey().map((key) -> {
            return key.getValue().toString();
        }).orElse("[unregistered]");

        boolean underground = !world.isSkyVisible(pos);
        var indimension = world.getRegistryKey();

        currentDimName = indimension.getValue().toString();

        Entity riding = VersionHelper.GetRidingEntity(player);

        long time = world.getTimeOfDay() % 24000;
        boolean night = time > 13300 && time < 23200;
        boolean sunset = time > 12000 && time < 13000;
        boolean sunrise = time > 23000;


        // TODO: someone help me I have no idea how to get the name of the world/server but if you know how then put it instead of "saved"
        if (!wasSleeping && player.isSleeping()) {
            ReactiveMusic.config.savedHomePositions.put("saved", player.getPos());

            ModConfig.saveConfig();
        }

        wasSleeping = player.isSleeping();


        // special

        if (ReactiveMusic.config.savedHomePositions.containsKey("saved")) {

            Vec3d dist = player.getPos().subtract(ReactiveMusic.config.savedHomePositions.get("saved"));

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


        // Actions

        songpackEventMap.put(SongpackEventType.DYING, player.getHealth() / player.getMaxHealth() < 0.35);
        songpackEventMap.put(SongpackEventType.FISHING, player.fishHook != null);

        songpackEventMap.put(SongpackEventType.MINECART, riding instanceof MinecartEntity);
        songpackEventMap.put(SongpackEventType.BOAT, riding instanceof BoatEntity);
        songpackEventMap.put(SongpackEventType.HORSE, riding instanceof HorseEntity);
        songpackEventMap.put(SongpackEventType.PIG, riding instanceof PigEntity);


        songpackEventMap.put(SongpackEventType.OVERWORLD, indimension == World.OVERWORLD);
        songpackEventMap.put(SongpackEventType.NETHER, indimension == World.NETHER);
        songpackEventMap.put(SongpackEventType.END, indimension == World.END);


        songpackEventMap.put(SongpackEventType.UNDERGROUND, indimension == World.OVERWORLD && underground && pos.getY() < 55);
        songpackEventMap.put(SongpackEventType.DEEP_UNDERGROUND, indimension == World.OVERWORLD && underground && pos.getY() < 15);
        songpackEventMap.put(SongpackEventType.HIGH_UP, indimension == World.OVERWORLD && !underground && pos.getY() > 128);

        songpackEventMap.put(SongpackEventType.UNDERWATER, player.isSubmergedInWater());

        // Weather
        songpackEventMap.put(SongpackEventType.RAIN, world.isRaining() && biome.value().getPrecipitation(pos) == Biome.Precipitation.RAIN);
        songpackEventMap.put(SongpackEventType.SNOW, world.isRaining() && biome.value().getPrecipitation(pos) == Biome.Precipitation.SNOW);

        songpackEventMap.put(SongpackEventType.STORM, world.isThundering());


        var currentTags = biome.streamTags().toList();

        // Update all ConventionalBiomeTags
        for (TagKey<Biome> tag : BIOME_TAGS) {
            boolean found = false;

            // search by ID instead of comparing tagkey, doesn't work on non-fabric
            for (TagKey<Biome> curTag : currentTags) {
                if (curTag.id() == tag.id()) {
                    found = true;
                    break;
                }
            }

            biomeTagEventMap.put(tag, found);
        }


        // process recent damage sources

        // remove past sources
        recentEntityDamageSources.entrySet().removeIf(entry -> entry.getKey() == null || !entry.getKey().isAlive() || world.getTime() - entry.getValue() > TIME_FOR_FORGET_DAMAGE_SOURCE);

        // add new damage sources
        var recentDamage = player.getRecentDamageSource();

        if (recentDamage != null && recentDamage.getSource() != null) {
            recentEntityDamageSources.put(recentDamage.getSource(), world.getTime());
        }




        // Search for nearby entities that could be relevant to music

        {
            int villagerCount = 0;

            double radiusXZ = 30.0;
            double radiusY = 15.0;

            Box box = new Box(player.getX() - radiusXZ, player.getY() - radiusY, player.getZ() - radiusXZ,
                    player.getX() + radiusXZ, player.getY() + radiusY, player.getZ() + radiusXZ);

            List<VillagerEntity> nearbyVillagerCheck = world.getEntitiesByClass(VillagerEntity.class, box, entity -> entity != null);

            for (VillagerEntity villagerEntity : nearbyVillagerCheck) {
                villagerCount++;
            }

            songpackEventMap.put(SongpackEventType.VILLAGE, villagerCount > 0);

        }

        {
            List<HostileEntity> nearbyHostile = world.getEntitiesByClass(HostileEntity.class,
                    GetBoxAroundPlayer(player, 12.f, 6.f),
                    entity -> entity != null);

            songpackEventMap.put(SongpackEventType.NEARBY_MOBS, nearbyHostile.size() >= 1);

        }


        //songpackEventMap.put(SongpackEventType.HOSTILE_MOBS, aggroMobsCount >= 4);

        //System.out.println("Villager count: " + villagerCount + ", Aggro mobs count: " + aggroMobsCount);


        // try to get boss bars
        boolean bossBarActive = false;

        if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
            try {

                var bossBars = ((BossBarHudAccessor) mc.inGameHud.getBossBarHud()).getBossBars();

                if (!bossBars.isEmpty()) {
                    bossBarActive = true;
                }
            } catch (Exception e) {
            }
        }

        songpackEventMap.put(SongpackEventType.BOSS, bossBarActive);


        songpackEventMap.put(SongpackEventType.GENERIC, true);
    }


    private static Box GetBoxAroundPlayer(ClientPlayerEntity player, float radiusXZ, float radiusY) {

        return new Box(player.getX() - radiusXZ, player.getY() - radiusY, player.getZ() - radiusXZ,
                player.getX() + radiusXZ, player.getY() + radiusY, player.getZ() + radiusXZ);

    }


    public static void initialize() {

        songpackEventMap.clear();

        for (SongpackEventType eventType : SongpackEventType.values()) {
            songpackEventMap.put(eventType, false);
        }
    }




    private static final List<SongpackEntry> reusableValidEntries = new ArrayList<>();


/*    public static List<SongpackEntry> getAllValidEntries() {

        reusableValidEntries.clear();

        for (int i = 0; i < SongLoader.activeSongpack.entries.length; i++) {

            SongpackEntry entry = SongLoader.activeSongpack.entries[i];
            if (entry == null) continue;

            boolean eventsMet = true;

            for (SongpackEventType songpackEvent : entry.songpackEvents) {

                if (!songpackEventMap.containsKey(songpackEvent))
                    continue;

                if (!songpackEventMap.get(songpackEvent)) {
                    eventsMet = false;
                    break;
                }
            }

            for (TagKey<Biome> biomeTagEvent : entry.biomeTagEvents) {

                if (!biomeTagEventMap.containsKey(biomeTagEvent))
                    continue;

                if (!biomeTagEventMap.get(biomeTagEvent)) {
                    eventsMet = false;
                    break;
                }
            }

            if (eventsMet) {
                reusableValidEntries.add(entry);
            }
        }

        return reusableValidEntries;
    }*/


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

        for (var condition : entry.conditions) {

            // each condition functions as an OR, if at least one of them is true then the condition is true


            boolean songpackEventsValid = false;

            for (SongpackEventType songpackEvent : condition.songpackEvents) {
                if (songpackEventMap.containsKey(songpackEvent) && songpackEventMap.get(songpackEvent)) {
                    songpackEventsValid = true;
                    break;
                }
            }

            boolean biomeTypesValid = false;
            for (var biome : condition.biomeTypes) {
                if (currentBiomeName.contains(biome)) {
                    biomeTypesValid = true;
                    break;
                }
            }

            boolean biomeTagsValid = false;
            for (var biomeTag : condition.biomeTags) {
                if (biomeTagEventMap.containsKey(biomeTag) && biomeTagEventMap.get(biomeTag)) {
                    biomeTagsValid = true;
                    break;
                }
            }

            boolean dimsValid = false;
            for (var dim : condition.dimTypes) {
                if (currentDimName.contains(dim)) {
                    dimsValid = true;
                    break;
                }
            }


            if (!songpackEventsValid && !biomeTypesValid && !biomeTagsValid && !dimsValid) {
                // none of the OR conditions were valid on this condition, return false
                return false;
            }

        }

        // we passed without failing so it must be true
        return true;
        
    }









}
