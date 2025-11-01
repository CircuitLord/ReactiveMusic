package rocamocha.lootsparkle;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import net.minecraft.util.math.BlockPos;

/**
 * Network packets for sparkle synchronization between server and client
 */
public class SparkleNetworking {
    // Packet identifiers
    public static final CustomPayload.Id<SyncSparklePacket> SYNC_SPARKLE = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "sync_sparkle"));
    public static final CustomPayload.Id<RemoveSparklePacket> REMOVE_SPARKLE = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "remove_sparkle"));
    public static final CustomPayload.Id<InteractSparklePacket> INTERACT_SPARKLE = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "interact_sparkle"));
    public static final CustomPayload.Id<InteractionFailedPacket> INTERACTION_FAILED = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "interaction_failed"));

    public static void initialize() {
        LootSparkle.LOGGER.info("Initializing sparkle networking...");

        // Register packet codecs
        registerCodecs();

        // Register packet receivers
        ServerPlayNetworking.registerGlobalReceiver(INTERACT_SPARKLE, (packet, context) -> {
            UUID sparkleId = packet.sparkleId();
            context.server().execute(() -> {
                SparkleManager.triggerSparkleInteraction(context.player(), sparkleId);
            });
        });
    }

    public static void registerClientCodecs() {
        LootSparkle.LOGGER.info("Registering client-side sparkle networking codecs...");

        // Register codecs for packets the client receives (S2C) and sends (C2S)
        try {
            PayloadTypeRegistry.playS2C().register(SYNC_SPARKLE, SyncSparklePacket.CODEC);
            PayloadTypeRegistry.playS2C().register(REMOVE_SPARKLE, RemoveSparklePacket.CODEC);
            PayloadTypeRegistry.playS2C().register(INTERACTION_FAILED, InteractionFailedPacket.CODEC);
            PayloadTypeRegistry.playC2S().register(INTERACT_SPARKLE, InteractSparklePacket.CODEC);
            LootSparkle.LOGGER.debug("Registered client-side sparkle networking codecs");
        } catch (IllegalArgumentException e) {
            // Codecs already registered, ignore
            LootSparkle.LOGGER.debug("Client-side sparkle networking codecs already registered");
        }
    }

    private static void registerCodecs() {
        PayloadTypeRegistry.playS2C().register(SYNC_SPARKLE, SyncSparklePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(REMOVE_SPARKLE, RemoveSparklePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(INTERACTION_FAILED, InteractionFailedPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(INTERACT_SPARKLE, InteractSparklePacket.CODEC);
        LootSparkle.LOGGER.debug("Registered sparkle networking codecs");
    }

    /**
     * Packet sent from server to client to sync a sparkle
     */
    public record SyncSparklePacket(UUID sparkleId, UUID playerId, BlockPos position, int tierLevel) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, SyncSparklePacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, SyncSparklePacket::sparkleId,
            UUID_CODEC, SyncSparklePacket::playerId,
            BlockPos.PACKET_CODEC, SyncSparklePacket::position,
            PacketCodecs.INTEGER, SyncSparklePacket::tierLevel,
            SyncSparklePacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return SYNC_SPARKLE;
        }
    }

    /**
     * Packet sent from server to client to remove a sparkle
     */
    public record RemoveSparklePacket(UUID sparkleId, UUID playerId) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, RemoveSparklePacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, RemoveSparklePacket::sparkleId,
            UUID_CODEC, RemoveSparklePacket::playerId,
            RemoveSparklePacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return REMOVE_SPARKLE;
        }
    }

    /**
     * Packet sent from client to server to interact with a sparkle
     */
    public record InteractSparklePacket(UUID sparkleId) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, InteractSparklePacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, InteractSparklePacket::sparkleId,
            InteractSparklePacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return INTERACT_SPARKLE;
        }
    }

    /**
     * Packet sent from server to client when sparkle interaction fails
     */
    public record InteractionFailedPacket(String reason) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, InteractionFailedPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, InteractionFailedPacket::reason,
            InteractionFailedPacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return INTERACTION_FAILED;
        }
    }

    /**
     * UUID codec using most/least significant bits
     */
    private static final PacketCodec<PacketByteBuf, UUID> UUID_CODEC = new PacketCodec<PacketByteBuf, UUID>() {
        @Override
        public UUID decode(PacketByteBuf buf) {
            return new UUID(buf.readLong(), buf.readLong());
        }

        @Override
        public void encode(PacketByteBuf buf, UUID value) {
            buf.writeLong(value.getMostSignificantBits());
            buf.writeLong(value.getLeastSignificantBits());
        }
    };
}