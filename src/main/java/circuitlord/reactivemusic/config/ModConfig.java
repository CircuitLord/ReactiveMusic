package circuitlord.reactivemusic.config;


import circuitlord.reactivemusic.ReactiveMusicCore;
import circuitlord.reactivemusic.ReactiveMusicState;
import circuitlord.reactivemusic.impl.songpack.RMSongpackLoader;
import circuitlord.reactivemusic.impl.songpack.RMSongpackZip;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;

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
    public MusicDelayLength musicDelayLength2 = MusicDelayLength.SONGPACK_DEFAULT;

    @SerialEntry
    public MusicSwitchSpeed musicSwitchSpeed2 = MusicSwitchSpeed.SONGPACK_DEFAULT;

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

    @SerialEntry
    public HashMap<String, Vec3d> savedHomePositions = new HashMap<>();

    @SerialEntry
    public List<String> soundsMuteMusic = new ArrayList<>();

    @SerialEntry
    public boolean hasForcedInitialVolume = false;







    public static Screen createScreen(Screen parent) {

        //SongLoader.fetchAvailableSongpacks();
        RMSongpackLoader.fetchAvailableSongpacks();

        return YetAnotherConfigLib.create(ModConfig.GSON, ((defaults, config, builder) -> {



            var songpacksBuilder = ConfigCategory.createBuilder();
            songpacksBuilder.name(Text.literal("Songpacks"));


            //boolean arIsLoaded = Objects.equals(ReactiveMusic.currentSongpack.config.name, "Adventure Redefined");

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

                boolean isLoaded = false;

                if (ReactiveMusicState.currentSongpack != null) {

                    isLoaded = Objects.equals(ReactiveMusicState.currentSongpack.getConfig().name, songpackZip.config.name);
                }

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


                    boolean allowedToShowErrors =
                            getConfig().debugModeEnabled ||
                            songpackZip.isv05OldSongpack ||
                            (songpackZip.path != null && !songpackZip.path.toString().endsWith(".zip"));


                    if (allowedToShowErrors && !songpackZip.errorString.isEmpty()) {
                        name = "WARNING: " + name;
                        description = "Encountered warnings while loading:\n\n" + songpackZip.errorString + "----------\n\n" + description;
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
                                    .binding(defaults.musicDelayLength2, () -> config.musicDelayLength2, newVal -> config.musicDelayLength2 = newVal )
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MusicDelayLength.class))
                                    .description(
                                            OptionDescription.createBuilder()
                                                    .text(Text.literal("Defines how much silence there should be between songs playing.\n\n" +
                                                            "SONGPACK_DEFAULT will use values recommended by the songpack creator."))
                                                    .build()
                                    )

                                    .build())

                            .option(Option.<MusicSwitchSpeed>createBuilder()
                                    .name(Text.literal("Music Switch Speed"))
                                    .binding(defaults.musicSwitchSpeed2, () -> config.musicSwitchSpeed2, newVal -> config.musicSwitchSpeed2 = newVal )
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MusicSwitchSpeed.class))
                                    .description(
                                            OptionDescription.createBuilder()
                                                    .text(Text.literal("Defines how long before a song fades out when it's event becomes invalid.\n\n" +
                                                            "SONGPACK_DEFAULT will use values recommended by the songpack creator."))
                                                    .build()
                                    )

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
                                            .text(Text.literal("Enables songpack developer functionality.\n" +
                                                    "- Always immediately switch between songs when events change.\n" +
                                                    "- Always display all songpack loading errors in the menu.\n"))
                                            .build())
                                    .binding(defaults.debugModeEnabled, () -> config.debugModeEnabled, newVal -> config.debugModeEnabled = newVal )
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())




                            .build())




            .build();





            return builder;

        })).generateScreen(parent);
    }





    public static void setActiveSongpack(RMSongpackZip songpack) {

        if (songpack.embedded) {
            getConfig().loadedUserSongpack = "";
        }
        else {
            getConfig().loadedUserSongpack = songpack.config.name;
        }

        GSON.save();

        ReactiveMusicCore.setActiveSongpack(songpack);

    }


    public static <E extends Enum<E>> Function<Option<E>, ControllerBuilder<E>> getEnumDropdownControllerFactory(ValueFormatter<E> formatter) {
        return opt -> EnumDropdownControllerBuilder.create(opt).formatValue(formatter);
    }

}


