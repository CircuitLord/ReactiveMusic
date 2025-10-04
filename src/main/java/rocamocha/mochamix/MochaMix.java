package rocamocha.mochamix;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import rocamocha.mochamix.commands.MochaMixCommandMenu;
import rocamocha.mochamix.commands.debug.DebugVisualizationHandlers;
import rocamocha.mochamix.commands.player.PlayerCommandHandlers;
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
