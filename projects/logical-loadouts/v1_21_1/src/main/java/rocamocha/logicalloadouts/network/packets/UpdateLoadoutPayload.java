package rocamocha.logicalloadouts.network.packets;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;

public record UpdateLoadoutPayload(NbtCompound loadoutData) implements CustomPayload {
    public static final CustomPayload.Id<UpdateLoadoutPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "update_loadout")
    );
    
    public static final PacketCodec<RegistryByteBuf, UpdateLoadoutPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.NBT_COMPOUND, UpdateLoadoutPayload::loadoutData,
        UpdateLoadoutPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}