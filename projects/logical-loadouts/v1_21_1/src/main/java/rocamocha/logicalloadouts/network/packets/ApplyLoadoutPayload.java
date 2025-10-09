package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import rocamocha.logicalloadouts.LogicalLoadouts;

import java.util.UUID;

public record ApplyLoadoutPayload(UUID loadoutId) implements CustomPayload {
    public static final CustomPayload.Id<ApplyLoadoutPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "apply_loadout")
    );
    
    public static final PacketCodec<RegistryByteBuf, ApplyLoadoutPayload> CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC, ApplyLoadoutPayload::loadoutId,
        ApplyLoadoutPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}