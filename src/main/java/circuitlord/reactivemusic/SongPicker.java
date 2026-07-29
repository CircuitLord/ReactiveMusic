package circuitlord.reactivemusic;


import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import circuitlord.reactivemusic.mixin.BossBarHudAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
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

//? if >=1.20 {
/*import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
*///?} else {
import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.Registry;
//?}
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.*;

public final class SongPicker {



    public static Map<SongpackEventType, Boolean> songpackEventMap = new EnumMap<>(SongpackEventType.class);

    public static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();

    public static Map<Entity, Long> recentEntityDamageSources = new HashMap<>();


    private static final Set<String> BLOCK_COUNTER_BLACKLIST = Set.of(); // = Set.of("ore", "debris");
    private static final Map<Block, String> BLOCK_ID_CACHE = new IdentityHashMap<>();

    public static boolean queuedToPrintBlockCounter = false;
    public static BlockPos cachedBlockCounterOrigin;
    public static int currentBlockCounterX = 99999;
    public static int currentBlockCounterY = 99999;

    public static Object2IntOpenHashMap<String> blockCounterMap = new Object2IntOpenHashMap<>();
    public static Object2IntOpenHashMap<String> cachedBlockChecker = new Object2IntOpenHashMap<>();

    public static String currentBiomeName = "";
    public static String currentDimName = "";


    private static final Random rand = new Random();

    private static List<String> recentlyPickedSongs = new ArrayList<>();

    public static Long TIME_FOR_FORGET_DAMAGE_SOURCE = 200L;

    public static boolean wasSleeping = false;

