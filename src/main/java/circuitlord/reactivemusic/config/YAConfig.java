package circuitlord.reactivemusic.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class YAConfig {

    //public static final GsonConfigInstance<YAConfig> INSTANCE = new GsonConfigInstance<>(YAConfig.class, FabricLoader.getInstance().getConfigDir().resolve("adaptive-tooltips.json"), GsonBuilder::setPrettyPrinting);




    public static boolean myBooleanOption = false;

    public static Screen buildScreen(Screen parent) {


/*        YetAnotherConfigLib.create(INSTANCE, (defaults, config, builder) -> {


            var categoryBuilder = ConfigCategory.createBuilder()
                    .name(Text.translatable("adaptivetooltips.title"));



        }).generateScreen();*/


        var test = YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Reactive Music"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("General"))
                        //.tooltip(Text.literal("This text will appear as a tooltip when you hover or focus the button with Tab. There is no need to add \n to wrap as YACL will do it for you."))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Name of the group"))
                                .description(OptionDescription.of(Text.literal("This text will appear when you hover over the name or focus on the collapse button with Tab.")))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Boolean Option"))
                                        .description(OptionDescription.of(Text.literal("This text will appear as a tooltip when you hover over the option.")))
                                        .binding(true, () -> myBooleanOption, newVal -> myBooleanOption = newVal)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Debug"))
                        .tooltip(Text.literal("Any debug tools useful for songpack creators or developers"))


                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Debug Mode Enabled"))
                                .binding(false, () -> myBooleanOption, newVal -> myBooleanOption = newVal )
                                .controller(TickBoxControllerBuilder::create)
                                .build())




                        .build())




                .build();


        return test.generateScreen(parent);
    }



}
