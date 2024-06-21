package circuitlord.reactivemusic;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

public class VersionHelper {





    // 1.20

/*
    public static Entity GetRidingEntity(ClientPlayerEntity player) {
        return player.getControllingVehicle();
    }
*/




    // -------------------------------------


    // 1.19

    public static Entity GetRidingEntity(ClientPlayerEntity player) {
        return player.getVehicle();
    }



}
