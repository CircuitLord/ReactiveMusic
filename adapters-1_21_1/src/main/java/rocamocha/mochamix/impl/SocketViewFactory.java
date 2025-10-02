package rocamocha.mochamix.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import rocamocha.mochamix.api.minecraft.MinecraftEntity;
import rocamocha.mochamix.api.minecraft.MinecraftEntity.MinecraftLivingEntity;
import rocamocha.mochamix.api.minecraft.MinecraftItemStack;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

import rocamocha.mochamix.impl.entity.EntitySocket;
import rocamocha.mochamix.impl.entity.LivingEntitySocket;
import rocamocha.mochamix.impl.item.ItemStackSocket;
import rocamocha.mochamix.impl.player.PlayerSocket;
import rocamocha.mochamix.impl.vector3.Vector3Socket;
import rocamocha.mochamix.impl.world.WorldSocket;

/**
 * ServiceLoader-backed factory that wires sockets for Minecraft 1.21.x.
 * This is done in a separate module to avoid classloading issues on other versions.
 * @see rocamocha.mochamix.api.io.MinecraftView
 * 
 * We can use this class as the implementation of the ViewFactory interface,
 * which is loaded via ServiceLoader in the main API class.
 * 
 */
public final class SocketViewFactory implements ViewFactory {
    @Override public MinecraftPlayer createPlayer(PlayerEntity player) { return new PlayerSocket(player);}
    @Override public MinecraftWorld createWorld(World world) { return new WorldSocket(world); }

    @Override public MinecraftEntity createEntity(Entity entity) { return new EntitySocket(entity);}
    @Override public MinecraftLivingEntity createLivingEntity(Entity entity) { return new LivingEntitySocket(entity);}
    @Override public MinecraftLivingEntity createLivingEntity(LivingEntity entity) { return new LivingEntitySocket(entity);}
    
    @Override public MinecraftItemStack createItemStack(ItemStack stack) { return new ItemStackSocket(stack); }

    @Override public MinecraftVector3 createPosition(BlockPos position) { return new Vector3Socket(position); }
    @Override public MinecraftVector3 createPosition(Vec3d vector) { return new Vector3Socket(vector);}
}
