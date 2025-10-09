package rocamocha.mochamix.impl.player.adapter;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer.Location;
import rocamocha.mochamix.api.io.MinecraftView;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;
import rocamocha.mochamix.impl.vector3.Vector3Socket;

public class PlayerLocationAdapter implements Location {
    protected final PlayerEntity p;
    public PlayerLocationAdapter(PlayerEntity p) { this.p = p; }

    @Override public MinecraftVector3 pos() { return new Vector3Socket(p.getPos()); }
    
    @Override public MinecraftVector3 blockPos(){ return new Vector3Socket(p.getBlockPos()); }
    
    @Override public MinecraftWorld world() { return MinecraftView.of(p.getWorld()); }

    @Override public String dimension() { Identifier id = p.getWorld().getRegistryKey().getValue(); return id.toString(); }
    
    @Override public String biome() {
        RegistryEntry<Biome> e = p.getWorld().getBiome(p.getBlockPos());
        return e.getKey().map(k -> k.getValue().toString()).orElse("unknown");
    }
}
