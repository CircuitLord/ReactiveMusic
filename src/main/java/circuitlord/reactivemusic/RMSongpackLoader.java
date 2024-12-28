package circuitlord.reactivemusic;

import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class RMSongpackLoader {


    public static List<SongpackZip> availableSongpacks = new ArrayList<>();

    public static MusicPackResource getInputStream(Path dirPath, String fileName, boolean embedded) {

        MusicPackResource resource = new MusicPackResource();

        // quick-path for embedded files
        if (embedded) {
            resource.inputStream = RMSongpackLoader.class.getResourceAsStream("/musicpack/" + fileName);

            return resource;
        }

        if (dirPath == null) {
            ReactiveMusic.LOGGER.error("dirpath was null");
            return null;
        }

        // Check if path is a zip file
        if (Files.isRegularFile(dirPath) && dirPath.toString().endsWith(".zip")) {

            Map<String, String> env = new HashMap<>();
            env.put("create", "false");

            FileSystem fs = null;

            try {
                fs = FileSystems.newFileSystem(dirPath, env);

            } catch (IOException e) {
                ReactiveMusic.LOGGER.error("Failed while loading file from zip " + e.getMessage());

                return null;
            }

            Path filePath = fs.getPath(fileName);

            if (Files.exists(filePath)) {
                try {
                    resource.inputStream = Files.newInputStream(filePath);
                    resource.fileSystem = fs;

                    return resource;
                } catch (IOException e) {
                    ReactiveMusic.LOGGER.error("Failed while creating inputstream from zip " + e.getMessage());
                    return null;
                }
            }


        } else {

            // Handle normal directory
            Path filePath = dirPath.resolve(fileName);
            if (Files.exists(filePath)) {
                try {
                    resource.inputStream = Files.newInputStream(filePath);

                    return resource;
                } catch (IOException e) {
                    ReactiveMusic.LOGGER.error(e.toString());
                }
            } else {
                ReactiveMusic.LOGGER.error("Couldn't find file! " + filePath);
            }
        }

        return null;

    }


    public static void fetchAvailableSongpacks() {

        long startTime = System.currentTimeMillis();

        availableSongpacks.clear();

        // First load the embedded songpack
        availableSongpacks.add(loadSongpack(null, true));

        // then load any user songpacks
        var gamePath = FabricLoader.getInstance().getGameDir();
        Path resourcePacksPath = gamePath.resolve("resourcepacks");


        List<Path> potentialPacks = new ArrayList<Path>();


        try {
            Files.walkFileTree(resourcePacksPath,  EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
                    potentialPacks.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            ReactiveMusic.LOGGER.error("Failed while visiting potential packs " + e.getMessage());
        }

        for (var packPath : potentialPacks) {
            SongpackZip songpackZip = loadSongpack(packPath, false);

            if (songpackZip == null)
                continue;

            availableSongpacks.add(songpackZip);
        }

        ReactiveMusic.LOGGER.info("Took " + (System.currentTimeMillis() - startTime) + "ms to parse available songpacks, found " + availableSongpacks.size() + "!");

    }


    public static SongpackZip loadSongpack(Path songpackPath, boolean embedded) {

        SongpackZip songpackZip = new SongpackZip();
        songpackZip.path = songpackPath;
        songpackZip.embedded = embedded;

        var configResource = getInputStream(songpackPath, "ReactiveMusic.yaml", embedded);
        if (configResource == null || configResource.inputStream == null) {
            return null;
        }

        Yaml yaml = new Yaml();

        try {
            songpackZip.config = yaml.loadAs(configResource.inputStream, SongpackConfig.class);
        }
        catch (Exception e) {
            songpackZip.config = new SongpackConfig();
            songpackZip.config.name = songpackPath.getFileName().toString();
            songpackZip.errorString = e.toString() + "\n\n";
            songpackZip.blockLoading = true;

            ReactiveMusic.LOGGER.error("Failed to load properties! Embedded=" + embedded + " Exception:" + e.toString());
        }

        if (!Constructor.errorString.isEmpty()) {
            songpackZip.errorString += Constructor.errorString;
        }

        if (Constructor.blockLoading) {
            songpackZip.blockLoading = true;
        }


        verifySongpackZip(songpackZip);

        if (!songpackZip.blockLoading) {
            songpackZip.runtimeEntries = getRuntimeEntries(songpackZip);
        }

        return songpackZip;

    }

    private static List<RMRuntimeEntry> getRuntimeEntries(SongpackZip songpackZip) {

        List<RMRuntimeEntry> runtimeEntries = new ArrayList<>();

        for (var entry : songpackZip.config.entries) {

            if (entry == null)
                continue;

            RMRuntimeEntry runtimeEntry = RMRuntimeEntry.create(songpackZip.config.name, entry);

            if (!runtimeEntry.errorString.isEmpty()) {
                songpackZip.errorString += runtimeEntry.errorString;

                continue;
            }

            if (runtimeEntry.conditions.isEmpty())
                continue;

            runtimeEntries.add(runtimeEntry);

        }

        return runtimeEntries;

    }

    // any extra verification
    public static void verifySongpackZip(SongpackZip songpackZip) {


        if (songpackZip.config == null || songpackZip.config.entries == null) {
            songpackZip.errorString += "Entries are null or not formatted correctly! Make sure you indent each entry with a TAB.\n\n";
            songpackZip.blockLoading = true;

            return;
        }

        // Check if all the songs are valid
        for (int i = 0; i < songpackZip.config.entries.length; i++) {

            if (songpackZip.config.entries[i] == null)
                continue;

            for (int j = 0; j < songpackZip.config.entries[i].songs.length; j++) {

                String song = songpackZip.config.entries[i].songs[j];

                var inputStream = getInputStream(songpackZip.path, "music/" + song + ".mp3", songpackZip.embedded);

                if (inputStream == null) {
                    String eventName = "";
                    for (int k = 0; k < songpackZip.config.entries[i].events.length; k++) {
                        eventName += songpackZip.config.entries[i].events[k].toString();
                    }

                    songpackZip.errorString += "Failed finding song: \"" +  songpackZip.config.entries[i].songs[j] + "\" for event: \"" + eventName + "\"\n\n";

                    try {
                        inputStream.close();
                    } catch (Exception e) {
                    }
                }
            }


        }

    }


}