    public static void registerBiomeTag(TagKey<Biome> tag) {
        if (!biomeTagEventMap.containsKey(tag)) {
            biomeTagEventMap.put(tag, false);
        }
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
        BlockPos playerPos = new BlockPos(player.getBlockPos());
        var biome = world.getBiome(playerPos);

        // Copied logic out from getIdAsString
        currentBiomeName = (String)biome.getKey().map((key) -> {
            return key.getValue().toString();
        }).orElse("[unregistered]");

        boolean underground = !world.isSkyVisible(playerPos);
        var indimension = world.getRegistryKey();

        currentDimName = indimension.getValue().toString();

        Entity riding = VersionHelper.GetRidingEntity(player);

        long time = world.getTimeOfDay() % 24000;
        boolean night = time >= 13000 && time < 23000;
        boolean sunset = time >= 12000 && time < 13000;
        boolean sunrise = time >= 23000;


        // TODO: someone help me I have no idea how to get the name of the world/server but if you know how then put it instead of "saved"
        if (!wasSleeping && player.isSleeping()) {
            //? if >=1.21.9 {
            /*ReactiveMusic.config.savedHomePositions.put("saved", player.getEntityPos());
            *///?} else {
            ReactiveMusic.config.savedHomePositions.put("saved", player.getPos());
            //?}

            ModConfig.saveConfig();
        }

        wasSleeping = player.isSleeping();


        // special

        if (ReactiveMusic.config.savedHomePositions.containsKey("saved")) {

            //? if >=1.21.9 {
            /*Vec3d dist = player.getEntityPos().subtract(ReactiveMusic.config.savedHomePositions.get("saved"));
            *///?} else {
            Vec3d dist = player.getPos().subtract(ReactiveMusic.config.savedHomePositions.get("saved"));
            //?}

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


        songpackEventMap.put(SongpackEventType.UNDERGROUND, indimension == World.OVERWORLD && underground && playerPos.getY() < 55);
        songpackEventMap.put(SongpackEventType.DEEP_UNDERGROUND, indimension == World.OVERWORLD && underground && playerPos.getY() < 15);
        songpackEventMap.put(SongpackEventType.HIGH_UP, indimension == World.OVERWORLD && !underground && playerPos.getY() > 128);

        songpackEventMap.put(SongpackEventType.UNDERWATER, player.isSubmergedInWater());

        // Weather
        //? if >=1.21.9 {
        /*songpackEventMap.put(SongpackEventType.RAIN, world.isRaining() && biome.value().getPrecipitation(playerPos, world.getSeaLevel()) == Biome.Precipitation.RAIN);
        songpackEventMap.put(SongpackEventType.SNOW, world.isRaining() && biome.value().getPrecipitation(playerPos, world.getSeaLevel()) == Biome.Precipitation.SNOW);
        *///?} else if >=1.20 {
        /*songpackEventMap.put(SongpackEventType.RAIN, world.isRaining() && biome.value().getPrecipitation(playerPos) == Biome.Precipitation.RAIN);
        songpackEventMap.put(SongpackEventType.SNOW, world.isRaining() && biome.value().getPrecipitation(playerPos) == Biome.Precipitation.SNOW);
        *///?} else {
        songpackEventMap.put(SongpackEventType.RAIN, world.isRaining() && biome.value().getPrecipitation() == Biome.Precipitation.RAIN);
        songpackEventMap.put(SongpackEventType.SNOW, world.isRaining() && biome.value().getPrecipitation() == Biome.Precipitation.SNOW);
        //?}

        songpackEventMap.put(SongpackEventType.STORM, world.isThundering());


        var currentTags = biome.streamTags().toList();

        // Update all registered biome tags
        for (TagKey<Biome> tag : biomeTagEventMap.keySet()) {
            boolean found = false;

            for (TagKey<Biome> curTag : currentTags) {
                if (curTag.id().equals(tag.id())) {
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



    public static void tickBlockCounterMap() {

        int RADIUS = 25;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        World world = mc.world;
        if (player == null || world == null)
            return;



        // Advance y, then x
/*        currentBlockCounterY++;
        if (currentBlockCounterY > RADIUS) {
            currentBlockCounterY = -RADIUS;

            currentBlockCounterX++;

            ReactiveMusic.LOGGER.info("blockchecker X:" + currentBlockCounterX);
            if (currentBlockCounterX > RADIUS) {
                currentBlockCounterX = -RADIUS;

               // ReactiveMusic.LOGGER.info("blockchecker X:" + currentBlockCounterX);
            }
        }*/

        // just X
        currentBlockCounterX++;
        if (currentBlockCounterX > RADIUS) {
            currentBlockCounterX = -RADIUS;
        }

        // finished iterating, reset
        if (currentBlockCounterX == -RADIUS/* && currentBlockCounterY == -RADIUS*/) {
            // ReactiveMusic.LOGGER.info("Finished checking for blocks, resetting! Total: " + blockCounterMap.size());

            if (queuedToPrintBlockCounter) {

                player.sendMessage(Text.of("[ReactiveMusic]: Logging Block Counter map! Radius: " + RADIUS), false);

                blockCounterMap.object2IntEntrySet().stream()
                        .sorted((first, second) -> Integer.compare(second.getIntValue(), first.getIntValue()))
                        .forEach(entry -> player.sendMessage(Text.of(entry.getKey() + ": " + entry.getIntValue()), false));

                queuedToPrintBlockCounter = false;

            }

            Object2IntOpenHashMap<String> completedScan = cachedBlockChecker;
            cachedBlockChecker = blockCounterMap;
            blockCounterMap = completedScan;
            blockCounterMap.clear();
            cachedBlockCounterOrigin = player.getBlockPos();

        }

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int y = -RADIUS; y <= RADIUS; y++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {

                // don't allocate new blockpos everytime
                mutablePos.set(
                        cachedBlockCounterOrigin.getX() + currentBlockCounterX,
                        cachedBlockCounterOrigin.getY() + y,
                        cachedBlockCounterOrigin.getZ() + z
                );


                Block block = world.getBlockState(mutablePos).getBlock();
                String key = getBlockId(block);

                boolean isBlacklisted = false;
                for (String black : BLOCK_COUNTER_BLACKLIST) {
                    if (key.contains(black)) {
                        isBlacklisted = true;
                        break;
                    }
                }
                if (isBlacklisted)
                    continue;

                blockCounterMap.addTo(key, 1);

            }
        }


    }



    private static String getBlockId(Block block) {
        String id = BLOCK_ID_CACHE.get(block);
        if (id != null) return id;

        //? if >=1.20 {
        /*id = Registries.BLOCK.getId(block).toString();
        *///?} else {
        id = Registry.BLOCK.getId(block).toString();
        //?}
        BLOCK_ID_CACHE.put(block, id);
        return id;
    }


    private static Box GetBoxAroundPlayer(ClientPlayerEntity player, float radiusXZ, float radiusY) {

        return new Box(player.getX() - radiusXZ, player.getY() - radiusY, player.getZ() - radiusXZ,
                player.getX() + radiusXZ, player.getY() + radiusY, player.getZ() + radiusXZ);

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

        for (var condition : entry.conditions) {

            // each condition functions as an OR, if at least one of them is true then the condition is true


            boolean songpackEventsValid = false;

            for (SongpackEventType songpackEvent : condition.songpackEvents) {
                if (songpackEventMap.containsKey(songpackEvent) && songpackEventMap.get(songpackEvent)) {
                    songpackEventsValid = true;
                    break;
                }
            }

            boolean blocksValid = false;
            for (var blockCond : condition.blocks) {
                for (var kvp : cachedBlockChecker.object2IntEntrySet()) {
                    if (kvp.getKey().contains(blockCond.block) && kvp.getIntValue() >= blockCond.requiredCount) {
                        blocksValid = true;
                        break;
                    }
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


            if (!songpackEventsValid && !biomeTypesValid && !biomeTagsValid && !dimsValid && !blocksValid) {
                // none of the OR conditions were valid on this condition, return false
                return false;
            }

        }

        // we passed without failing so it must be true
        return true;
        
    }









}
