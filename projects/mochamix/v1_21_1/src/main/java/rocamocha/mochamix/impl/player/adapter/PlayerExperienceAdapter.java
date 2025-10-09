package rocamocha.mochamix.impl.player.adapter;

import net.minecraft.entity.player.PlayerEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer.Experience;
import rocamocha.mochamix.mixin.accessor.PlayerEntityAccess;

public class PlayerExperienceAdapter implements Experience {
    protected final PlayerEntity p;
    public PlayerExperienceAdapter(PlayerEntity p) { this.p = p; }

    @Override public int   level() { return ((PlayerEntityAccess) (Object) p).mocha$getExperienceLevel(); }
    @Override public float progress() { return ((PlayerEntityAccess) (Object) p).mocha$getExperienceProgress(); }
    @Override public int   total() { return ((PlayerEntityAccess) (Object) p).mocha$getTotalExperience(); }
    
}
