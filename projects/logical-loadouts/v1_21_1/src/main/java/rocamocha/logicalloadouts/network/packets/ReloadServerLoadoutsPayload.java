package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;

/**
 * Payload for requesting server to reload server-shared loadouts
 */
public record ReloadServerLoadoutsPayload() implements CustomPayload {
    public static final CustomPayload.Id<ReloadServerLoadoutsPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "reload_server_loadouts")
    );

    public static final PacketCodec<RegistryByteBuf, ReloadServerLoadoutsPayload> CODEC = PacketCodec.unit(new ReloadServerLoadoutsPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}