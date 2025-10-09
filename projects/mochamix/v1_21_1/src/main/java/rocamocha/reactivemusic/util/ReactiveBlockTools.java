package rocamocha.reactivemusic.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

public class ReactiveBlockTools {

    /**
     * Count all blocks in a box around an origin position, ignoring any block IDs in the blacklist.
     * @param world The Minecraft world
     * @param origin The center position
     * @param radius The radius to check in each direction (x, y, z)
     * @param blacklist A set of block IDs to ignore
     * @return A map of block IDs to their counts
     */
    public static Map<String,Integer> countAllInBox(MinecraftWorld world, MinecraftVector3 origin, int radius, Set<String> blacklist) {
        Map<String,Integer> out = new HashMap<>();
        for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
                MinecraftVector3 pos = origin.offset(0, y, z).asBlockPos();
                String key = world.blocks().getIdAt(pos);
                if (!blacklist.contains(key)) out.merge(key, 1, Integer::sum);
            }
        }
        return out;
    }
}
