package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.ReactiveMusic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void play(SoundInstance soundInstance, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {

        String path = soundInstance.getId().getPath();


        MinecraftClient mc = MinecraftClient.getInstance();


        if (mc.player != null && ReactiveMusic.printSoundEvents) {
            mc.player.sendMessage(Text.of("[ReactiveMusic]: Sound: " + path + " Attenuation: " + soundInstance.getAttenuationType()), false);
        }

        if (path.contains("music_disc")) {
            ReactiveMusic.trackedSoundsMuteMusic.add(soundInstance);
        }

        // cobblemon resource pack uses:
        //"battle.pvn.default"
        //"battle.pvp.default"
        //"battle.pvw.default"
        else if (path.contains("battle.pv")) {
            ReactiveMusic.trackedSoundsMuteMusic.add(soundInstance);

            ReactiveMusic.LOGGER.info("Detected cobblemon battle event, adding to list!");
        }


        for (String muteSound : ReactiveMusic.config.soundsMuteMusic) {
            if (path.contains(muteSound)) {
                ReactiveMusic.trackedSoundsMuteMusic.add(soundInstance);
                break;
            }
        }


    }

}