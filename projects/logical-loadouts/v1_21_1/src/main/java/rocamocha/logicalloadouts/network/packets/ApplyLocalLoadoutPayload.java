package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.data.Loadout;

public record ApplyLocalLoadoutPayload(Loadout loadout) implements CustomPayload {
    public static final CustomPayload.Id<ApplyLocalLoadoutPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "apply_local_loadout")
    );
    
    public static final PacketCodec<RegistryByteBuf, ApplyLocalLoadoutPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.NBT_COMPOUND, payload -> payload.loadout.toNbt(),
        nbt -> new ApplyLocalLoadoutPayload(Loadout.fromNbt(nbt))
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}