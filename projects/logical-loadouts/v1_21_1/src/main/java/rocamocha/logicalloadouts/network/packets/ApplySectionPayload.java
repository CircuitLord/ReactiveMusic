package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import rocamocha.logicalloadouts.LogicalLoadouts;

import java.util.UUID;

public record ApplySectionPayload(UUID loadoutId, String sectionName) implements CustomPayload {
    public static final CustomPayload.Id<ApplySectionPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "apply_section")
    );
    
    public static final PacketCodec<RegistryByteBuf, ApplySectionPayload> CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC, ApplySectionPayload::loadoutId,
        PacketCodecs.STRING, ApplySectionPayload::sectionName,
        ApplySectionPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}