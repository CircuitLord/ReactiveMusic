package circuitlord.reactivemusic;


import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;

//import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.lang.reflect.Field;
import java.util.*;

public final class SongPicker {


	public static Map<SongpackEventType, Boolean> songpackEventMap = new EnumMap<>(SongpackEventType.class);

	public static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();


	private static final Random rand = new Random();

	private static List<String> recentlyPickedSongs = new ArrayList<>();

	public static final Field[] BIOME_TAG_FIELDS = ConventionalBiomeTags.class.getDeclaredFields();
	public static final List<TagKey<Biome>> BIOME_TAGS = new ArrayList<>();

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


		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		World world = mc.world;


		songpackEventMap.put(SongpackEventType.MAIN_MENU, player == null || world == null);
		songpackEventMap.put(SongpackEventType.CREDITS, mc.currentScreen instanceof CreditsScreen);

		// Early out if not in-game
		if (player == null || world == null) return;

		// World processing
		BlockPos pos = new BlockPos(player.getBlockPos());
		var biome = world.getBiome(pos);

		boolean underground = !world.isSkyVisible(pos);
		var indimension = world.getRegistryKey();

		Entity riding = VersionHelper.GetRidingEntity(player);

		long time = world.getTimeOfDay() % 24000;
		boolean night = time > 13300 && time < 23200;
		boolean sunset = time > 12000 && time < 13000;
		boolean sunrise = time > 23000;

		// Time
		songpackEventMap.put(SongpackEventType.DAY, !night);
		songpackEventMap.put(SongpackEventType.NIGHT, night);
		songpackEventMap.put(SongpackEventType.SUNSET, sunset);
		songpackEventMap.put(SongpackEventType.SUNRISE, sunrise);


		// Actions

		songpackEventMap.put(SongpackEventType.DYING, player.getHealth() < 7);
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
		songpackEventMap.put(SongpackEventType.RAIN, world.isRaining());


		// TODO: WILL BE REMOVED, use biomeTagEventMap
		songpackEventMap.put(SongpackEventType.MOUNTAIN, biome.isIn(BiomeTags.IS_MOUNTAIN));
		songpackEventMap.put(SongpackEventType.FOREST, biome.isIn(BiomeTags.IS_FOREST));
		songpackEventMap.put(SongpackEventType.BEACH, biome.isIn(BiomeTags.IS_BEACH));


		// Update all ConventionalBiomeTags
		for (TagKey<Biome> tag : BIOME_TAGS) {
			biomeTagEventMap.put(tag, biome.isIn(tag));
		}

		songpackEventMap.put(SongpackEventType.GENERIC, true);
	}



	public static void initialize() {

		songpackEventMap.clear();

		for (SongpackEventType eventType : SongpackEventType.values()) {
			songpackEventMap.put(eventType, false);
		}

	}



	public static SongpackEntry getCurrentEntry() {


		for (int i = 0; i < SongLoader.activeSongpack.entries.length; i++) {

			SongpackEntry entry = SongLoader.activeSongpack.entries[i];
			if (entry == null) continue;

			boolean eventsMet = true;

			for (SongpackEventType songpackEvent : entry.songpackEvents) {

				if (!songpackEventMap.containsKey(songpackEvent)) continue;

				if (!songpackEventMap.get(songpackEvent)) {
					eventsMet = false;
					break;
				}
			}

			for (TagKey<Biome> biomeTagEvent : entry.biomeTagEvents) {

				if (!biomeTagEventMap.containsKey(biomeTagEvent)) continue;

				if (!biomeTagEventMap.get(biomeTagEvent)) {
					eventsMet = false;
					break;
				}

			}

			if (eventsMet) {
				return entry;
			}

		}

		// Failed
		return null;
	}









	static String pickRandomSong(String[] songArr) {

		List<String> songs = new ArrayList<>(Arrays.stream(songArr).toList());
		songs.removeAll(recentlyPickedSongs);


		String picked;

		// If there's remaining songs, pick one of those
		if (!songs.isEmpty()) {
			int randomIndex = rand.nextInt(songs.size());
			picked = songs.get(randomIndex);
		}

		// Else we've played all these recently so just pick a new random one
		else {
			int randomIndex = rand.nextInt(songArr.length);
			picked = songArr[randomIndex];
		}


		// only track the past X songs
		if (recentlyPickedSongs.size() > 5) {
			recentlyPickedSongs.remove(0);
		}

		recentlyPickedSongs.add(picked);


		return picked;
	}


	public static String getSongName(String song) {
		return song == null ? "" : song.replaceAll("([^A-Z])([A-Z])", "$1 $2");
	}
}
