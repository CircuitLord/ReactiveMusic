package circuitlord.reactivemusic.mixin;


import circuitlord.reactivemusic.ReactiveMusic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    public ClientPlayerEntity player;

    @Shadow
    public ClientWorld world;

   // @Unique
   // private static int dynmus$tickCounter = 0;

    @Inject(method = "tick", at = @At("RETURN"))
    private void bettermusic$tick(CallbackInfo ci) {
        //dynmus$tickCounter++;

        //if (dynmus$tickCounter >= 40 && world != null && player != null) {
        //    dynmus$tickCounter = 0;

            ReactiveMusic.tick();
        //}
    }



}
