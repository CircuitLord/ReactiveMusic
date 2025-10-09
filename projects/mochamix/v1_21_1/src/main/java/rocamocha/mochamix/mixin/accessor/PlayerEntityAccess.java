// src/main/java/rocamocha/mochamix/mixin/accessor/PlayerEntityAccess.java
package rocamocha.mochamix.mixin.accessor;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin using prefixed methods to avoid name conflicts.
 */
@Mixin(PlayerEntity.class)
public interface PlayerEntityAccess {
    @Accessor("experienceLevel")    int mocha$getExperienceLevel();
    @Accessor("experienceProgress") float mocha$getExperienceProgress();
    @Accessor("totalExperience")    int mocha$getTotalExperience();
    @Accessor("fishHook")           FishingBobberEntity mocha$getFishHook();
}
