package rocamocha.mochamix.impl.player.adapter;

import net.minecraft.entity.player.PlayerEntity;
import rocamocha.mochamix.api.minecraft.MinecraftEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer.Mount;
import rocamocha.mochamix.api.io.MinecraftView;

public class PlayerMountAdapter implements Mount {
    private final PlayerEntity player;

    public PlayerMountAdapter(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public boolean riding() {
        return player.hasVehicle();
    }

    @Override
    public MinecraftEntity vehicle() {
        return MinecraftView.of(player.getVehicle());
    }
}
