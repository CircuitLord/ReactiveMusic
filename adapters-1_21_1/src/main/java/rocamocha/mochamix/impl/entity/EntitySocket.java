// src/main/java/rocamocha/mochamix/runtime/EntityView.java
package rocamocha.mochamix.impl.entity;

import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;

import rocamocha.mochamix.api.minecraft.*;
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
    public EntitySocket(Entity e) { this.e = e; }
    @Override public Entity asNative() { return e; }
    @Override public MinecraftLivingEntity asLiving() { return new LivingEntitySocket(e); }

    @Override public boolean isLivingEntity() { return e.isLiving(); }

    @Override public java.util.UUID uuid() { return e.getUuid(); }
    @Override public String typeId() { return Registries.ENTITY_TYPE.getId(e.getType()).toString(); }
    @Override public boolean onGround() { return e.isOnGround(); }
    @Override public MinecraftWorld world() { return MinecraftView.of(e.getWorld()); }
    @Override public MinecraftVector3 pos() { return MinecraftView.of(e.getPos()); }
    @Override public MinecraftVector3 velocity() { return MinecraftView.of(e.getVelocity()); }
}
