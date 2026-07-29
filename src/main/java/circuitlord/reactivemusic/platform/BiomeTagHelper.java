package circuitlord.reactivemusic.platform;

//? if >=1.20 {
/*import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
*///?} else {
import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.Registry;
//?}
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

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
        //? if >=1.21 {
        /*return TagKey.of(RegistryKeys.BIOME, Identifier.of(getTagNamespace(), remapped));
        *///?} else if >=1.20 {
        /*return TagKey.of(RegistryKeys.BIOME, new Identifier(getTagNamespace(), remapped));
        *///?} else {
        return TagKey.of(Registry.BIOME_KEY, new Identifier(getTagNamespace(), remapped));
        //?}
    }
}
