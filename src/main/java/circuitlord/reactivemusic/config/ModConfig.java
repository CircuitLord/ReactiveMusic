package circuitlord.reactivemusic.config;


import circuitlord.reactivemusic.RMScreen;
import circuitlord.reactivemusic.RMSongpackLoader;
import circuitlord.reactivemusic.ReactiveMusic;
//import circuitlord.reactivemusic.SongLoader;
import circuitlord.reactivemusic.SongpackZip;
import com.google.gson.GsonBuilder;
import com.terraformersmc.modmenu.ModMenu;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.gui.controllers.BooleanController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.isxander.yacl3.platform.YACLPlatform.getConfigDir;


public class ModConfig {

    public static final ValueFormatter<Formatting> FORMATTING_FORMATTER = formatting -> Text.literal(StringUtils.capitalize(formatting.getName().replaceAll("_", " ")));


    public static ModConfig getConfig() {
        return GSON.instance();
    }

    public static void saveConfig() {
        GSON.save();
    }

    public static final ConfigClassHandler<ModConfig> GSON = ConfigClassHandler.createBuilder(ModConfig.class)
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(getConfigDir().resolve("ReactiveMusic.json5"))
                    .setJson5(true)
                    //.appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                    .build())
            .build();



    @SerialEntry
    public MusicDelayLength musicDelayLength = MusicDelayLength.SONGPACK_DEFAULT;

    @SerialEntry
    public MusicSwitchSpeed musicSwitchSpeed = MusicSwitchSpeed.SONGPACK_DEFAULT;

    @SerialEntry
    public boolean debugModeEnabled = false;

    //@SerialEntry
    //public boolean treatAsWhitelist = false;

    //@SerialEntry
    //public double confirmationResetDelay = 1.0;

    @SerialEntry
    public String loadedUserSongpack = "";

    @SerialEntry
    public List<String> blacklistedDimensions = new ArrayList<>();





    public static Screen createScreen(Screen parent) {

        //SongLoader.fetchAvailableSongpacks();
        RMSongpackLoader.fetchAvailableSongpacks();

        return YetAnotherConfigLib.create(ModConfig.GSON, ((defaults, config, builder) -> {



            var songpacksBuilder = ConfigCategory.createBuilder();
            songpacksBuilder.name(Text.literal("Songpacks"));


            boolean arIsLoaded = Objects.equals(ReactiveMusic.currentSongpack.config.name, "Adventure Redefined");

/*            songpacksBuilder.option(ButtonOption.createBuilder()
                    .name(Text.literal("Adventure Redefined (Default)"))
                    .description(
                            OptionDescription.createBuilder()
                                    .text(Text.literal("The included songpack with Reactive Music."))
                                    .build()
                    )

                    .available(!arIsLoaded)
                    .text(Text.literal(arIsLoaded ? "Loaded" : "Load"))
                    .action((yaclScreen, buttonOption) -> {
                        setActiveSongpack(RMSongpackLoader.availableSongpacks.getFirst(), true);
                        ReactiveMusic.refreshSongpack();
                        MinecraftClient.getInstance().setScreen(ModConfig.createScreen(parent));
                    })

                    .build());*/


            for (var songpackZip : RMSongpackLoader.availableSongpacks) {

                boolean isLoaded = Objects.equals(ReactiveMusic.currentSongpack.config.name, songpackZip.config.name);


                if (songpackZip.blockLoading) {
                    songpacksBuilder.option(ButtonOption.createBuilder()
                            .name(Text.literal("FAILED LOADING: " + songpackZip.config.name))
                            .description(
                                    OptionDescription.createBuilder()
                                            .text(Text.literal("Failed to load songpack:\n\n" + songpackZip.errorString))
                                            .build()
                            )

                            .available(false)
                            .text(Text.literal(""))
                            .action((yaclScreen, buttonOption) -> {

                            })
                            .build());

                }
                else {

                    String name = songpackZip.config.name;
                    String description = songpackZip.config.description + "\n\nCredits:\n" + songpackZip.config.credits;

                    if (!songpackZip.errorString.isEmpty()) {
                        name = "ERRORS: " + name;
                        description = "Encountered errors while loading:\n\n" + songpackZip.errorString + "----------\n\n" + description;
                    }

                    songpacksBuilder.option(ButtonOption.createBuilder()
                            .name(Text.literal(name))
                            .description(
                                    OptionDescription.createBuilder()
                                            .text(Text.literal(description))
                                            .build()
                            )

                            .available(!isLoaded)

                            .text(Text.literal(isLoaded ? "Loaded" : "Load"))


                            .action((yaclScreen, buttonOption) -> {
                                setActiveSongpack(songpackZip);
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

                            .option(Option.<MusicSwitchSpeed>createBuilder()
                                    .name(Text.literal("Music Switch Speed"))
                                    .binding(defaults.musicSwitchSpeed, () -> config.musicSwitchSpeed, newVal -> config.musicSwitchSpeed = newVal )
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MusicSwitchSpeed.class))

                                    .build())


/*
                            .option(Option.<MusicDelayLength>createBuilder()
                                    .name(Text.literal("Enum Dropdown"))
                                    .binding(
                                            defaults.musicDelayLength,
                                            () -> config.musicDelayLength,
                                            (value) -> config.musicDelayLength = value
                                    )
                                    .controller(option -> EnumDropdownControllerBuilder.create(option).formatValue(formatting -> Text.literal(StringUtils.capitalize(formatting.toString()).replaceAll("_", " "))))
                                    .build())


*/



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





    public static void setActiveSongpack(SongpackZip songpack) {

        if (songpack.embedded) {
            getConfig().loadedUserSongpack = "";
        }
        else {
            getConfig().loadedUserSongpack = songpack.config.name;
        }

        GSON.save();

        ReactiveMusic.setActiveSongpack(songpack);

    }


    public static <E extends Enum<E>> Function<Option<E>, ControllerBuilder<E>> getEnumDropdownControllerFactory(ValueFormatter<E> formatter) {
        return opt -> EnumDropdownControllerBuilder.create(opt).formatValue(formatter);
    }

}


