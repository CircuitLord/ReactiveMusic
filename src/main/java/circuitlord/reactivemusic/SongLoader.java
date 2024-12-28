/*
	package circuitlord.reactivemusic;

    import net.fabricmc.loader.api.FabricLoader;
    import net.minecraft.world.biome.Biome;
    import org.yaml.snakeyaml.Yaml;

    import java.io.*;
    import java.nio.file.*;
    import java.nio.file.attribute.BasicFileAttributes;
    import java.util.*;

    public final class SongLoader {

        public static SongpackConfig activeSongpack = null;
        public static Path activeSongpackPath = null;

        public static boolean activeSongpackEmbedded = false;


        public static List<SongpackZip> availableSongpacks = new ArrayList<SongpackZip>();


        public static void fetchAvailableSongpacks() {


            long startTime = System.currentTimeMillis();

            var gamePath = FabricLoader.getInstance().getGameDir();
            Path resourcePacksPath = gamePath.resolve("resourcepacks");


            availableSongpacks.clear();

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



                SongpackConfig config = null;
                Path configPath = null;

                if (Files.isDirectory(packPath)) {
                    configPath = packPath.resolve("ReactiveMusic.yaml");

                    if (Files.exists(configPath)) {
                        config = loadSongpackConfig(configPath, false);
                    }
                }
                else {
                    Map<String, String> env = new HashMap<>();
                    env.put("create", "false");

                    try (FileSystem fs = FileSystems.newFileSystem(packPath, env)) {
                        configPath = fs.getPath("ReactiveMusic.yaml");

                        if (Files.exists(configPath)) {
                            config = loadSongpackConfig(configPath, false);
                        }

                    } catch (Exception e) {
                        ReactiveMusic.LOGGER.error("Failed while loading potential packs " + e.getMessage());
                    }
                }

                if (config != null) {
                    
                    // Verify the songs


                    if (config.entries == null) {
                        config.errorString += "Entries are null or not formatted correctly! Make sure you indent each entry with a TAB.\n\n";
                        config.blockLoading = true;
                    }
                    else {
                        for (int i = 0; i < config.entries.length; i++) {

                            if (config.entries[i] == null || config.entries[i].songs == null)
                                continue;

                            for (int j = 0; j < config.entries[i].songs.length; j++) {

                                SongResource songRes = getStream(packPath, false, config.entries[i].songs[j]);

                                if (songRes == null) {

                                    String eventName = "";

                                    for (int k = 0; k < config.entries[i].events.length; k++) {
                                        eventName += config.entries[i].events[k].toString();
                                    }

                                    config.errorString += "Failed finding song: \"" + config.entries[i].songs[j] + "\" for event: \"" + eventName + "\"\n\n";
                                }

                            }
                        }
                    }

                    
                    SongpackZip zip = new SongpackZip();
                    zip.path = packPath;
                    zip.config = config;

                    availableSongpacks.add(zip);
                }
                
            }


            ReactiveMusic.LOGGER.info("Took " + (System.currentTimeMillis() - startTime) + "ms to parse available songpacks, found " + availableSongpacks.size() + "!");



        }


        public static void setActiveSongpack(SongpackZip songpackZip, boolean embeddedMode) {

            if (embeddedMode) {
                ReactiveMusic.LOGGER.info("Loading embedded songpack!");

                // Just reload it from the resources
                activeSongpack = loadSongpackConfig(null, true);
                activeSongpackPath = null;
            }
            else {
                ReactiveMusic.LOGGER.info("Loading songpack: " + songpackZip.config.name);

                activeSongpack = songpackZip.config;
                activeSongpackPath = songpackZip.path;
            }

            if (!SongLoader.activeSongpack.errorString.isEmpty()) {
                ReactiveMusic.LOGGER.error("ERRORS while loading songpack:\n\n" + SongLoader.activeSongpack.errorString);
            }

            activeSongpackEmbedded = embeddedMode;
        }


        public static SongpackConfig loadSongpackConfig(Path configPath, boolean embeddedMode) {
            SongpackConfig songpack = new SongpackConfig();

            songpack.configPath = configPath;

            Yaml yaml = new Yaml();

            try {

                InputStream inputStream;

                if (embeddedMode) {
                    inputStream = SongLoader.class.getResourceAsStream("/musicpack/ReactiveMusic.yaml");
                }
                else {
                    // This only works because we have a file stream open to the zip when we call this
                    // ugly but it's fine i guess
                    inputStream = Files.newInputStream(configPath);
                }

                // TODO: add better logging when this fails
                songpack = yaml.loadAs(inputStream, SongpackConfig.class);

            }
            catch (Exception e) {

                songpack.name = configPath.getName(configPath.getNameCount() - 2).toString();
                songpack.errorString = e.toString() + "\n\n";
                songpack.blockLoading = true;

                //ReactiveMusic.LOGGER.error("Failed to load properties! Embedded=" + embeddedMode + " Exception:" + e.toString());
            }


            if (songpack.entries != null) {
                // Load the IDs
                for (int i = 0; i < songpack.entries.length; i++) {
                    if (songpack.entries[i] == null) continue;

                    songpack.entries[i].id = i;

                    // Loop over all events and expand them out into songpack events and biome tag events
                    for (int j = 0; j < songpack.entries[i].events.length; j++) {

                        String val = songpack.entries[i].events[j].toLowerCase();
                        if (val.isEmpty()) continue;


                        // try to figure out if it's a biome=
                        if (val.startsWith("biome=")) {
                            String biomeTagName = val.substring(6);

                            biomeTagName = CleanBiomeTagString(biomeTagName);

                            // remove the IS_ to match the check below
                            //biomeTagName = biomeTagName.replace("is_", "");

                            boolean foundTag = false;

                            for (int k = 0; k < SongPicker.BIOME_TAG_FIELDS.length; k++) {

                                // i love creating GC
                                String fieldName = SongPicker.BIOME_TAG_FIELDS[k].getName();

                                // TODO: cache this in song picker?
                                fieldName = CleanBiomeTagString(fieldName);

                                if (fieldName.equals(biomeTagName)) {

                                    var biomeTag = SongPicker.getBiomeTagFromField(SongPicker.BIOME_TAG_FIELDS[k]);

                                    if (biomeTag != null) {
                                        songpack.entries[i].biomeTagEvents.add(biomeTag);

                                        foundTag = true;
                                        break;
                                    }
                                }
                            }

                            // go to next event
                            if (foundTag) continue;
                        }

                        // last case -- try casting to songpack event enum
                        else {

                            try {
                                // try to cast to SongpackEvent
                                // needs upcase for enum names
                                SongpackEventType eventType = Enum.valueOf(SongpackEventType.class, val.toUpperCase());

                                // it's a songpack event
                                if (eventType != SongpackEventType.NONE) {
                                    songpack.entries[i].songpackEvents.add(eventType);

                                    continue;
                                }
                            } catch (Exception e) {
                                //e.printStackTrace();
                            }
                        }

                        songpack.errorString += "Unknown event type: " + val + "\n\n";

                    }

                    // done checking all events

                    // TODO: still allow loading if not all events are found (old versions may not have same biome tags or changed names)
                    // remove if no events were found

                }
            }




            return songpack;
        }


        public static String CleanBiomeTagString(String input) {
            input = input.toLowerCase();

            // handle cases where the start of the tag changed
            input = input.replace("is_", "");
            input = input.replace("in_", "");
            input = input.replace("climate_", "");

            // converting to 1.21 format with biometag v2
            input = input.replace("tree_coniferous", "coniferous_tree");

            return input;

        }




        public static SongResource getStream(Path songpackPath, boolean embedded, String songName) {

            //if (songpackPath == null) return null;

            if(songName == null || songName.isEmpty() || songName.equals("null"))
                return null;

            SongResource songRes = new SongResource();

            // embedded, use resources
            if (embedded) {
                String path = "/musicpack/music/" + songName + ".mp3";

                songRes.inputStream = SongLoader.class.getResourceAsStream(path);

            }

            // Folder in resource packs (not zipped, can just read directly)
            else if (songpackPath.toFile().isDirectory()) {

                Path songPath = songpackPath.resolve("music").resolve(songName + ".mp3");

                if (Files.exists(songPath)) {
                    try {
                       songRes.inputStream = new FileInputStream(songPath.toFile());
                    } catch (FileNotFoundException e) {
                        ReactiveMusic.LOGGER.error("Failed to load song file " + songName);
                        return null;
                    }
                }
                else {
                    return null;
                }

            }

            // Zipped file in resource packs
            else {
                Map<String, String> env = new HashMap<>();
                env.put("create", "false");

                FileSystem fs = null;

                try {
                    fs = FileSystems.newFileSystem(songpackPath, env);

                } catch (IOException e) {
                    ReactiveMusic.LOGGER.error("Failed while loading song file from zip " + e.getMessage());

                    return null;
                }

                Path songPath = fs.getPath("music", songName + ".mp3");

                if (Files.exists(songPath)) {
                    try {
                        songRes.inputStream = Files.newInputStream(songPath);
                        songRes.fileSystem = fs;
                    } catch (IOException e) {
                        ReactiveMusic.LOGGER.error("Failed while creating inputstream from zip " + e.getMessage());
                        return null;
                    }
                }
                else {
                    return null;
                }
            }

            return songRes;

        }

        private static String joinTokensExceptFirst(String[] tokens) {
            String s = "";
            int i = 0;
            for(String token : tokens) {
                i++;
                if(i == 1)
                    continue;
                s += token;
            }
            return s;
        }
    }
*/
