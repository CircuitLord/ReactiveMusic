// src/main/java/rocamocha/mochamix/runtime/WorldView.java
package rocamocha.mochamix.impl.world;

import net.minecraft.world.World;

import rocamocha.mochamix.api.minecraft.*;
import rocamocha.mochamix.impl.world.adapter.WorldBlocksAdapter;
import rocamocha.mochamix.impl.world.adapter.WorldDimensionAdapter;
import rocamocha.mochamix.impl.world.adapter.WorldTimeAdapter;
import rocamocha.mochamix.impl.world.adapter.WorldWeatherAdapter;

/**
 * Socket-backed view wrapper for World.
 * @see rocamocha.mochamix.api.minecraft.MinecraftWorld
 */
public class WorldSocket implements MinecraftWorld {
    
    protected final World w;
    @Override public World asNative() { return w; }

    // Adapters for different world functionalities
    private final WorldTimeAdapter time;
    private final WorldWeatherAdapter weather;
    private final WorldDimensionAdapter dimension;
    private final WorldBlocksAdapter blocks;

    // Constructor initializes the world and its adapters
    public WorldSocket(World w) {
        this.w = w;
        this.time = new WorldTimeAdapter(w);
        this.weather = new WorldWeatherAdapter(w);
        this.dimension = new WorldDimensionAdapter(w);
        this.blocks = new WorldBlocksAdapter(w);
    }
    
    // Method hooks into adapters for clear separation of concerns
    public final WorldTimeAdapter time() { return time; }
    public final WorldWeatherAdapter weather() { return weather; }
    public final WorldDimensionAdapter dimension() { return dimension; }
    public final WorldBlocksAdapter blocks() { return blocks; }

    @Override public boolean clientSided(){ return w.isClient(); }

}
