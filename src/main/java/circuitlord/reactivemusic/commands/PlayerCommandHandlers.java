package circuitlord.reactivemusic.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import circuitlord.reactivemusic.ReactiveMusicDebug.TextBuilder;
import circuitlord.reactivemusic.api.ReactiveMusicAPI;
import circuitlord.reactivemusic.api.audio.GainSupplier;
import circuitlord.reactivemusic.api.audio.ReactivePlayer;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.util.Formatting;

public class PlayerCommandHandlers {

    public static int playerList(CommandContext<FabricClientCommandSource> ctx) {
        TextBuilder playerList = new TextBuilder();

        playerList.header("AUDIO PLAYERS");
        for (ReactivePlayer player : ReactiveMusicAPI.audioManager().getAll()) {
            playerList.line(player.id(), Formatting.AQUA);
        }
        
        ctx.getSource().sendFeedback(playerList.build());
        return 1;
    }

    public static int playerInfo(CommandContext<FabricClientCommandSource> ctx) {


        String id = StringArgumentType.getString(ctx, "namespace") + ":" + StringArgumentType.getString(ctx, "path");
        ReactivePlayer player = ReactiveMusicAPI.audioManager().get(id);
        TextBuilder playerInfo = new TextBuilder();

        playerInfo.header("AUDIO PLAYER INFO")

        .line("id", player.id(), Formatting.AQUA)
        .line("isPlaying", player.isPlaying() ? "YES" : "NO", player.isPlaying() ? Formatting.GREEN : Formatting.GRAY)
        .line("stopOnFadeOut", player.stopOnFadeOut() ? "YES" : "NO", player.stopOnFadeOut() ? Formatting.GREEN : Formatting.GRAY)
        .line("resetOnFadeOut", player.resetOnFadeOut() ? "YES" : "NO", player.resetOnFadeOut() ? Formatting.GREEN : Formatting.GRAY)
        .line("gainSuppliers", "", Formatting.WHITE);

        player.getGainSuppliers().forEach((supplierId, gainSupplier) -> {
            playerInfo.line(" --> " + supplierId, Float.toString(gainSupplier.supplyComputedPercent()), gainSupplier.supplyComputedPercent() > 0 ? Formatting.LIGHT_PURPLE : Formatting.GRAY);
        });
        
        ctx.getSource().sendFeedback(playerInfo.build());
        return 1;
    }

    public static int gainSupplierInfo(CommandContext<FabricClientCommandSource> ctx) {

        String id = StringArgumentType.getString(ctx, "namespace") + ":" + StringArgumentType.getString(ctx, "path");
        String gainSupplierId = StringArgumentType.getString(ctx, "gainSupplierId");
        TextBuilder supplierInfo = new TextBuilder();
        ReactivePlayer player = ReactiveMusicAPI.audioManager().get(id);
        GainSupplier gainSupplier = player.getGainSuppliers().get(gainSupplierId);

        supplierInfo.header("GAIN SUPPLIER")
        .line("player", id, Formatting.WHITE)
        .line("id", gainSupplierId, Formatting.AQUA)
        .newline()
        .line("computedPercent", Float.toString(gainSupplier.supplyComputedPercent()), Formatting.LIGHT_PURPLE)
        .line("fadeStart", Float.toString(gainSupplier.getFadeStart()), Formatting.AQUA)
        .line("fadeTarget", Float.toString(gainSupplier.getFadeTarget()), Formatting.AQUA)
        .line("fadeDuration", Integer.toString(gainSupplier.getFadeDuration()), Formatting.BLUE)
        .line("isFadingOut", gainSupplier.isFadingOut() ? "YES" : "NO", gainSupplier.isFadingOut() ? Formatting.GREEN : Formatting.GRAY)
        .line("isFadingIn", gainSupplier.isFadingIn() ? "YES" : "NO", gainSupplier.isFadingIn() ? Formatting.GREEN : Formatting.GRAY);

        ctx.getSource().sendFeedback(supplierInfo.build());
        return 1;
    }
    
}
