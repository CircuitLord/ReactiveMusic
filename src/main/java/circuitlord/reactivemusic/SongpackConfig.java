package circuitlord.reactivemusic;

import java.nio.file.Path;

public class SongpackConfig {

    public String name;
    public String version = "";
    public String author = "";
    public String description = "";

    public String credits = "";

    public SongpackEntry[] entries;


    // Not part of config file
    public Path configPath;

    public String errorString = "";

    public boolean blockLoading = false;





}


