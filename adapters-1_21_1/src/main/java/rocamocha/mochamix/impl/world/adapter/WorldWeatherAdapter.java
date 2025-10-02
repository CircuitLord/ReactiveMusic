package rocamocha.mochamix.impl.world.adapter;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld.Weather;;

public class WorldWeatherAdapter implements Weather {
    protected final World w;
    public WorldWeatherAdapter(World w) { this.w = w; }

    @Override
    public boolean isClearAt(MinecraftVector3 pos) {
        return !isRainingAt(pos) && !isThunderingAt(pos) && !isSnowingAt(pos);
    }

    @Override
    public boolean isRainingAt(MinecraftVector3 pos) {
        BlockPos blockPos = (BlockPos) pos.asNativeBlockPos();
        return w.isRaining() && w.getBiome(blockPos).value().getPrecipitation(blockPos) == Biome.Precipitation.RAIN;
    }

    @Override
    public boolean isThunderingAt(MinecraftVector3 pos) {
        return w.isThundering();
    }

    @Override
    public boolean isSnowingAt(MinecraftVector3 pos) {
        BlockPos blockPos = (BlockPos) pos.asNativeBlockPos();
        return w.isRaining() && w.getBiome(blockPos).value().getPrecipitation(blockPos) == Biome.Precipitation.SNOW;
    }
}
