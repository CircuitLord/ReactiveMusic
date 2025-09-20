package rocamocha.mochamix.impl.world.adapter;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rocamocha.mochamix.api.minecraft.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld.Blocks;

public class WorldBlocksAdapter implements Blocks {
    protected final World w;
    public WorldBlocksAdapter(World w) {this.w = w;}

    @Override public String getIdAt(int x, int y, int z) {
        var state = w.getBlockState(new BlockPos(x,y,z)).getBlock();
        Identifier id = Registries.BLOCK.getId(state);
        return id == null ? "unknown" : id.toString();
    }
    @Override public String getIdAt(MinecraftVector3 p) {
        var blockPos = p.asBlockPos();
        return getIdAt(blockPos.xi(), blockPos.yi(), blockPos.zi());
    }

    @Override public String getBiomeAt(int x, int y, int z) {
        return w.getBiome(new BlockPos(x,y,z)).getKey()
                .map(k -> k.getValue().toString()).orElse("unknown");
    }
    @Override public String getBiomeAt(MinecraftVector3 p) {
        var blockPos = p.asBlockPos();
        return getBiomeAt(blockPos.xi(), blockPos.yi(), blockPos.zi());
    }
}
