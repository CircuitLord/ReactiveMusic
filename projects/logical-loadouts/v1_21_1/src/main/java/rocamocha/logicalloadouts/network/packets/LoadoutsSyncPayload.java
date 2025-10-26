package rocamocha.logicalloadouts.network.packets;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;

import java.util.List;

public record LoadoutsSyncPayload(List<NbtCompound> personalLoadouts, List<NbtCompound> serverSharedLoadouts) implements CustomPayload {
    public static final CustomPayload.Id<LoadoutsSyncPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "loadouts_sync")
    );
    
    public static final PacketCodec<RegistryByteBuf, LoadoutsSyncPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.NBT_COMPOUND.collect(PacketCodecs.toList()), LoadoutsSyncPayload::personalLoadouts,
        PacketCodecs.NBT_COMPOUND.collect(PacketCodecs.toList()), LoadoutsSyncPayload::serverSharedLoadouts,
        LoadoutsSyncPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}