package circuitlord.reactivemusic.api.songpack;

import java.nio.file.Path;
import java.util.List;

import circuitlord.reactivemusic.impl.songpack.RMSongpackConfig;

public interface SongpackZip {
    boolean isEmbedded();
    Path getPath();
    String getErrorString();
    void setErrorString(String s);
    List<RuntimeEntry> getEntries();

    String getName();
    String getAuthor();
    RMSongpackConfig getConfig();
}