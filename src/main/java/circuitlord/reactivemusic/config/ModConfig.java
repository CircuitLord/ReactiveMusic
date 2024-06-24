package circuitlord.reactivemusic.config;


import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongLoader;
import circuitlord.reactivemusic.SongpackZip;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.gui.controllers.BooleanController;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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




    public static Screen createScreen(Screen parent) {

        return YetAnotherConfigLib.create(ModConfig.GSON, ((defaults, config, builder) -> {

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
                                    .binding(defaults.debugModeEnabled, () -> config.debugModeEnabled, newVal -> config.debugModeEnabled = newVal )
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())




                            .build())




            .build();


            var songpacksBuilder = ConfigCategory.createBuilder();
            songpacksBuilder.name(Text.literal("Songpacks"));

/*            songpacksBuilder.option(Option.<String>createBuilder()
                    .name(net.minecraft.network.chat.literal("String Dropdown"))
                    .binding(
                            defaults.stringOptions,
                            () -> config.stringOptions,
                            (value) -> config.stringOptions = value
                    )
                    .controller(opt -> DropdownStringControllerBuilder.create(opt)
                            .values("Apple", "Banana", "Cherry", "Date")
                            .
                    )
                    .build())*/

            for (var songpackZip : SongLoader.availableSongpacks) {

                songpacksBuilder.option(ButtonOption.createBuilder()
                        .name(Text.literal(songpackZip.config.name))
                        .description(
                                OptionDescription.createBuilder()
                                        .text(Text.literal("Cool songpack"))
                                        .build()
                        )


                        .action((yaclScreen, buttonOption) -> {
                            setActiveSongpack(songpackZip);
                            ReactiveMusic.refreshSongpack();
                        })


                        .build());


            }


            builder.category(songpacksBuilder.build());



            return builder;

        })).generateScreen(parent);
    }





    public static void setActiveSongpack(SongpackZip zip) {
        SongLoader.setActiveSongpack(zip, false);


    }



}


