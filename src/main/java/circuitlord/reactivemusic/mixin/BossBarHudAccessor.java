package circuitlord.reactivemusic.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
@Mixin(BossHealthOverlay.class)
public interface BossBarHudAccessor {

    @Accessor("events")
    Map<UUID, LerpingBossEvent> getBossBars();

}
