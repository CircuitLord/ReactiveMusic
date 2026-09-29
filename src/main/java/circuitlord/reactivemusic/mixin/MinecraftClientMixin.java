package circuitlord.reactivemusic.mixin;


import circuitlord.reactivemusic.ReactiveMusic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    public LocalPlayer player;

    @Shadow
    public ClientLevel level;

    @Inject(method = "tick", at = @At("RETURN"))
    private void reactivemusic$tick(CallbackInfo ci) {

        ReactiveMusic.newTick();

    }



}
