package circuitlord.reactivemusic.fabric;

import circuitlord.reactivemusic.ReactiveMusic;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side structure detection, adopted from Rebel459/music-and-melody's
 * StructureMusicHandler (26.1 branch) with mod config swapped for a gamerule
 * per user request. Runs from the "main" entrypoint so it also fires on
 * singleplayer's integrated server, not just dedicated servers.
 */
public class RMStructureSyncServer {

    public static final GameRule<Boolean> STRUCTURE_SYNC = GameRuleBuilder.forBoolean(true)
            .category(GameRuleCategory.MISC)
            .buildAndRegister(Identifier.fromNamespaceAndPath(ReactiveMusic.MOD_ID, "structure_sync"));

    // padding around each structure piece's bounding box a player counts as "in" the structure for.
    // Real piece geometry is often riddled with small gaps between adjacent pieces (mineshafts
    // especially - lots of short corridor segments), so testing exact containment alone flickers
    // on/off constantly as you walk through; padding bridges those gaps. Y gets a much smaller pad
    // than X/Z deliberately: a piece's own bounding box height is already accurate 3D geometry (a
    // deep mineshaft's vertical shaft pieces can legitimately span dozens of blocks), so padding it
    // further isn't needed to fix flicker and would only make matches reach further than intended.
    private static final int DETECTION_RADIUS_XZ = 16;
    private static final int DETECTION_RADIUS_Y = 6;

    private static int serverTicks = 0;
    private static boolean shouldUpdateStructures = true;
    private static final Map<ResourceKey<Level>, List<Holder.Reference<Structure>>> STRUCTURES = new HashMap<>();
    private static final Map<UUID, Info> LAST_STRUCTURES = new HashMap<>();
    private static final Map<UUID, BlockPos> LAST_POSITION = new HashMap<>();

    private record Info(Set<Identifier> structures, Set<Identifier> tags) {}

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            shouldUpdateStructures = true;
            STRUCTURES.clear();
            LAST_STRUCTURES.clear();
            LAST_POSITION.clear();
        });

        ServerTickEvents.END_SERVER_TICK.register(RMStructureSyncServer::onEndTick);
    }

    private static void onEndTick(MinecraftServer server) {
        if (!server.getGameRules().get(STRUCTURE_SYNC)) return;

        if (++serverTicks < 20) return;
        serverTicks = 0;

        Map<ResourceKey<Level>, List<Holder.Reference<Structure>>> structures = getStructures(server);

        for (ServerLevel level : server.getAllLevels()) {
            StructureManager structureManager = level.structureManager();
            List<Holder.Reference<Structure>> levelStructures = structures.get(level.dimension());
            if (levelStructures == null) continue;

            for (ServerPlayer player : level.players()) {
                UUID playerId = player.getUUID();
                BlockPos blockPos = player.blockPosition();

                if (blockPos.equals(LAST_POSITION.get(playerId))) continue;
                LAST_POSITION.put(playerId, blockPos);

                Set<Identifier> structureIds = new HashSet<>();
                Set<Identifier> structureTags = new HashSet<>();

                for (Holder.Reference<Structure> structure : levelStructures) {
                    if (!isNearStructure(structureManager, blockPos, structure.value())) continue;

                    structure.unwrapKey().ifPresent(key -> structureIds.add(key.identifier()));
                    structure.tags().forEach(tag -> structureTags.add(tag.location()));
                }

                Info info = new Info(structureIds, structureTags);
                if (info.equals(LAST_STRUCTURES.get(playerId))) continue;

                LAST_STRUCTURES.put(playerId, info);
                ServerPlayNetworking.send(player, new RMStructureSyncPayload(info.structures(), info.tags()));
            }
        }
    }

    // replaces StructureManager.getStructureWithPieceAt's exact per-piece containment with the
    // same check padded by DETECTION_RADIUS_XZ/Y, to avoid flicker at piece boundaries
    private static boolean isNearStructure(StructureManager structureManager, BlockPos pos, Structure structure) {
        for (StructureStart start : structureManager.startsForStructure(SectionPos.of(pos), structure)) {
            for (StructurePiece piece : start.getPieces()) {
                if (piece.getBoundingBox().inflatedBy(DETECTION_RADIUS_XZ, DETECTION_RADIUS_Y, DETECTION_RADIUS_XZ).isInside(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<ResourceKey<Level>, List<Holder.Reference<Structure>>> getStructures(MinecraftServer server) {
        if (shouldUpdateStructures) {
            for (ServerLevel level : server.getAllLevels()) {
                STRUCTURES.put(level.dimension(), level.registryAccess().lookupOrThrow(Registries.STRUCTURE).listElements().toList());
            }
            shouldUpdateStructures = false;
        }
        return STRUCTURES;
    }
}
