package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.ReactiveMusicDebug;
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

        String path = soundInstance.getId().getPath();

        if (path.contains("music_disc")) {
            ReactiveMusic.trackedSoundsMuteMusic.add(soundInstance);
        }

        // cobblemon resource pack uses:
        //"battle.pvn.default"
        //"battle.pvp.default"
        //"battle.pvw.default"
        else if (path.contains("battle.pv")) {
            ReactiveMusic.trackedSoundsMuteMusic.add(soundInstance);

            ReactiveMusicDebug.LOGGER.info("Detected cobblemon battle event, adding to list!");
        }


        for (String muteSound : ReactiveMusic.modConfig.soundsMuteMusic) {
            if (path.contains(muteSound)) {
                ReactiveMusic.trackedSoundsMuteMusic.add(soundInstance);
                break;
            }
        }


    }

}