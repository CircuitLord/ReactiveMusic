package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.data.Loadout;

public record CreateLoadoutFromDataPayload(Loadout loadout) implements CustomPayload {
    public static final CustomPayload.Id<CreateLoadoutFromDataPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "create_loadout_from_data")
    );
    
    public static final PacketCodec<RegistryByteBuf, CreateLoadoutFromDataPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.NBT_COMPOUND, payload -> payload.loadout.toNbt(),
        nbt -> new CreateLoadoutFromDataPayload(Loadout.fromNbt(nbt))
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}