package rocamocha.mochamix;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import rocamocha.mochamix.commands.MochaMixCommandMenu;
import rocamocha.mochamix.commands.player.PlayerCommandHandlers;

/**
 * Main mod class for MochaMix.
 * This class initializes the mod and registers commands.
 * Currently, it only sets up a command structure under /mochamix api.
 * The actual command implementations are in separate handler classes.
 */
@SuppressWarnings("unused")
public class MochaMix implements ModInitializer {
    @Override public void onInitialize() {
    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
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
    });
    }
}
