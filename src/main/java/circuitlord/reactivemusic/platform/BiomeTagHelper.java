package circuitlord.reactivemusic.platform;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

import java.util.ServiceLoader;

public interface BiomeTagHelper {
    BiomeTagHelper INSTANCE = ServiceLoader.load(BiomeTagHelper.class).findFirst()
        .orElseThrow(() -> new RuntimeException("No BiomeTagHelper implementation found! Is a loader module missing?"));

    /**
     * Returns the tag namespace for the current platform.
     * Fabric/NeoForge: "c", Forge: "forge"
     */
    String getTagNamespace();

    /**
     * Remaps a canonical tag path to the platform-specific equivalent.
     * Returns null if the tag doesn't exist on this platform.
     */
    default String remapTagPath(String path) {
        return path;
    }

    /**
     * Creates a TagKey<Biome> from a canonical tag path string.
     * Returns null if the tag doesn't exist on this platform.
     */
    default TagKey<Biome> createBiomeTagKey(String canonicalPath) {
        String remapped = remapTagPath(canonicalPath);
        if (remapped == null) return null;
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(getTagNamespace(), remapped));
    }
}
