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
import rocamocha.mochamix.api.minecraft.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

/**
 * Contract for version-specific modules to supply socket-backed view wrappers.
 * Implementations are discovered via ServiceLoader.
 * Uses Object parameters to avoid version-specific dependencies in the interface.
 * Implementations cast to appropriate Minecraft types internally.
 * @see MinecraftView
 */
public interface ViewFactory {
    // From native Minecraft objects to MochaMix API
    MinecraftPlayer createPlayer(PlayerEntity playerEntity);
    MinecraftEntity createEntity(Entity entity);
    MinecraftLivingEntity createLivingEntity(Entity livingEntity);
    MinecraftLivingEntity createLivingEntity(LivingEntity livingEntity);
    MinecraftWorld createWorld(World world);
    MinecraftVector3 createPosition(BlockPos position);
    MinecraftVector3 createPosition(Vec3d position); // BlockPos, Vec3d, Vec3i, etc.
    MinecraftItemStack createItemStack(ItemStack itemStack);
}
