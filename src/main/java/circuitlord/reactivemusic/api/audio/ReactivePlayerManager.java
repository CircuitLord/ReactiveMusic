package circuitlord.reactivemusic.api.audio;

import java.util.Collection;

public interface ReactivePlayerManager {
    // Factory
    ReactivePlayer create(String id, ReactivePlayerOptions opts);   // throws if id already exists

    // Lookup / control
    ReactivePlayer get(String id);                            // null if missing
    Collection<ReactivePlayer> getAll();
    Collection<ReactivePlayer> getByGroup(String group);

    // Cross-player ducking (optional, simple + predictable)
    void setGroupDuck(String group, float percent);     // multiplies each player’s duck layer
    float getGroupDuck(String group);

    // Lifecycle
    void closeAllForPlugin(String pluginNamespace);
    void closeAll();                                    // on shutdown

    void tick();
}