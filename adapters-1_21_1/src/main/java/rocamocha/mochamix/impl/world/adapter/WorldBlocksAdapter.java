package rocamocha.mochamix.impl.world.adapter;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld.Blocks;

public class WorldBlocksAdapter implements Blocks {
    protected final World w;
    public WorldBlocksAdapter(World w) {this.w = w;}

    private String adapt$IdAt(BlockPos blockPos) {
        BlockState blockState = w.getBlockState(blockPos);
        Identifier id = Registries.BLOCK.getId(blockState.getBlock());
        return id == null ? "unknown" : id.toString();
    }

    private String adapt$BiomeAt(BlockPos blockPos) {
        return w.getBiome(blockPos).getKey()
                .map(k -> k.getValue().toString()).orElse("unknown");
    }

    private int adapt$LightLevelAt(BlockPos blockPos) {
        return w.getLightLevel(blockPos);
    }

    private int adapt$BlockLightAt(BlockPos blockPos) {
        return w.getLightLevel(net.minecraft.world.LightType.BLOCK, blockPos);
    }

    private int adapt$SkyLightAt(BlockPos blockPos) {
        return w.getLightLevel(net.minecraft.world.LightType.SKY, blockPos);
    }

    @Override public String getIdAt(int x, int y, int z) { return adapt$IdAt(new BlockPos(x,y,z)); }
    @Override public String getIdAt(MinecraftVector3 p) { return adapt$IdAt(p.asNativeBlockPos()); }

    @Override public String getBiomeAt(int x, int y, int z) { return adapt$BiomeAt(new BlockPos(x,y,z)); }
    @Override public String getBiomeAt(MinecraftVector3 p) { return adapt$BiomeAt(p.asNativeBlockPos()); }

    @Override public int getLightLevelAt(int x, int y, int z) { return adapt$LightLevelAt(new BlockPos(x, y, z));}
    @Override public int getLightLevelAt(MinecraftVector3 p) {return adapt$LightLevelAt(p.asNativeBlockPos()); }

    @Override public int getBlockLightAt(int x, int y, int z) { return adapt$BlockLightAt(new BlockPos(x, y, z));}
    @Override public int getBlockLightAt(MinecraftVector3 p) { return adapt$BlockLightAt(p.asNativeBlockPos());}

    @Override public int getSkyLightAt(int x, int y, int z) {return adapt$SkyLightAt(new BlockPos(x, y, z)); }
    @Override public int getSkyLightAt(MinecraftVector3 p) { return adapt$SkyLightAt(p.asNativeBlockPos()); }
}
