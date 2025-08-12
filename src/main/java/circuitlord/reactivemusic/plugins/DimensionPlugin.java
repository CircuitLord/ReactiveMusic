package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.api.eventsys.EventRecord;
import circuitlord.reactivemusic.api.songpack.SongpackEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class DimensionPlugin extends ReactiveMusicPlugin {
    public DimensionPlugin() {
        super("reactivemusic", "dimension");
    }
    private static EventRecord OVERWORLD, NETHER, END;

    @Override
    public void init() {
        registerSongpackEvents("OVERWORLD", "NETHER", "END");

        OVERWORLD = SongpackEvent.get("OVERWORLD");
        NETHER    = SongpackEvent.get("NETHER");
        END       = SongpackEvent.get("END");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<EventRecord, Boolean> eventMap) {
        if (world == null) return;

        var indimension = world.getRegistryKey();
        SongPicker.currentDimName = indimension.getValue().toString();

        boolean isOverworld = indimension == World.OVERWORLD;
        boolean isNether    = indimension == World.NETHER;
        boolean isEnd       = indimension == World.END;

        eventMap.put(OVERWORLD, isOverworld);
        eventMap.put(NETHER,    isNether);
        eventMap.put(END,       isEnd);
    }
}
