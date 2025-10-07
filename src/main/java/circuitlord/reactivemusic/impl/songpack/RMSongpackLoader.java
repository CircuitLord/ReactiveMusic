package circuitlord.reactivemusic.impl.songpack;

import circuitlord.reactivemusic.ReactiveMusicDebug;

import circuitlord.reactivemusic.api.songpack.RuntimeEntry;
import net.fabricmc.loader.api.FabricLoader;
import org.rm_yaml.snakeyaml.Yaml;
import org.rm_yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class RMSongpackLoader {

    /**
    * Allows relative paths in the songpack, for folders other than "music"
    * this is useful for songpack makers who want to use a different folder structure
    * potentially for letting songpacks replace plugin assets as well, if the plugin dev
    * implements loading from the songpack
    */
    static String normalizeSongPath(String song) {
        String withExt = song.endsWith(".mp3") ? song : (song + ".mp3");
        // If caller provided a path (contains '/'), use it as-is.
        // Otherwise default to the "music/" subfolder.
        return (song.startsWith("/") || song.startsWith("\\")) ? withExt : ("music/" + withExt);
    }

    public static List<RMSongpackZip> availableSongpacks = new ArrayList<>();

    public static MusicPackResource getInputStream(Path dirPath, String fileName, boolean embedded) {
        MusicPackResource resource = new MusicPackResource();

        // quick-path for embedded files
        if (embedded) {
            resource.inputStream = RMSongpackLoader.class.getResourceAsStream("/musicpack/" + fileName);
            return resource;
        }

        if (dirPath == null) {
            ReactiveMusicDebug.LOGGER.error("dirpath was null");
            return null;
        }

        // Check if path is a zip file
        if (Files.isRegularFile(dirPath) && dirPath.toString().endsWith(".zip")) {
            Map<String, String> env = new HashMap<>();
            env.put("create", "false");

            try {
                FileSystem fs = FileSystems.newFileSystem(dirPath, env);
                Path filePath = fs.getPath(fileName);
                if (Files.exists(filePath)) {
                    resource.inputStream = Files.newInputStream(filePath);
                    resource.fileSystem = fs;
                    return resource;
                }
            } catch (IOException e) {
                ReactiveMusicDebug.LOGGER.error("Failed while loading file from zip " + e.getMessage());
                return null;
            }
        }

        // handle normal directories
        else {
            Path filePath = dirPath.resolve(fileName);
            if (Files.exists(filePath)) {
                try {
                    resource.inputStream = Files.newInputStream(filePath);
                    return resource;
                } catch (IOException e) {
                    ReactiveMusicDebug.LOGGER.error(e.toString());
                }
            } else {
                ReactiveMusicDebug.LOGGER.error("Couldn't find file! " + filePath);
            }
        }

        return null;
    }
    
    @SuppressWarnings("unchecked")
    private static RMSongpackConfig populateConfigFromMap(Map<String, Object> configMap) {
        RMSongpackConfig config = new RMSongpackConfig();
        
        // Populate top-level config properties
        if (configMap.containsKey("name")) config.name = (String) configMap.get("name");
        if (configMap.containsKey("description")) config.description = (String) configMap.get("description");
        if (configMap.containsKey("author")) config.author = (String) configMap.get("author");
        if (configMap.containsKey("version")) config.version = (String) configMap.get("version");
        
        // Process entries array
        if (configMap.containsKey("entries") && configMap.get("entries") instanceof List) {
            List<Map<String, Object>> entriesList = (List<Map<String, Object>>) configMap.get("entries");
            config.entries = new RMSongpackEntry[entriesList.size()];
            
            for (int i = 0; i < entriesList.size(); i++) {
                config.entries[i] = populateEntryFromMap(entriesList.get(i));
            }
        }
        
        return config;
    }
    
    @SuppressWarnings("unchecked")
    private static RMSongpackEntry populateEntryFromMap(Map<String, Object> entryMap) {
        RMSongpackEntry entry = new RMSongpackEntry();
        
        // Store all keys in entryMap first
        entry.entryMap.putAll(entryMap);
        
        // Populate known properties that exist in RMSongpackEntry
        if (entryMap.containsKey("events") && entryMap.get("events") instanceof List) {
            List<String> eventsList = (List<String>) entryMap.get("events");
            entry.events = eventsList.toArray(new String[0]);
        }
        if (entryMap.containsKey("songs") && entryMap.get("songs") instanceof List) {
            List<String> songsList = (List<String>) entryMap.get("songs");
            entry.songs = songsList.toArray(new String[0]);
        }
        
        // Handle zones array (this will also be stored in entryMap for external options)
        if (entryMap.containsKey("zones") && entryMap.get("zones") instanceof List) {
            List<String> zonesList = (List<String>) entryMap.get("zones");
            // Zones is not a standard property, so it stays only in entryMap for external options
        }
        if (entryMap.containsKey("allowFallback")) entry.allowFallback = (Boolean) entryMap.get("allowFallback");
        if (entryMap.containsKey("useOverlay")) entry.useOverlay = (Boolean) entryMap.get("useOverlay");
        if (entryMap.containsKey("forceStopMusicOnValid")) entry.forceStopMusicOnValid = (Boolean) entryMap.get("forceStopMusicOnValid");
        if (entryMap.containsKey("forceStopMusicOnInvalid")) entry.forceStopMusicOnInvalid = (Boolean) entryMap.get("forceStopMusicOnInvalid");
        if (entryMap.containsKey("forceStartMusicOnValid")) entry.forceStartMusicOnValid = (Boolean) entryMap.get("forceStartMusicOnValid");
        Object forceChance = entryMap.get("forceChance");
        if (forceChance instanceof Number) {
            entry.forceChance = ((Number) forceChance).floatValue();
        } else if (forceChance instanceof String) {
            try {
                String forceChanceStr = (String) forceChance;
                // Remove 'f' suffix if present
                if (forceChanceStr.endsWith("f") || forceChanceStr.endsWith("F")) {
                    forceChanceStr = forceChanceStr.substring(0, forceChanceStr.length() - 1);
                }
                entry.forceChance = Float.parseFloat(forceChanceStr);
            } catch (NumberFormatException e) {
                ReactiveMusicDebug.LOGGER.warn("Failed to parse forceChance value: " + forceChance + ", using default");
            }
        }
        if (entryMap.containsKey("stackable")) entry.stackable = (Boolean) entryMap.get("stackable");
        if (entryMap.containsKey("alwaysPlay")) entry.alwaysPlay = (Boolean) entryMap.get("alwaysPlay");
        if (entryMap.containsKey("alwaysStop")) entry.alwaysStop = (Boolean) entryMap.get("alwaysStop");
        
        return entry;
    }

    public static void fetchAvailableSongpacks() {
        long startTime = System.currentTimeMillis();
        availableSongpacks.clear();

        // Load embedded songpack
        availableSongpacks.add(loadSongpack(null, true, "ReactiveMusic.yaml"));

        // Load user songpacks
        Path resourcePacksPath = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
        List<Path> potentialPacks = new ArrayList<>();

        try {
            Files.walkFileTree(resourcePacksPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                    potentialPacks.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            ReactiveMusicDebug.LOGGER.error("Failed while visiting potential packs " + e.getMessage());
        }

        for (Path packPath : potentialPacks) {
            List<String> yamlFileNames = new ArrayList<>();

            // zip files
            if (Files.isRegularFile(packPath) && packPath.toString().endsWith(".zip")) {
                try (FileSystem fs = FileSystems.newFileSystem(packPath, (ClassLoader) null)) {

                    Path root = fs.getPath("/");

                    yamlFileNames = getYamlFiles(Files.list(root).toList());

                } catch (IOException e) {
                    ReactiveMusicDebug.LOGGER.error("Failed reading zip: " + e);
                    continue;
                }
            }

            // normal directories
            else if (Files.isDirectory(packPath)) {
                try {

                    yamlFileNames = getYamlFiles(Files.list(packPath).toList());

                } catch (IOException e) {
                    ReactiveMusicDebug.LOGGER.error("Failed reading directory: " + e);
                    continue;
                }
            }

            for (String yamlFile : yamlFileNames) {
                RMSongpackZip songpackZip = loadSongpack(packPath, false, yamlFile);
                if (songpackZip != null) {
                    availableSongpacks.add(songpackZip);
                }
            }
        }

        ReactiveMusicDebug.LOGGER.info("Took " + (System.currentTimeMillis() - startTime) + "ms to parse available songpacks, found " + availableSongpacks.size() + "!");
    }

    public static List<String> getYamlFiles(List<Path> paths) {

        List<String> found = new ArrayList<>();

        for (Path path : paths) {

            if (Files.isRegularFile(path)) {
                String filename = path.getFileName().toString();

                if (filename.toLowerCase().endsWith(".yaml")) {
                    found.add(filename);
                }
            }
        }

        return found;
    }


    // New version of loadSongpack with YAML file name
    public static RMSongpackZip loadSongpack(Path songpackPath, boolean embedded, String yamlFileName) {
        RMSongpackZip songpackZip = new RMSongpackZip();
        songpackZip.path = songpackPath;
        songpackZip.embedded = embedded;

        var configResource = getInputStream(songpackPath, yamlFileName, embedded);
        if (configResource == null || configResource.inputStream == null) {
            return null;
        }

        Yaml yaml = new Yaml();

        try {
            // Load as generic Map to capture all keys, then manually populate
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = yaml.load(configResource.inputStream);
            ReactiveMusicDebug.LOGGER.info("YAML loaded successfully, config keys: " + (configMap != null ? configMap.keySet() : "null"));
            songpackZip.config = populateConfigFromMap(configMap);
            ReactiveMusicDebug.LOGGER.info("Config populated successfully");
        } catch (Exception e) {
            songpackZip.config = new RMSongpackConfig();
            songpackZip.config.name = songpackPath != null ? songpackPath.getFileName().toString() : "Embedded";
            songpackZip.errorString = e.toString() + "\n\n";
            songpackZip.blockLoading = true;

            ReactiveMusicDebug.LOGGER.error("Failed to load properties! Embedded=" + embedded + " Exception:" + e.toString());
            e.printStackTrace();
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

    // Legacy call for default "ReactiveMusic.yaml"
    public static RMSongpackZip loadSongpack(Path songpackPath, boolean embedded) {
        return loadSongpack(songpackPath, embedded, "ReactiveMusic.yaml");
    }

    private static List<RuntimeEntry> getRuntimeEntries(RMSongpackZip songpackZip) {
        List<RuntimeEntry> runtimeEntries = new ArrayList<>();

        for (var entry : songpackZip.getConfig().entries) {
            if (entry == null) continue;

            RuntimeEntry runtimeEntry = new RMRuntimeEntry(songpackZip, entry);

            if (!runtimeEntry.getErrorString().isEmpty()) {
                songpackZip.setErrorString(songpackZip.getErrorString() + runtimeEntry.getErrorString());

                // allow it to keep loading if it passes the check below
                //continue;
            }

            if (runtimeEntry.getConditions().isEmpty()) continue;

            runtimeEntries.add(runtimeEntry);
        }

        return runtimeEntries;
    }

    public static void verifySongpackZip(RMSongpackZip songpackZip) {
        if (songpackZip.config == null || songpackZip.config.entries == null) {
            songpackZip.errorString += "Entries are null or not formatted correctly! Make sure you.\n\n";
            songpackZip.blockLoading = true;
            return;
        }

        for (int i = 0; i < songpackZip.config.entries.length; i++) {
            if (songpackZip.config.entries[i] == null) continue;

            if (songpackZip.config.entries[i].alwaysPlay || songpackZip.config.entries[i].alwaysStop) {
                songpackZip.errorString += "WARNING! You are using a songpack made for Reactive Music v0.5 and older, things may not work well!\n\n";
                songpackZip.convertBiomeToBiomeTag = true;
                songpackZip.isv05OldSongpack = true;
                break;
            }
        }

        // Check if all the songs are valid
        for (int i = 0; i < songpackZip.config.entries.length; i++) {
            if (songpackZip.config.entries[i] == null || songpackZip.config.entries[i].songs == null) continue;

            for (int j = 0; j < songpackZip.config.entries[i].songs.length; j++) {
                String song = songpackZip.config.entries[i].songs[j];
                var inputStream = getInputStream(songpackZip.path, normalizeSongPath(song), songpackZip.embedded);

                if (inputStream == null) {
                    StringBuilder eventName = new StringBuilder();
                    for (int k = 0; k < songpackZip.config.entries[i].events.length; k++) {
                        eventName.append(songpackZip.config.entries[i].events[k].toString());
                    }

                    songpackZip.errorString += "Failed finding song: \"" + song + "\" for event: \"" + eventName + "\"\n\n";
                } else {
                    try {
                        inputStream.close();
                    } catch (Exception ignored) {}
                }
            }
        }
    }

}
