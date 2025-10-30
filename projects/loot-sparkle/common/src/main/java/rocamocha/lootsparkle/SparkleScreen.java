package rocamocha.lootsparkle;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;

/**
 * GUI screen for sparkle inventories
 *
 * Displays the sparkle's inventory in a chest-like interface
 */
public class SparkleScreen extends HandledScreen<GenericContainerScreenHandler> {
    private final SimpleInventory sparkleInventory;

    public SparkleScreen(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title, SimpleInventory sparkleInventory) {
        super(handler, inventory, title);
        this.sparkleInventory = sparkleInventory;
        this.backgroundWidth = 176; // Standard chest width
        this.backgroundHeight = 166; // Standard chest height
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // TODO: Implement custom background texture
        // For now, this would use a custom texture similar to chest GUI
        // context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
    }
}