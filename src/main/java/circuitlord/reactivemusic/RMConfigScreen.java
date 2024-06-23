package circuitlord.reactivemusic;

import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.config.ModConfigProps;
import circuitlord.reactivemusic.config.MusicDelayLength;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class RMConfigScreen {


    public static Screen buildScreen(Screen parent) {
        final ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("reactive_music.title"));

        final ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        final ConfigCategory generalCategory = builder.getOrCreateCategory(Text.translatable("reactive_music.config.category.general"));
        final ConfigCategory debugCategory = builder.getOrCreateCategory(Text.translatable("reactive_music.config.category.debug"));
        //final ConfigCategory visualCategory = builder.getOrCreateCategory(Text.translatable("reactive_music.config.category.visual"));





        generalCategory.addEntry(entryBuilder.startEnumSelector(Text.translatable("reactive_music.config.option.music_delay"), MusicDelayLength.class, ModConfig.props.musicDelay)
                //.setDefaultValue(true)
                //.setTooltip(Text.translatable("reactive_music.config.option.debug_mode_enabled.tooltip"))
                //.setRequirement(CompatUtils::isBlockFeatureModLoaded)
                .setSaveConsumer(ModConfig::setMusicDelay)
                .build());



        //generalCategory.addEntry(entryBuilder.startStrList(Text.literal("test"), ModConfig.props.testList)
        //        .build());




        debugCategory.addEntry(entryBuilder.startBooleanToggle(Text.literal("Debug Mode Enabled"), ModConfig.props.isDebugModeEnabled)
                //.setDefaultValue(true)
                //.setTooltip(Text.translatable("reactive_music.config.option.debug_mode_enabled.tooltip"))
                //.setRequirement(CompatUtils::isBlockFeatureModLoaded)
                .setSaveConsumer(ModConfig::setDebugModeEnabled)
                .build());






        builder.setSavingRunnable(ModConfig::saveConfig);

        return builder.build();
    }
}