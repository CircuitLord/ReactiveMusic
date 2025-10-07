package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.ReactiveMusicDebug;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

public final class BiomeIdentityPlugin extends ReactiveMusicPlugin {
    
    // Moved from SongPicker - now owned by this plugin
    private static String currentBiomeName = "";
    
    // Public accessor for SongPicker.isEntryValid()
    public static String getCurrentBiomeName() {
        return currentBiomeName;
    }
    
    public BiomeIdentityPlugin() {
        super("reactivemusic", "biome_id");
    }
    
    @Override public void init() {
        ReactiveMusicDebug.log(ReactiveMusicDebug.LogCategory.PLUGIN_EXECUTION, 
            "BiomeIdentityPlugin initialized");
    }

    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, java.util.Map<EventRecord, Boolean> out) {
        if (player == null || world == null) return;

        MinecraftVector3 pos = player.location().pos();
        currentBiomeName = world.blocks().getBiomeAt(pos);
    }
}

