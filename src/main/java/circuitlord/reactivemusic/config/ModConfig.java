package circuitlord.reactivemusic.config;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongLoader;
import circuitlord.reactivemusic.SongpackConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    public static ModConfigProps props;


    public static void setMusicDelay(MusicDelayLength newDelay) {
        props.musicDelay = newDelay;
    }


    public static void setDebugModeEnabled(boolean enable) {
        props.isDebugModeEnabled = enable;
    }


    public static void loadConfig() {

        Yaml yaml = new Yaml();

        props = yaml.loadAs(getConfigStream(), ModConfigProps.class);

        if (props == null) {
            props = new ModConfigProps();
            // Gen a new config
            saveConfig();
        }

    }


    public static void saveConfig() {
        //MinecraftClient.getInstance().reloadResources();

        Yaml yaml = new Yaml();

        String saveString = yaml.dumpAs(props, Tag.YAML, DumperOptions.FlowStyle.BLOCK);

        try {
            Files.write(getConfigFile().toPath(), saveString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {

            ReactiveMusic.LOGGER.warn("Failed to save config!");
        }

    }





    public static File getConfigFile() {

        var file = FabricLoader.getInstance().getConfigDir().resolve("ReactiveMusic.yaml").toFile();

        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                if (!file.createNewFile()) {
                    throw new IOException();
                }
            } catch(IOException | SecurityException e) {
                ReactiveMusic.LOGGER.warn("Failed to create config file! " + e.getMessage());
            }
        }


        return file;
    }


    public static InputStream getConfigStream() {

        var file = getConfigFile();

        InputStream stream;

        try {
            stream = Files.newInputStream(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return stream;

    }
}


