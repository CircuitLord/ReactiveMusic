// src/main/java/rocamocha/mochamix/runtime/EntityView.java
package rocamocha.mochamix.impl.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;

import rocamocha.mochamix.api.minecraft.*;
import rocamocha.mochamix.api.minecraft.util.MinecraftIdentity;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.impl.common.IdentityAdapter;
import rocamocha.mochamix.impl.player.PlayerSocket;
import rocamocha.mochamix.api.io.MinecraftView;

/**
 * Socket-backed view wrapper for Entity.
 * Provides access to common entity properties and methods.
 * This is the base class for more specific entity types like LivingEntitySocket and ItemEntitySocket,
 * somewhat mirroring the subclassing of Minecraft's Entity class.
 * 
 * In the subclassing hierarchy, this class' subclasses implement more specific interfaces,
 * where the calls to asNative() are cast to the more specific native types.
 * This avoids the need for downcasting in user code, and keeps the casting logic centralized.
 * 
 * References to the native entity are kept in this base class only, reducing duplication,
 * and avoiding the need for multiple references in subclasses.
 * @see rocamocha.mochamix.api.minecraft.MinecraftEntity
 */
public class EntitySocket implements MinecraftEntity {
    protected final Entity e;
    protected final IdentityAdapter identity;
    
    public EntitySocket(Entity e) {
        this.identity = new IdentityAdapter(e);
        this.e = e;
    }
    
    @Override public Entity asNative() { return e; }
    @Override public MinecraftIdentity identity() { return identity; }
    
    @Override public boolean isLivingEntity() { return e.isLiving(); }
    @Override public boolean isItemEntity() { return e instanceof ItemEntity; }
    @Override public boolean isPlayerEntity() { return e instanceof PlayerEntity; }
    
    @Override public MinecraftLivingEntity asLiving() { return new LivingEntitySocket((LivingEntity) e); }
    @Override public MinecraftItemEntity asItem() { return new ItemEntitySocket((ItemEntity) e); }
    @Override public MinecraftPlayer asPlayer() { return new PlayerSocket((PlayerEntity) e); }
    
    @Override public java.util.UUID uuid() { return e.getUuid(); }
    @Override public String typeId() { return Registries.ENTITY_TYPE.getId(e.getType()).toString(); }
    @Override public boolean onGround() { return e.isOnGround(); }
    @Override public MinecraftWorld world() { return MinecraftView.of(e.getWorld()); }
    @Override public MinecraftVector3 pos() { return MinecraftView.of(e.getPos()); }
    @Override public MinecraftVector3 velocity() { return MinecraftView.of(e.getVelocity()); }
}
