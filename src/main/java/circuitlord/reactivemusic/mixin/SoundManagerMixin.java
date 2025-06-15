package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.ReactiveMusic;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "Lnet/minecraft/client/sound/SoundManager;play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void play(SoundInstance soundInstance, CallbackInfo ci) {
        //if (soundInstance.getCategory() == SoundCategory.MUSIC) {
            //ReactiveMusic.musicInstanceList.add(soundInstance);
        //} else
        if (soundInstance.getId().getPath().contains("music_disc")) {
            ReactiveMusic.musicDiscInstanceList.add(soundInstance);

/*            if (ReactiveMusic.CONFIG.pauseForDiscMusic) {
                MinecraftClient.getInstance().getSoundManager().stop(InfiniteMusic.musicInstance);
            }*/
        }
        else if (soundInstance.getId().getPath().contains("cobblemon") && soundInstance.getId().getPath().contains("battle")) {
            ReactiveMusic.musicDiscInstanceList.add(soundInstance);

            ReactiveMusic.LOGGER.info("Detected cobblemon battle event, adding to list!");
        }
    }

}