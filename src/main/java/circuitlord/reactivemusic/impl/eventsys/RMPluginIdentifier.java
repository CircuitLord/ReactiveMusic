package circuitlord.reactivemusic.impl.eventsys;

import circuitlord.reactivemusic.api.eventsys.PluginIdentifier;

/**
 * Identifier class for the plugin registry.
 * Will be set as the value within the registry's map.
 */
public class RMPluginIdentifier implements PluginIdentifier{

    private String title;
    private String namespace;
    private String path;

    public RMPluginIdentifier(String ns, String p) {
        this.namespace = ns;
        this.path = p;
    }
    
    public String getNamespace() { return namespace; }
    public String getPath() { return path; }
    public String getId() { return namespace + ":" + path; }
    
    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title == null ? getId() : title; }

}
