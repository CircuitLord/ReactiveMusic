package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;

public record CreateLoadoutPayload(String loadoutName) implements CustomPayload {
    public static final CustomPayload.Id<CreateLoadoutPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "create_loadout")
    );
    
    public static final PacketCodec<RegistryByteBuf, CreateLoadoutPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, CreateLoadoutPayload::loadoutName,
        CreateLoadoutPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}