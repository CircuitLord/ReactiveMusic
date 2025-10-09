package rocamocha.mochamix.impl.player.adapter;

import net.minecraft.entity.player.PlayerEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer.Motion;

public class PlayerMotionAdapter implements Motion {
    private final PlayerEntity player;

    public PlayerMotionAdapter(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public boolean sneaking() {
        return player.isSneaking();
    }

    @Override
    public boolean sprinting() {
        return player.isSprinting();
    }

    @Override
    public boolean crawling() {
        return player.isCrawling();
    }

    @Override
    public boolean swimming() {
        return player.isSwimming();
    }

    @Override
    public boolean gliding() {
        return player.isFallFlying();
    }
}
