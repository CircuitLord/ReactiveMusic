package rocamocha.mochamix.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin using prefixed methods to avoid name conflicts.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccess {
    @Accessor("lastDamageSource") DamageSource mocha$getLastDamageSource();
}