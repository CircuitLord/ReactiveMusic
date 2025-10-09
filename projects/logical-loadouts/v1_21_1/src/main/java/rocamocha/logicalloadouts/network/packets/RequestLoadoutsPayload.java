package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;

public record RequestLoadoutsPayload() implements CustomPayload {
    public static final CustomPayload.Id<RequestLoadoutsPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "request_loadouts")
    );
    
    public static final PacketCodec<RegistryByteBuf, RequestLoadoutsPayload> CODEC = PacketCodec.unit(new RequestLoadoutsPayload());
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}