package rocamocha.mochamix.impl.entity.adapter;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.damage.DamageSource;
import rocamocha.mochamix.api.minecraft.MinecraftEntity;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import rocamocha.mochamix.impl.entity.EntitySocket;
import rocamocha.mochamix.impl.vector3.Vector3Socket;

public class DamageSourceAdapter {
    protected final DamageSource damageSource;

    public DamageSourceAdapter(DamageSource damageSource) {
        this.damageSource = damageSource;
    }

    public DamageSource asNative() { return damageSource; }

    @Nullable
    public MinecraftEntity source() {
        return damageSource.getSource() != null ? new EntitySocket(damageSource.getSource()) : null;
    }

    @Nullable
    public MinecraftEntity attacker() {
        return damageSource.getAttacker() != null ? new EntitySocket(damageSource.getAttacker()) : null;
    }

    @Nullable
    public MinecraftVector3 position() {
        return damageSource.getPosition() != null ? new Vector3Socket(damageSource.getPosition()) : null;
    }

}
