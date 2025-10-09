package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;

public record OperationResultPayload(String operation, boolean success, String message) implements CustomPayload {
    public static final CustomPayload.Id<OperationResultPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "operation_result")
    );
    
    public static final PacketCodec<RegistryByteBuf, OperationResultPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, OperationResultPayload::operation,
        PacketCodecs.BOOL, OperationResultPayload::success,
        PacketCodecs.STRING, OperationResultPayload::message,
        OperationResultPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}