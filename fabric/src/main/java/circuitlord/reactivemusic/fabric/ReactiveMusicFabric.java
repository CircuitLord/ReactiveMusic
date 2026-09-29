package circuitlord.reactivemusic.fabric;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ReactiveMusicFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ReactiveMusic.init();
        ReactiveMusic.initClient();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommands.literal("reactivemusic")
                .executes(context -> {
                    Minecraft mc = context.getSource().getClient();
                    Screen screen = ModConfig.createScreen(mc.screen);
                    mc.execute(() -> mc.setScreen(screen));
                    return 1;
                })

                .then(ClientCommands.literal("logBlockCounter")
                        .executes(context -> {
                            SongPicker.queuedToPrintBlockCounter = true;
                            return 1;
                        })
                )

                .then(ClientCommands.literal("toggleSoundEventLogging")
                        .executes(context -> {
                            ReactiveMusic.printSoundEvents = !ReactiveMusic.printSoundEvents;
                            return 1;
                        })
                )

                .then(ClientCommands.literal("blacklistDimension")
                        .executes(context -> {
                            String key = context.getSource().getClient().level.dimension().identifier().toString();

                            if (ReactiveMusic.config.blacklistedDimensions.contains(key)) {
                                context.getSource().sendFeedback(Component.literal("[ReactiveMusic]: " + key + " was already in blacklist."));
                                return 1;
                            }

                            context.getSource().sendFeedback(Component.literal("[ReactiveMusic]: Added " + key + " to blacklist."));
                            ReactiveMusic.config.blacklistedDimensions.add(key);
                            ModConfig.saveConfig();
                            return 1;
                        })
                )

                .then(ClientCommands.literal("unblacklistDimension")
                        .executes(context -> {
                            String key = context.getSource().getClient().level.dimension().identifier().toString();

                            if (!ReactiveMusic.config.blacklistedDimensions.contains(key)) {
                                context.getSource().sendFeedback(Component.literal("[ReactiveMusic]: " + key + " was not in blacklist."));
                                return 1;
                            }

                            context.getSource().sendFeedback(Component.literal("[ReactiveMusic]: Removed " + key + " from blacklist."));
                            ReactiveMusic.config.blacklistedDimensions.remove(key);
                            ModConfig.saveConfig();
                            return 1;
                        })
                )

                .then(ClientCommands.literal("toggleLogging")
                        .executes(context -> {
                            ReactiveMusic.chatLoggingEnabled = !ReactiveMusic.chatLoggingEnabled;
                            context.getSource().sendFeedback(Component.literal("[ReactiveMusic]: Logging enabled: " + ReactiveMusic.chatLoggingEnabled));
                            return 1;
                        })
                )
            )
        );
    }
}
