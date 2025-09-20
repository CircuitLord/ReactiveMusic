package rocamocha.mochamix.impl.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import rocamocha.mochamix.api.minecraft.MinecraftEntity.MinecraftLivingEntity;
import rocamocha.mochamix.mixin.accessor.LivingEntityAccess;

public class LivingEntitySocket extends EntitySocket implements MinecraftLivingEntity {

    @Override public LivingEntity asNative() { return (LivingEntity) super.asNative(); }

    /**
     * Creates a new LivingEntitySocket for the given entity,
     * using a cast to LivingEntity.
     * This constructor is used when we only have an Entity reference.
     */
    public LivingEntitySocket(Entity e) {
        super(e);
    }

    /**
     * Creates a new LivingEntitySocket for the given entity.
     * This constructor is separate to avoid casting when we already have a LivingEntity.
     */
    public LivingEntitySocket(LivingEntity e) {
        super(e);
    }

    @Override public float health() { return asNative().getHealth(); }
    @Override public float maxHealth() { return asNative().getMaxHealth(); }
    @Override public boolean dead() { return asNative().isDead(); }
    @Override public boolean alive() { return asNative().isAlive(); }

    /**
     * Return the current target of this entity, if any.
     * This method tries multiple approaches to find the entity's target:
     * 1. getAttacking() - Returns entity currently being attacked
     * 2. For MobEntity, tries to access AI target via getTarget()
     * 3. For PathAwareEntity, tries getTarget() method
     * @return The target entity, or null if none.
     */
    @Nullable
    @Override public MinecraftLivingEntity aggroTarget() {
        // First try the basic getAttacking method
        LivingEntity target = asNative().getAttacking();
        if (target != null) {
            return new LivingEntitySocket(target);
        }
        
        // Try mob-specific targeting methods
        if (asNative() instanceof MobEntity mobEntity) {
            // MobEntity has a getTarget() method that returns the current attack target
            LivingEntity mobTarget = mobEntity.getTarget();
            if (mobTarget != null) {
                return new LivingEntitySocket(mobTarget);
            }
        }

        if (asNative() instanceof PathAwareEntity pathAwareEntity) {
            // PathAwareEntity also has targeting capabilities
            LivingEntity pathTarget = pathAwareEntity.getTarget();
            if (pathTarget != null) {
                return new LivingEntitySocket(pathTarget);
            }
        }
        
        return null;
    }

    /**
     * Return the entity that last attacked this entity, if any.
     * This method tries multiple approaches to find the attacker:
     * 1. getAttacker() - Returns the last entity that dealt damage within 100 ticks
     * 2. lastDamageSource.getAttacker() - More detailed damage source information
     * 3. lastDamageSource.getSource() - Direct damage source (for projectiles, etc.)
     * @return The attacking entity, or null if none.
     */
    @Nullable
    @Override public MinecraftLivingEntity attackedBy() {
        // First try the standard getAttacker method
        LivingEntity attacker = asNative().getAttacker();
        if (attacker != null) {
            return new LivingEntitySocket(attacker);
        }
        
        // Try to get more detailed damage information via mixin accessor
        try {
            // Check if the mixin is available and working
            if (asNative() instanceof LivingEntityAccess livingEntityAccess) {
                DamageSource lastDamageSource = livingEntityAccess.mocha$getLastDamageSource();
                if (lastDamageSource != null) {
                    // First check for a direct attacker (living entity that caused damage)
                    Entity sourceAttacker = lastDamageSource.getAttacker();
                    if (sourceAttacker instanceof LivingEntity livingAttacker) {
                        return new LivingEntitySocket(livingAttacker);
                    }
                    
                    // If no direct attacker, check the damage source (for projectiles, etc.)
                    Entity damageSource = lastDamageSource.getSource();
                    if (damageSource instanceof LivingEntity livingSource) {
                        return new LivingEntitySocket(livingSource);
                    }
                    
                    // For indirect damage, still return the attacker even if not living
                    if (sourceAttacker != null) {
                        // This might be a non-living entity like a projectile
                        // We could potentially trace back to its owner, but for now return null
                        // as our interface only supports MinecraftLivingEntity
                    }
                }
            }
        } catch (Exception e) {
            // Mixin accessor failed, fall back to basic method
            // This is expected if mixins aren't working properly
        }
        
        return null;
    }
    
    /**
     * Get detailed damage source information for debugging purposes.
     * This is not part of the MinecraftLivingEntity interface but can be used
     * by debug commands to show more detailed information.
     * @return String describing the last damage source, or null if none
     * 
     * TODO: Consider expanding this to include more details like damage amount, whether it was magic, etc.
     * TODO: Refactor this to use the DamageSourceAdapter class for consistency and ease of maintenance?
     * TODO: Implement a way to retrieve historical damage sources for more context.
     */
    @Nullable
    public String getLastDamageSourceInfo() {
        try {
            // Check if the mixin is available and working
            if (asNative() instanceof LivingEntityAccess livingEntityAccess) {
                DamageSource lastDamageSource = livingEntityAccess.mocha$getLastDamageSource();
                if (lastDamageSource != null) {
                    StringBuilder info = new StringBuilder();
                    
                    // Damage type
                    String damageType = lastDamageSource.getType().toString();
                    info.append("type=").append(damageType);
                    
                    // Attacker info
                    Entity attacker = lastDamageSource.getAttacker();
                    if (attacker != null) {
                        String attackerType = attacker.getType().toString();
                        info.append(", attacker=").append(attackerType);
                    } else {
                        info.append(", attacker=<none>");
                    }
                    
                    // Source info (for projectiles, etc.)
                    Entity source = lastDamageSource.getSource();
                    if (source != null && source != attacker) {
                        String sourceType = source.getType().toString();
                        info.append(", source=").append(sourceType);
                    }
                    
                    return info.toString();
                }
            } else {
                return "mixin not available";
            }
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
        return null;
    }
}
