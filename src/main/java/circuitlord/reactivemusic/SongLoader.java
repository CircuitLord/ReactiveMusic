	package circuitlord.reactivemusic;

    import net.fabricmc.loader.api.FabricLoader;
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

                // Just reload it from the resources
                activeSongpack = loadSongpackConfig(null, true);
                activeSongpackPath = null;
            }
            else {
                activeSongpack = songpackZip.config;
                activeSongpackPath = songpackZip.path;
            }

            activeSongpackEmbedded = embeddedMode;
        }


        public static SongpackConfig loadSongpackConfig(Path configPath, boolean embeddedMode) {
            SongpackConfig songpack = null;

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
                ReactiveMusic.LOGGER.error("Failed to load properties! Embedded=" + embeddedMode + " Exception:" + e.toString());
            }

            if (songpack != null) {
                // Load the IDs
                for (int i = 0; i < songpack.entries.length; i++) {
                    if (songpack.entries[i] == null) continue;

                    songpack.entries[i].id = i;
                }
            }


            return songpack;
        }



        public static SongResource getStream(String songName) {

            if (activeSongpack == null) return null;

            if(songName == null || songName.equals("null"))
                return null;

            SongResource songRes = new SongResource();

            // embedded, use resources
            if (activeSongpackEmbedded) {
                String path = "/musicpack/music/" + songName + ".mp3";

                songRes.inputStream = SongLoader.class.getResourceAsStream(path);

            }

            // Folder in resource packs (not zipped, can just read directly)
            else if (activeSongpackPath.toFile().isDirectory()) {

                Path songPath = activeSongpackPath.resolve("music").resolve(songName + ".mp3");

                if (Files.exists(songPath)) {
                    try {
                       songRes.inputStream = new FileInputStream(songPath.toFile());
                    } catch (FileNotFoundException e) {
                        ReactiveMusic.LOGGER.error("Failed to load song file " + songName);
                    }
                }

            }

            // Zipped file in resource packs
            else {
                Map<String, String> env = new HashMap<>();
                env.put("create", "false");

                FileSystem fs = null;

                try {
                    fs = FileSystems.newFileSystem(activeSongpackPath, env);

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
                    }
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
