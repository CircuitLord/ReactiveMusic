package rocamocha.mochamix;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import rocamocha.mochamix.commands.MochaMixCommandMenu;
import rocamocha.mochamix.commands.debug.DebugVisualizationHandlers;
import rocamocha.mochamix.commands.player.PlayerCommandHandlers;
import rocamocha.mochamix.commands.zones.ZoneCommandHandlers;
import rocamocha.mochamix.render.DebugRenderManager;
import rocamocha.mochamix.render.ClientZoneRenderer;

/**
 * Main mod class for MochaMix.
 * This class initializes the mod and registers commands.
 * Currently, it only sets up a command structure under /mochamix api.
 * The actual command implementations are in separate handler classes.
 */
@SuppressWarnings("unused")
public class MochaMix implements ModInitializer {
    @Override public void onInitialize() {
        // Initialize debug rendering system
        DebugRenderManager.initialize();
        
        // Initialize client-side zone renderer with Fabric events
        ClientZoneRenderer.initialize();
    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
        // Register API commands under /mochamix api
        dispatcher.register(
            literal("mochamix")
            .then(literal("api")
                .executes(MochaMixCommandMenu::main)
                .then(literal("player")
                    .executes(PlayerCommandHandlers::playerInfo)
                    .then(literal("mainhand")
                        .executes(PlayerCommandHandlers::mainhandInfo))
                    .then(literal("nearby_entities")
                        .executes(PlayerCommandHandlers::nearbyEntitiesInfo))
                )
            )
        );
        
        // Register zone management commands under /mochamix zones
        dispatcher.register(
            literal("mochamix")
            .then(literal("zones")
                .executes(ZoneCommandHandlers::listZones)
                .then(literal("create")
                    .then(literal("center")
                        .then(argument("name", StringArgumentType.string())
                        .then(argument("center", Vec3ArgumentType.vec3())
                        .then(argument("x_radius", DoubleArgumentType.doubleArg())
                        .then(argument("y_radius", DoubleArgumentType.doubleArg())
                        .then(argument("z_radius", DoubleArgumentType.doubleArg())
                            .executes(ZoneCommandHandlers::createZoneFromCenter)
                        ))))))
                    .then(literal("here")
                        .then(argument("name", StringArgumentType.string())
                        .then(argument("x_radius", DoubleArgumentType.doubleArg())
                        .then(argument("y_radius", DoubleArgumentType.doubleArg())
                        .then(argument("z_radius", DoubleArgumentType.doubleArg())
                            .executes(ZoneCommandHandlers::createZoneFromPlayerCenter)
                        )))))
                    .then(literal("corners")
                        .then(argument("name", StringArgumentType.string())
                        .then(argument("corner1", Vec3ArgumentType.vec3())
                        .then(argument("corner2", Vec3ArgumentType.vec3())
                            .executes(ZoneCommandHandlers::createZoneFromCorners)
                        ))))
                )
                .then(literal("list")
                    .executes(ZoneCommandHandlers::listZones))
                .then(literal("delete")
                    .then(argument("index", IntegerArgumentType.integer(1))
                        .executes(ZoneCommandHandlers::deleteZoneByIndex)))
                .then(literal("clear")
                    .executes(ZoneCommandHandlers::clearAllZones))
                .then(literal("render")
                    .executes(ZoneCommandHandlers::showZoneRenderStatus)
                    .then(literal("on")
                        .executes(ZoneCommandHandlers::enableZoneRendering))
                    .then(literal("off")
                        .executes(ZoneCommandHandlers::disableZoneRendering))
                    .then(literal("toggle")
                        .executes(ZoneCommandHandlers::toggleZoneRendering))
                    .then(literal("status")
                        .executes(ZoneCommandHandlers::showZoneRenderStatus))
                    .then(literal("sync")
                        .executes(ZoneCommandHandlers::syncZoneRendering)))
                .then(literal("debug")
                    .executes(ZoneCommandHandlers::debugZoneSystem))
            )
        );

        // Register debug commands under /mochamix debug
        dispatcher.register(
            literal("mochamix")
            .then(literal("debug")
                .then(literal("box_center")
                    .then(argument("center", Vec3ArgumentType.vec3())
                    .then(argument("x_radius", DoubleArgumentType.doubleArg())
                    .then(argument("y_radius", DoubleArgumentType.doubleArg())
                    .then(argument("z_radius", DoubleArgumentType.doubleArg())
                        .executes(DebugVisualizationHandlers::visualizeBoxFromCenter)
                    )))))
                .then(literal("box_minmax")
                    .then(argument("min", Vec3ArgumentType.vec3())
                    .then(argument("max", Vec3ArgumentType.vec3())
                        .executes(DebugVisualizationHandlers::visualizeBoxFromMinMax)
                    )))
                // Keep the convenience command for simple usage
                .then(literal("box_here")
                    .then(argument("x_radius", DoubleArgumentType.doubleArg())
                    .then(argument("y_radius", DoubleArgumentType.doubleArg())
                    .then(argument("z_radius", DoubleArgumentType.doubleArg())
                        .executes(DebugVisualizationHandlers::visualizeBoxAtPlayerPosition)
                    ))))
                // Configuration commands for debug rendering
                .then(literal("render")
                    .then(literal("enable")
                        .executes(DebugVisualizationHandlers::enableZoneRendering))
                    .then(literal("disable")
                        .executes(DebugVisualizationHandlers::disableZoneRendering))
                    .then(literal("toggle")
                        .executes(DebugVisualizationHandlers::toggleZoneRendering))
                    .then(literal("clear")
                        .executes(DebugVisualizationHandlers::clearAllZones))
                    )
            )
        );
    });
    }
}
