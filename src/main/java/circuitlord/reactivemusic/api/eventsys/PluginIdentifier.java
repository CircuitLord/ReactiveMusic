package circuitlord.reactivemusic.api.eventsys;

public interface PluginIdentifier {
    String getNamespace();
    String getPath();
    String getId();

    void setTitle(String title);
}
