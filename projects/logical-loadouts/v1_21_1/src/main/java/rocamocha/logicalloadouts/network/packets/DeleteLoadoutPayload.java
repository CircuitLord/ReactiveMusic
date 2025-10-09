package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import rocamocha.logicalloadouts.LogicalLoadouts;

import java.util.UUID;

public record DeleteLoadoutPayload(UUID loadoutId) implements CustomPayload {
    public static final CustomPayload.Id<DeleteLoadoutPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "delete_loadout")
    );
    
    public static final PacketCodec<RegistryByteBuf, DeleteLoadoutPayload> CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC, DeleteLoadoutPayload::loadoutId,
        DeleteLoadoutPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}