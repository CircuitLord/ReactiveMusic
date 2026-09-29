package circuitlord.reactivemusic.fabric;

import circuitlord.reactivemusic.ReactiveMusic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

public record RMStructureSyncPayload(Set<Identifier> structures, Set<Identifier> tags) implements CustomPacketPayload {

    public static final Type<RMStructureSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ReactiveMusic.MOD_ID, "structure_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RMStructureSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(HashSet::new, Identifier.STREAM_CODEC), RMStructureSyncPayload::structures,
            ByteBufCodecs.collection(HashSet::new, Identifier.STREAM_CODEC), RMStructureSyncPayload::tags,
            RMStructureSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
