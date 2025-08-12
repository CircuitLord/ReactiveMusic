package circuitlord.reactivemusic.impl.eventsys;

import circuitlord.reactivemusic.api.eventsys.EventRecord;

public class RMEventRecord implements EventRecord {
        
        private String eventId;
        private RMPluginIdentifier pluginId;
        
        public RMEventRecord(String eventId, RMPluginIdentifier pluginId) {
            this.eventId = eventId;
            this.pluginId = pluginId;
        }
        
        public String getEventId() { return eventId; }
        public RMPluginIdentifier getPluginId() { return pluginId; }
        
    }
