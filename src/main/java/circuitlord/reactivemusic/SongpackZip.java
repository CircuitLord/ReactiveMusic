package circuitlord.reactivemusic;

import java.nio.file.Path;
import java.util.List;

public class SongpackZip {

    public SongpackConfig config;


    public List<RMRuntimeEntry> runtimeEntries;


    public Path path;

    public String errorString = "";
    public boolean blockLoading = false;

    // backwards compat
    public boolean convertBiomeToBiomeTag = false;

    public boolean embedded = false;

}
