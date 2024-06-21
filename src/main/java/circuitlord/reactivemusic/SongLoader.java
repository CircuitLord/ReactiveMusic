	package circuitlord.reactivemusic;

    import org.yaml.snakeyaml.Yaml;

    import java.io.*;

    public final class SongLoader {

        public static File mainDir;

        public static boolean enabled = false;
        public static boolean embeddedMode = true;


        public static SongpackConfig activeSongpack;

        public static void loadFrom(File f) {



            SongpackConfig songpack = null;

            Yaml yaml = new Yaml();



            try {

                // Load from resources
                if (embeddedMode) {
                    InputStream inputStream = SongLoader.class.getResourceAsStream("/musicpack/bettermusic.yaml");
                    songpack = yaml.loadAs(inputStream, SongpackConfig.class);
                }

                // TODO: always prioritize user songpack over embedded in the future?
                // Load from user songpack
                else {

/*                    configFile = new YamlFile(new File(f, "bettermusic.yaml"));

                    if (!configFile.exists()) {
                        configFile.createNewFile();
                    }

                    configFile.load();*/

                }
            }
            catch (Exception e) {
                ReactiveMusic.LOGGER.error("Failed to load properties! Embedded=" + embeddedMode + " Exception:" + e.toString());
            }

            if (songpack == null) return;

            enabled = songpack.enabled;

            if (!enabled) return;


            activeSongpack = songpack;

/*            File musicDir = new File(f, "music");
            if(!musicDir.exists())
                musicDir.mkdir();

            mainDir = musicDir;*/
        }

        class EventInstanceConfig {
            public String[] keys;

            public String[] songs;
        }


        public static void initConfig(File f) {
            try {
                f.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                writer.write("# BetterMusic Config\n");
                //writer.write("enabled: true\n");
                writer.write("embedded: true\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static InputStream getStream() {
            if(PlayerThread.currentSong == null || PlayerThread.currentSong.equals("null"))
                return null;

            if (embeddedMode) {
                String path = "/musicpack/music/" + PlayerThread.currentSong + ".mp3";
                //System.out.println(SongLoader.class.getResource(path));

                InputStream stream = SongLoader.class.getResourceAsStream(path);

                return stream;
            }
            else {
                File f = new File(mainDir, PlayerThread.currentSong + ".mp3");
                if(f.getName().equals("null.mp3"))
                    return null;

                try {
                    return new FileInputStream(f);
                } catch (FileNotFoundException e) {
                    ReactiveMusic.LOGGER.error("File " + f + " not found. Fix your config!");
                    e.printStackTrace();
                    return null;
                }
            }

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
