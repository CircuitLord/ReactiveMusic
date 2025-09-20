package rocamocha.mochamix.impl.world.adapter;

import net.minecraft.world.World;
import rocamocha.mochamix.api.minecraft.MinecraftWorld.Time;

public class WorldTimeAdapter implements Time {
    protected final World w;
    public WorldTimeAdapter(World w) { this.w = w; }

    @Override public long timeOfDay()    { return w.getTimeOfDay(); }
    @Override public long worldAge()    { return w.getTime(); }
    @Override public int daysPassed()   { return (int)(w.getTime() / 24000L); }
    
}
