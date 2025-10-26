package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import rocamocha.logicalloadouts.LogicalLoadouts;

import java.util.UUID;

public record DepositSectionPayload(UUID loadoutId, String sectionName) implements CustomPayload {
    public static final CustomPayload.Id<DepositSectionPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "deposit_section")
    );
    
    public static final PacketCodec<RegistryByteBuf, DepositSectionPayload> CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC, DepositSectionPayload::loadoutId,
        PacketCodecs.STRING, DepositSectionPayload::sectionName,
        DepositSectionPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}