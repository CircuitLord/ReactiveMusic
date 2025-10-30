package rocamocha.lootsparkle;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/**
 * Screen handler for sparkle inventories
 *
 * Manages the interaction between player inventory and sparkle inventory
 */
public class SparkleScreenHandler extends ScreenHandler {
    private final SimpleInventory sparkleInventory;

    public SparkleScreenHandler(int syncId, PlayerInventory playerInventory, SimpleInventory sparkleInventory) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        this.sparkleInventory = sparkleInventory;

        // Add sparkle inventory slots (3x9 grid)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(sparkleInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.sparkleInventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot2 = this.slots.get(slot);

        if (slot2 != null && slot2.hasStack()) {
            ItemStack itemStack2 = slot2.getStack();
            itemStack = itemStack2.copy();

            // If the slot is in the sparkle inventory (0-26)
            if (slot < 27) {
                // Try to move to player inventory/hotbar
                if (!this.insertItem(itemStack2, 27, 63, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try to move to sparkle inventory
                if (!this.insertItem(itemStack2, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack2.isEmpty()) {
                slot2.setStack(ItemStack.EMPTY);
            } else {
                slot2.markDirty();
            }
        }

        return itemStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);

        // Check if sparkle inventory is now empty
        if (sparkleInventory.isEmpty()) {
            // Find and remove the sparkle associated with this inventory
            SparkleManager.removeSparkleIfEmpty(player.getUuid(), sparkleInventory);
            LootSparkle.LOGGER.info("Sparkle inventory emptied by player {}, removing sparkle", player.getUuid());
        }
    }

    /**
     * Factory for creating sparkle screen handlers
     */
    public static class Factory implements NamedScreenHandlerFactory {
        private final Sparkle sparkle;

        public Factory(Sparkle sparkle) {
            this.sparkle = sparkle;
        }

        @Override
        public Text getDisplayName() {
            return Text.literal("Sparkle");
        }

        @Override
        public SparkleScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new SparkleScreenHandler(syncId, playerInventory, sparkle.getInventory());
        }
    }
}