package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;

public record LoadoutAppliedPayload(String loadoutName) implements CustomPayload {
    public static final CustomPayload.Id<LoadoutAppliedPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "loadout_applied")
    );
    
    public static final PacketCodec<RegistryByteBuf, LoadoutAppliedPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, LoadoutAppliedPayload::loadoutName,
        LoadoutAppliedPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}