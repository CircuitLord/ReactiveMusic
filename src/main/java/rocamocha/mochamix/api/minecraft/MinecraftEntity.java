package rocamocha.mochamix.api.minecraft;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import rocamocha.mochamix.impl.NativeAccess;

public interface MinecraftEntity extends NativeAccess {

    /**
     * Cast to living entity if possible.
     * This is a safe cast, but will throw if not a living entity.
     * You can use {@link #isLivingEntity()} to check first if needed.
     * 
     * This is contained in this interface to avoid the need for
     * downcasting in user code.
     * 
     * @throws ClassCastException if not a living entity
     * @return
     */
    default MinecraftLivingEntity asLiving() {
        if (!isLivingEntity()) throw new ClassCastException("Not a living entity");
        return (MinecraftLivingEntity) this;
    }

    UUID uuid();
    String typeId();       // e.g. "minecraft:zombie"
    
    boolean isLivingEntity();
    boolean onGround();
    
    MinecraftWorld world();      // e.g. "minecraft:overworld"
    
    MinecraftVector3 pos();
    MinecraftVector3 velocity();

    interface MinecraftLivingEntity extends MinecraftEntity {
        float health();
        float maxHealth();
        boolean dead();
        boolean alive();

        @Nullable
        MinecraftLivingEntity aggroTarget();

        @Nullable
        MinecraftLivingEntity attackedBy();
    }

    interface MinecraftItemEntity extends MinecraftEntity {
        int age();
        MinecraftItemStack itemStack();
    }
}