package circuitlord.reactivemusic.impl.songpack;

import circuitlord.reactivemusic.config.MusicDelayLength;
import circuitlord.reactivemusic.config.MusicSwitchSpeed;

public class RMSongpackConfig {

    public String name;
    public String version = "";
    public String author = "";
    public String description = "";

    public String credits = "";

    public MusicDelayLength musicDelayLength = MusicDelayLength.NORMAL;

    public MusicSwitchSpeed musicSwitchSpeed = MusicSwitchSpeed.NORMAL;

    public RMSongpackEntry[] entries;


}


