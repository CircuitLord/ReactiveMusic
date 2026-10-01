package circuitlord.reactivemusic.fabric;

import circuitlord.reactivemusic.platform.BiomeTagHelper;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.tags.TagKey;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FabricBiomeTagHelper implements BiomeTagHelper {

    // Songpacks have always been able to use the ConventionalBiomeTags field names (the list
    // MAKING_SONGPACKS.md links to), e.g. "TAG=IS_COLD_OVERWORLD". Several of those don't match
    // their tag's actual path ("c:is_cold/overworld", "c:is_dense_vegetation", ...), so map field
    // names to real paths here. Keys/lookups drop a leading "is_", since normalizeBiomeTagPath
    // prepends one to names like PRIMARY_WOOD_TYPE_OAK that don't have it.
    private static final Set<String> TAG_PATHS = new HashSet<>();
    private static final Map<String, String> FIELD_NAME_TO_PATH = new HashMap<>();

    static {
        for (Field field : ConventionalBiomeTags.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != TagKey.class) continue;
            try {
                String path = ((TagKey<?>) field.get(null)).location().getPath();
                TAG_PATHS.add(path);
                FIELD_NAME_TO_PATH.put(stripIsPrefix(field.getName().toLowerCase()), path);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static String stripIsPrefix(String path) {
        return path.startsWith("is_") ? path.substring(3) : path;
    }

    @Override
    public String getTagNamespace() {
        return "c";
    }

    @Override
    public String remapTagPath(String path) {
        // a real tag path always wins, so field-name compat can't shadow one
        if (TAG_PATHS.contains(path)) return path;

        String fromFieldName = FIELD_NAME_TO_PATH.get(stripIsPrefix(path));
        return fromFieldName != null ? fromFieldName : path;
    }
}
