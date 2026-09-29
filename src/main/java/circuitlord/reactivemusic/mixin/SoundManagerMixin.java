package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.ReactiveMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void play(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {

        String path = soundInstance.getIdentifier().getPath();

        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && ReactiveMusic.printSoundEvents) {
            mc.player.sendSystemMessage(Component.literal("[ReactiveMusic]: Sound: " + path + " Attenuation: " + soundInstance.getAttenuation()));
        }

        if (path.contains("music_disc")) {
            ReactiveMusic.trackSoundMuteMusic(soundInstance, false);
        }

        // cobblemon resource pack uses:
        //"battle.pvn.default"
        //"battle.pvp.default"
        //"battle.pvw.default"
        else if (path.contains("battle.pv")) {
            ReactiveMusic.trackSoundMuteMusic(soundInstance, false);

            ReactiveMusic.LOGGER.info("Detected cobblemon battle event, adding to list!");
        }

        for (String muteSound : ReactiveMusic.config.soundsMuteMusic) {
            if (path.contains(muteSound)) {
                ReactiveMusic.trackSoundMuteMusic(soundInstance, false);
                break;
            }
        }

        for (String muteSound : ReactiveMusic.config.soundsMuteMusicIgnoreDistance) {
            if (path.contains(muteSound)) {
                ReactiveMusic.trackSoundMuteMusic(soundInstance, true);
                break;
            }
        }
    }
}
