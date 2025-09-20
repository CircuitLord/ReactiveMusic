package rocamocha.mochamix.impl.player.adapter;

import net.minecraft.entity.player.PlayerEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer.Hunger;

public class PlayerHungerAdapter implements Hunger {
    protected final PlayerEntity p;
    public PlayerHungerAdapter(PlayerEntity p) { this.p = p; }

    @Override public int foodLevel() { return p.getHungerManager().getFoodLevel(); }
    @Override public float saturation() { return p.getHungerManager().getSaturationLevel(); }
    @Override public boolean isNotFull() { return p.getHungerManager().isNotFull(); }
}
