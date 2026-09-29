package circuitlord.reactivemusic;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public class VersionHelper {

    public static Entity GetRidingEntity(LocalPlayer player) {
        return player.getVehicle();
    }
}
