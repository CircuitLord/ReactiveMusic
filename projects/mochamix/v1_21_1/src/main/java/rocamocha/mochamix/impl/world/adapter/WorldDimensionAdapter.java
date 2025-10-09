package rocamocha.mochamix.impl.world.adapter;

import rocamocha.mochamix.api.minecraft.MinecraftWorld.Dimension;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class WorldDimensionAdapter implements Dimension {
    protected final World w;
    public WorldDimensionAdapter(World w) { this.w = w; }

    @Override public String id()         { Identifier id = w.getRegistryKey().getValue(); return id.toString(); }
    @Override public String namespace()  { return w.getRegistryKey().getValue().getNamespace(); }
    @Override public String path()       { return w.getRegistryKey().getValue().getPath(); }
}
