package rocamocha.mochamix.impl.player.adapter;

import net.minecraft.entity.player.PlayerEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer.Gamemode;

public class PlayerGamemodeAdapter implements Gamemode {
    protected final PlayerEntity p;
    public PlayerGamemodeAdapter(PlayerEntity p) { this.p = p; }

    @Override public boolean isSurvival() { return !isCreative() && !isSpectator() && p.canModifyBlocks(); }
    @Override public boolean isAdventure() { return !isCreative() && !isSpectator() && !p.canModifyBlocks(); }
    @Override public boolean isCreative() { return p.isCreative(); }
    @Override public boolean isSpectator() { return p.isSpectator(); }
    
}
