package rocamocha.mochamix.impl.player.adapter;

import java.util.List;
import java.util.Objects;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer.Surroundings;
import rocamocha.mochamix.api.io.MinecraftView;
import rocamocha.mochamix.api.minecraft.MinecraftEntity;
import rocamocha.mochamix.api.minecraft.MinecraftEntity.MinecraftLivingEntity;;

public class PlayerSurroundingsAdapter implements Surroundings {
    private final PlayerEntity player;

    public PlayerSurroundingsAdapter(PlayerEntity player) {
        this.player = player;
    }

    @Override public boolean underwater() { return player.isSubmergedInWater(); }
    @Override public boolean inRain() { return !player.isTouchingWater() && player.isTouchingWaterOrRain(); }
    @Override public boolean inPowderedSnow() { return player.inPowderSnow; }

    /**
     * Get a list of nearby entities within a certain radius within a spherical area
     * around the player. This does not include the player themselves.
     */
    @Override public List<MinecraftEntity> nearbyEntities(double radius) {
        return player.getWorld().getOtherEntities((Entity) player, player.getBoundingBox().expand(radius))
            .stream().map(e -> {
                if (e.squaredDistanceTo(player) > radius * radius) return null;
                return MinecraftView.of(e);
            }
        ).filter(Objects::nonNull).toList();
    }

    @Override public List<MinecraftLivingEntity> nearbyLivingEntities(double radius) {
        return player.getWorld().getOtherEntities((Entity) player, player.getBoundingBox().expand(radius), Entity::isLiving)
            .stream().map(e -> {
                if (e.squaredDistanceTo(player) > radius * radius) return null;
                MinecraftEntity entity = MinecraftView.of(e);
                return entity != null ? entity.asLiving() : null;
            }
        ).filter(Objects::nonNull).toList();
    }

    @Override public List<MinecraftLivingEntity> nearbyHostileEntities(double radius) {
        return player.getWorld().getOtherEntities((Entity) player, player.getBoundingBox().expand(radius), 
                e -> e.isLiving() && e instanceof HostileEntity)
            .stream().map(e -> {
                if (e.squaredDistanceTo(player) > radius * radius) return null;
                MinecraftEntity entity = MinecraftView.of(e);
                return entity != null ? entity.asLiving() : null;
            }
        ).filter(Objects::nonNull).toList();
    }
}