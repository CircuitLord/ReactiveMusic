package rocamocha.reactivemusic.plugins;

import rocamocha.reactivemusic.ReactiveMusicDebug;
import rocamocha.reactivemusic.api.*;
import rocamocha.reactivemusic.api.eventsys.EventRecord;
// TODO: find a way to remove these leaks
// This one seems complicated...
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;

import java.lang.reflect.Field;
import java.util.*;

public final class BiomeTagPlugin extends ReactiveMusicPlugin {
    
    // Moved from SongPicker - now owned by this plugin
    private static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();
    private static final String[] CONVENTIONAL_BIOME_TAGS_CANDIDATES = {
        "net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags",
        "net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags"
    };
    private static final Class<?> CONVENTIONAL_BIOME_TAGS_CLASS = resolveConventionalBiomeTagsClass();
    private static final Field[] BIOME_TAG_FIELDS = CONVENTIONAL_BIOME_TAGS_CLASS != null ? CONVENTIONAL_BIOME_TAGS_CLASS.getDeclaredFields() : new Field[0];
    private static final List<TagKey<Biome>> BIOME_TAGS = new ArrayList<>();

    // Public accessor for SongPicker.isEntryValid()
    public static Map<TagKey<Biome>, Boolean> getBiomeTagEventMap() {
        return biomeTagEventMap;
    }

    public BiomeTagPlugin() {
        super("reactivemusic", "biome_tag");
    }

    private static Class<?> resolveConventionalBiomeTagsClass() {
        for (String candidate : CONVENTIONAL_BIOME_TAGS_CANDIDATES) {
            try {
                return Class.forName(candidate);
            } catch (ClassNotFoundException ignored) {
            }
        }
        ReactiveMusicDebug.LOGGER.warn("Could not locate Fabric ConventionalBiomeTags class on the classpath.");
        return null;
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

    @Override 
    public void init() {
        // Moved initialization logic from SongPicker static block
        for (Field field : BIOME_TAG_FIELDS) {
            TagKey<Biome> biomeTag = getBiomeTagFromField(field);

            if (biomeTag == null) {
                continue;
            }

            BIOME_TAGS.add(biomeTag);
            biomeTagEventMap.put(biomeTag, false);
        }
        ReactiveMusicDebug.log(ReactiveMusicDebug.LogCategory.PLUGIN_EXECUTION, 
            "BiomeTagPlugin initialized with " + BIOME_TAGS.size() + " biome tags");
    }

    @Override public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> out) {
        if (player == null || world == null) return;

        PlayerEntity mcplayer = MinecraftClient.getInstance().player;
        World mcworld = MinecraftClient.getInstance().world;

        BlockPos pos = mcplayer.getBlockPos();
        RegistryEntry<Biome> biome = mcworld.getBiome(pos);

        // Collect current tags once
        List<TagKey<Biome>> currentTags = biome.streamTags().toList();

        // Mirror SongPicker’s original per-tick loop: compare by tag.id() identity
        for (TagKey<Biome> tag : BIOME_TAGS) {
            boolean found = false;
            for (TagKey<Biome> cur : currentTags) {
                if (cur.id() == tag.id()) { // keep the same non-Fabric-safe identity check
                    found = true;
                    break;
                }
            }
            biomeTagEventMap.put(tag, found);
        }
    }

    // Public accessors for external use (e.g., RMRuntimeEntry)
    public static Field[] getBiomeTagFields() {
        return BIOME_TAG_FIELDS;
    }
}