// circuitlord/reactivemusic/ClientBootstrap.java
package circuitlord.reactivemusic;

import circuitlord.reactivemusic.api.ReactiveMusicUtils;
import circuitlord.reactivemusic.mixin.BossBarHudAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.CreditsScreen;

import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class ClientBootstrap {
    private ClientBootstrap() {}

    /** Called only on client to wire the delegate in ReactiveMusicUtils. */
    public static void install() {
        ReactiveMusicUtils.setClientDelegate(new ReactiveMusicUtils.ClientDelegate() {
            @Override public boolean isBossBarActive() {
                try {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc == null || mc.inGameHud == null) return false;
                    BossBarHud hud = mc.inGameHud.getBossBarHud();
                    Map<UUID, ClientBossBar> map = ((BossBarHudAccessor) hud).getBossBars();
                    return map != null && !map.isEmpty();
                } catch (Throwable t) { return false; }
            }
            @Override public boolean isMainMenu() {
                var mc = MinecraftClient.getInstance();
                return mc == null || mc.player == null || mc.world == null;
            }
            @Override public boolean isCredits() {
                var mc = MinecraftClient.getInstance();
                return mc != null && mc.currentScreen instanceof CreditsScreen;
            }
        });
    }
}
