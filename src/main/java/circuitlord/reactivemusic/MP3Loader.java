package circuitlord.reactivemusic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MP3Loader {

    public static InputStream loadMP3FromZip(Path zipFilePath, String mp3FileName) throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put("create", "false");

        FileSystem fs = FileSystems.newFileSystem(zipFilePath, env);
        Path mp3Path = fs.getPath(mp3FileName);

        if (Files.exists(mp3Path)) {
            return Files.newInputStream(mp3Path);
        } else {
            throw new IOException("MP3 file not found in the zip: " + mp3FileName);
        }
    }

/*    public static void main(String[] args) {
        Path zipFilePath = Path.of("path/to/your/musicpack.zip");
        String mp3FileName = "music/song.mp3";

        try (InputStream mp3Stream = loadMP3FromZip(zipFilePath, mp3FileName)) {
            // Use the mp3Stream as needed
            System.out.println("MP3 file loaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}