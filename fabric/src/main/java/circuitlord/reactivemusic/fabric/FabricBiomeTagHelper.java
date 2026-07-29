package circuitlord.reactivemusic.fabric;

import circuitlord.reactivemusic.platform.BiomeTagHelper;

//? if <1.21 {
import java.util.Map;
//?}

public class FabricBiomeTagHelper implements BiomeTagHelper {

    //? if <1.21 {
    private static final Map<String, String> V1_REMAP = Map.ofEntries(
        Map.entry("is_hot", "climate_hot"),
        Map.entry("is_cold", "climate_cold"),
        Map.entry("is_wet", "climate_wet"),
        Map.entry("is_dry", "climate_dry"),
        Map.entry("is_tree/coniferous", "tree_coniferous"),
        Map.entry("is_tree/deciduous", "tree_deciduous"),
        Map.entry("is_tree/savanna", "tree_savanna"),
        Map.entry("is_tree/jungle", "tree_jungle"),
        Map.entry("is_end", "in_the_end"),
        Map.entry("is_nether", "in_the_nether"),
        Map.entry("is_overworld", "in_the_overworld"),
        Map.entry("is_sparse_vegetation", "vegetation_sparse"),
        Map.entry("is_dense_vegetation", "vegetation_dense"),
        Map.entry("is_coniferous_tree", "tree_coniferous"),
        Map.entry("is_deciduous_tree", "tree_deciduous"),
        Map.entry("is_savanna_tree", "tree_savanna"),
        Map.entry("is_jungle_tree", "tree_jungle")
    );
    //?}

    @Override
    public String getTagNamespace() {
        return "c";
    }

    //? if <1.21 {
    @Override
    public String remapTagPath(String path) {
        return V1_REMAP.getOrDefault(path, path);
    }
    //?}
}
