package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import rocamocha.logicalloadouts.LogicalLoadouts;

import java.util.UUID;

public record SaveLoadoutPayload(UUID loadoutId) implements CustomPayload {
    public static final CustomPayload.Id<SaveLoadoutPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "save_loadout")
    );
    
    public static final PacketCodec<RegistryByteBuf, SaveLoadoutPayload> CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC, SaveLoadoutPayload::loadoutId,
        SaveLoadoutPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}