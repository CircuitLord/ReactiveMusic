package circuitlord.reactivemusic.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.ReactiveMusicDebug;
import circuitlord.reactivemusic.ReactiveMusicDebug.TextBuilder;
import circuitlord.reactivemusic.api.ReactiveMusicPlugin;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.util.Formatting;

public final class PluginCommandHandlers {

    public static int listPlugins(CommandContext<FabricClientCommandSource> ctx) {
        TextBuilder pluginList = new TextBuilder();
        for (ReactiveMusicPlugin plugin : ReactiveMusic.PLUGINS) {
            pluginList.line(plugin.pluginId.getId(), Formatting.AQUA);
        }   
        ctx.getSource().sendFeedback(pluginList.build());
        return 1;
    }

    public static int enablePlugin(CommandContext<FabricClientCommandSource> ctx) {
        String pluginId = StringArgumentType.getString(ctx, "pluginId");
        ctx.getSource().sendFeedback(ReactiveMusicDebug.NON_IMPL_WARN_BUILT);
        return 1;
    }
    
    public static int disablePlugin(CommandContext<FabricClientCommandSource> ctx) {
        String pluginId = StringArgumentType.getString(ctx, "pluginId");
        ctx.getSource().sendFeedback(ReactiveMusicDebug.NON_IMPL_WARN_BUILT);
        return 1;
    }
}
