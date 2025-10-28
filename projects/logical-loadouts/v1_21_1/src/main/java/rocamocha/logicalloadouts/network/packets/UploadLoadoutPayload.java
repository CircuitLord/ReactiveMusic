package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.data.Loadout;

public record UploadLoadoutPayload(Loadout loadout, String name) implements CustomPayload {
    public static final CustomPayload.Id<UploadLoadoutPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "upload_loadout")
    );

    public static final PacketCodec<RegistryByteBuf, UploadLoadoutPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.NBT_COMPOUND, payload -> payload.loadout.toNbt(),
        PacketCodecs.STRING, payload -> payload.name,
        (nbt, name) -> new UploadLoadoutPayload(Loadout.fromNbt(nbt), name)
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}