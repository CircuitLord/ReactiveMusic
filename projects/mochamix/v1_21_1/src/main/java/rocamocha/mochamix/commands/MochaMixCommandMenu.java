package rocamocha.mochamix.commands;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

/**
 * Command handler for /mochamix api help command.
 * Displays clickable links to available subcommands.
 */
public class MochaMixCommandMenu {
    public static int main(CommandContext<FabricClientCommandSource> context) {
        MutableText header = Text.literal("=== MochaMix API Debug Tools ===\n")
            .formatted(Formatting.GOLD, Formatting.BOLD);
        
        Text playerLink = Text.literal("Player Information")
            .setStyle(Style.EMPTY
                .withColor(Formatting.AQUA)
                .withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mochamix api player"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Text.literal("Shows current player state and info")))
            );
            
        Text mainhandLink = Text.literal("Mainhand Analysis")  
            .setStyle(Style.EMPTY
                .withColor(Formatting.GREEN)
                .withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mochamix api player mainhand"))
            );
            
        Text entitiesLink = Text.literal("Nearby Entities")
            .setStyle(Style.EMPTY
                .withColor(Formatting.YELLOW)  
                .withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mochamix api player nearby_entities"))
            );
            
        MutableText message = header
            .append(playerLink).append("\n")
            .append(mainhandLink).append("\n") 
            .append(entitiesLink).append("\n\n")
            .append(Text.literal("Click any option above to run it!")
                .formatted(Formatting.GRAY, Formatting.ITALIC));
            
        context.getSource().sendFeedback(message);
        return 1;
    }
}
