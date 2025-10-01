package circuitlord.reactivemusic.impl.songpack;

import java.io.InputStream;
import java.nio.file.FileSystem;

public class MusicPackResource {

    public FileSystem fileSystem = null;

    public InputStream inputStream = null;

    public void close() throws Exception {
        inputStream.close();
        fileSystem.close();
    }


}
