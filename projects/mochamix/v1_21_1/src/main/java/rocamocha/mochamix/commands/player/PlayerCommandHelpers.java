package rocamocha.mochamix.commands.player;

import rocamocha.reactivemusic.ReactiveMusicDebug.TextBuilder;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;
import net.minecraft.util.Formatting;

public class PlayerCommandHelpers {
    
    public static TextBuilder gamemodeInfo(TextBuilder info, MinecraftPlayer player) {
        info.line("gamemode",
            player.gamemode().isCreative() ? "creative" :
            player.gamemode().isSpectator() ? "spectator" :
            player.gamemode().isAdventure() ? "adventure" :
            player.gamemode().isSurvival() ? "survival" : "unknown", 
            Formatting.WHITE);
        return info;
    }

    public static TextBuilder statusInfo(TextBuilder info, MinecraftPlayer player) {
        String healthLv = String.format("%.1f / %.1f", player.health(), player.maxHealth());
        String hungerLv = Integer.toString(player.hunger().foodLevel());
        String saturationLv = Float.toString(player.hunger().saturation());
        
        info.line("health", healthLv, Formatting.RED);
        info.line("food", hungerLv + " (" + saturationLv + ")", Formatting.GOLD);
        return info;
    }

    public static TextBuilder positionInfo(TextBuilder info, MinecraftPlayer player) {
        MinecraftVector3.Vector3d blockPos = player.location().pos().asVec3d();
        String position = String.format(
            "%.1f %.1f %.1f",
            blockPos.xd(),
            blockPos.yd(),
            blockPos.zd()
        );
        
        info.line("position", position,Formatting.AQUA);
        return info;
    }
    
}
