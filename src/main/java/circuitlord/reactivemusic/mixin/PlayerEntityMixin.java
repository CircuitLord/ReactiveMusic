package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.SongPicker;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        if ((Object)this instanceof ClientPlayerEntity) {
            ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
            World world = player.getWorld();
            LOGGER.info("Player attacked: {} at time: {}", player.getName().getString(), world.getTime());
            SongPicker.lastCombatTime = world.getTime();
        }
    }
    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        World playerWorld = player.getWorld();
        // cause of source
        if (source.getAttacker() != null) {
            SongPicker.lastCombatTime = playerWorld.getTime();
        }
    }
}