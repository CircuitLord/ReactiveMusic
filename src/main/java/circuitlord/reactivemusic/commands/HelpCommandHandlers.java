package circuitlord.reactivemusic.commands;

import com.mojang.brigadier.context.CommandContext;

import circuitlord.reactivemusic.ReactiveMusicDebug.TextBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HelpCommandHandlers {

    public static class HelpBuilder extends TextBuilder {

        private String commandTree;

        public HelpBuilder(String commmandTree) {
            this.commandTree = "/" + commmandTree;
        }

        @Override
        public HelpBuilder header(String text) {
            root.append(
                Text.literal("====== " + text + " ======\n\n")
                .formatted(Formatting.GOLD, Formatting.BOLD)
            );
            return this;
        }
        
        public HelpBuilder helpline(String command, String description, Formatting valueColor) {
            root.append(Text.literal(commandTree + " "));
            root.append(Text.literal(command + " -> ").formatted(Formatting.GREEN, Formatting.BOLD));
            root.append(Text.literal(description).formatted(Formatting.BOLD, Formatting.ITALIC, valueColor));
            root.append(Text.literal("\n"));
            return this;
        }

    }

    public static int songpackCommands(CommandContext<FabricClientCommandSource> ctx) {
        HelpBuilder help = new HelpBuilder("songpack");
        
        help.header("SONGPACK COMMANDS")
        
        .helpline("info", "Not implemented.", Formatting.RED)
        .helpline("entry current", "Info for the current songpack entry.", Formatting.WHITE)
        .helpline("entry list", "Lists all entries. Valid entries are highlighted.", Formatting.WHITE);
        
        ctx.getSource().sendFeedback(help.build());
        return 1;
    }

    public static int playerCommands(CommandContext<FabricClientCommandSource> ctx) {
        HelpBuilder help = new HelpBuilder("player");
        
        help.header("AUDIO COMMANDS")
        
        .helpline("list", "Provides a list of all audio players.", Formatting.WHITE)
        .helpline("info <playerId>", "Info for the specified player.", Formatting.WHITE)
        .helpline("info <gainSupplierId>", "Info for the specified supplier.", Formatting.WHITE);
        
        ctx.getSource().sendFeedback(help.build());
        return 1;
    }

    public static int pluginCommands(CommandContext<FabricClientCommandSource> ctx) {
        HelpBuilder help = new HelpBuilder("plugin");
        
        help.header("PLUGIN COMMANDS")
        
        .helpline("list", "Provides a list of all plugins.", Formatting.WHITE)
        .helpline("enable <pluginId>", "Not implemented.", Formatting.RED)
        .helpline("disable <pluginId>", "Not implemented.", Formatting.RED);
        
        ctx.getSource().sendFeedback(help.build());
        return 1;
    }
    
}
