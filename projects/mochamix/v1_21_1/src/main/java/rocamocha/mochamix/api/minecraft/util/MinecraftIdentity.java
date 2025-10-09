package rocamocha.mochamix.api.minecraft.util;

/**
 * Represents the identity of a Minecraft resource, such as an item or block.
 * Provides methods to access the namespace, path, and full identifier.
 */
public interface MinecraftIdentity {
    String namespace();
    String path();
    String full();
}
