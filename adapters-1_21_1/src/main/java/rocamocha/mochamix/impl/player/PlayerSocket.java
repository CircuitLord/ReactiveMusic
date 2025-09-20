// src/main/java/rocamocha/mochamix/runtime/PlayerView.java
package rocamocha.mochamix.impl.player;

import net.minecraft.entity.player.PlayerEntity;

import rocamocha.mochamix.api.minecraft.*;
import rocamocha.mochamix.impl.player.adapter.*;

/**
 * Socket-backed view wrapper for PlayerEntity.
 * @see rocamocha.mochamix.api.minecraft.MinecraftPlayer
 */
public class PlayerSocket implements MinecraftPlayer {

    protected final PlayerEntity player;
    @Override public PlayerEntity asNative() { return player; }
    
    private final PlayerGamemodeAdapter gamemode;
    private final PlayerHungerAdapter hunger;
    private final PlayerExperienceAdapter experience;
    private final PlayerLocationAdapter location;
    private final PlayerMotionAdapter motion;
    private final PlayerActivityAdapter activity;
    private final PlayerSurroundingsAdapter surroundings;
    private final PlayerMountAdapter mount;
    private final PlayerInventoryAdapter inventory;

    public PlayerSocket(PlayerEntity player) {
        this.player = player;
        this.gamemode = new PlayerGamemodeAdapter(player);
        this.hunger = new PlayerHungerAdapter(player);
        this.experience = new PlayerExperienceAdapter(player);
        this.location = new PlayerLocationAdapter(player);
        this.motion = new PlayerMotionAdapter(player);
        this.activity = new PlayerActivityAdapter(player);
        this.surroundings = new PlayerSurroundingsAdapter(player);
        this.mount = new PlayerMountAdapter(player);
        this.inventory = new PlayerInventoryAdapter(player);
    }


    // composite views
    @Override public Gamemode gamemode() { return gamemode; }
    @Override public Hunger hunger() { return hunger; }
    @Override public Experience experience() { return experience; }
    @Override public Location location() { return location; }
    @Override public Motion motion() { return motion; }
    @Override public Activity activity() { return activity; }
    @Override public Surroundings surroundings() { return surroundings; }
    @Override public Mount mount() { return mount; }
    @Override public Inventory inventory() { return inventory; }

    // identity
    @Override public java.util.UUID uuid() { return player.getUuid(); }
    @Override public String name() { return player.getName().getString(); }
    @Override public String team() {
        var team = player.getScoreboardTeam();
        return team == null ? null : team.getName();
    }

    // status
    @Override public float health() { return player.getHealth(); }
    @Override public float maxHealth() { return player.getMaxHealth(); }
}
