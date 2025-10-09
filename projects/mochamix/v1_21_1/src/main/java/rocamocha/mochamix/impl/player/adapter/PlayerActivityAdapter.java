package rocamocha.mochamix.impl.player.adapter;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer.Activity;
import rocamocha.mochamix.mixin.accessor.PlayerEntityAccess;

public class PlayerActivityAdapter implements Activity {
    private final PlayerEntity player;

    public PlayerActivityAdapter(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public boolean sleeping() {
        return player.isSleeping();
    }

    @Override
    public boolean fishing() {
        FishingBobberEntity hook = ((PlayerEntityAccess) (Object) player).mocha$getFishHook();
        return hook != null;
    }
}
