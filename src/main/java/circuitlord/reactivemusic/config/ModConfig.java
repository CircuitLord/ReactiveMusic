package circuitlord.reactivemusic.config;


import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongLoader;
import circuitlord.reactivemusic.SongpackZip;
import com.google.gson.GsonBuilder;
import com.terraformersmc.modmenu.ModMenu;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.gui.controllers.BooleanController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.isxander.yacl3.platform.YACLPlatform.getConfigDir;


public class ModConfig {

    public static ModConfig getConfig() {
        return GSON.instance();
    }

    public static final ConfigClassHandler<ModConfig> GSON = ConfigClassHandler.createBuilder(ModConfig.class)
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(getConfigDir().resolve("ReactiveMusic.json5"))
                    .setJson5(true)
                    //.appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                    .build())
            .build();



    @SerialEntry
    public MusicDelayLength musicDelayLength = MusicDelayLength.NORMAL;

    @SerialEntry
    public boolean debugModeEnabled = false;

    @SerialEntry
    public boolean treatAsWhitelist = false;

    @SerialEntry
    public double confirmationResetDelay = 1.0;

    @SerialEntry
    public String loadedUserSongpack = "";



    public static Screen createScreen(Screen parent) {

        SongLoader.fetchAvailableSongpacks();

        return YetAnotherConfigLib.create(ModConfig.GSON, ((defaults, config, builder) -> {



            var songpacksBuilder = ConfigCategory.createBuilder();
            songpacksBuilder.name(Text.literal("Songpacks"));


            boolean arIsLoaded = Objects.equals(SongLoader.activeSongpack.name, "Adventure Redefined");

            songpacksBuilder.option(ButtonOption.createBuilder()
                    .name(Text.literal("Adventure Redefined (Default)"))
                    .description(
                            OptionDescription.createBuilder()
                                    .text(Text.literal("The included songpack with Reactive Music."))
                                    .build()
                    )

                    .available(!arIsLoaded)
                    .text(Text.literal(arIsLoaded ? "Loaded" : "Load"))
                    .action((yaclScreen, buttonOption) -> {
                        setActiveSongpack(null, true);
                        ReactiveMusic.refreshSongpack();
                        MinecraftClient.getInstance().setScreen(ModConfig.createScreen(parent));
                    })

                    .build());


            for (var songpackZip : SongLoader.availableSongpacks) {

                boolean isLoaded = Objects.equals(SongLoader.activeSongpack.name, songpackZip.config.name);


                if (!songpackZip.config.errorString.isEmpty()) {
                    songpacksBuilder.option(ButtonOption.createBuilder()
                            .name(Text.literal("FAILED LOADING: " + songpackZip.config.name))
                            .description(
                                    OptionDescription.createBuilder()
                                            .text(Text.literal("Encountered errors while loading:\n\n" + songpackZip.config.errorString))
                                            .build()
                            )

                            .available(false)
                            .text(Text.literal(""))
                            .action((yaclScreen, buttonOption) -> {

                            })
                            .build());

                }
                else {
                    songpacksBuilder.option(ButtonOption.createBuilder()
                            .name(Text.literal(songpackZip.config.name))
                            .description(
                                    OptionDescription.createBuilder()
                                            .text(Text.literal(songpackZip.config.description + "\n\nCredits:\n" + songpackZip.config.credits))
                                            .build()
                            )

                            .available(!isLoaded)

                            .text(Text.literal(isLoaded ? "Loaded" : "Load"))


                            .action((yaclScreen, buttonOption) -> {
                                setActiveSongpack(songpackZip, false);
                                ReactiveMusic.refreshSongpack();
                                MinecraftClient.getInstance().setScreen(ModConfig.createScreen(parent));
                            })



                            .build());
                }

            }


            builder.category(songpacksBuilder.build());





            builder
                    .title(Text.literal("Reactive Music"))


                    .category(ConfigCategory.createBuilder()
                            .name(Text.literal("General"))

                            .option(Option.<MusicDelayLength>createBuilder()
                                    .name(Text.literal("Music Delay Length"))
                                    .binding(defaults.musicDelayLength, () -> config.musicDelayLength, newVal -> config.musicDelayLength = newVal )
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MusicDelayLength.class))

                                    .build())

                            .build())


                    .category(ConfigCategory.createBuilder()
                            .name(Text.literal("Debug"))
                            .tooltip(Text.literal("Any debug tools useful for songpack creators or developers"))


                            .option(Option.<Boolean>createBuilder()
                                    .name(Text.literal("Debug Mode Enabled"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Text.literal("Enables some developer functionality such as always switching between songs when events change."))
                                            .build())
                                    .binding(defaults.debugModeEnabled, () -> config.debugModeEnabled, newVal -> config.debugModeEnabled = newVal )
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())




                            .build())




            .build();





            return builder;

        })).generateScreen(parent);
    }





    public static void setActiveSongpack(SongpackZip zip, boolean embeddedMode) {

        if (embeddedMode) {
            getConfig().loadedUserSongpack = "";
            ReactiveMusic.LOGGER.info("Loading embedded songpack!");
        }
        else {
            getConfig().loadedUserSongpack = zip.config.name;
            ReactiveMusic.LOGGER.info("Loading songpack: " + zip.config.name);
        }

        GSON.save();

        SongLoader.setActiveSongpack(zip, embeddedMode);


    }



}


