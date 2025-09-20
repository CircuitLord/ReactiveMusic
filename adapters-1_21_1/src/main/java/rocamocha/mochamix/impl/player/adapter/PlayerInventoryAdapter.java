package rocamocha.mochamix.impl.player.adapter;

import net.minecraft.entity.player.PlayerEntity;
import rocamocha.mochamix.api.io.MinecraftView;
import rocamocha.mochamix.api.minecraft.MinecraftItemStack;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer.Inventory;

public class PlayerInventoryAdapter implements Inventory {
    protected final PlayerEntity p;
    public PlayerInventoryAdapter(PlayerEntity p) { this.p = p; }

    @Override public MinecraftItemStack mainhand() {
        return MinecraftView.of(p.getInventory().getMainHandStack());
    }

    @Override public MinecraftItemStack offhand() {
        return MinecraftView.of(p.getOffHandStack());
    }

    @Override public MinecraftItemStack slot(int slot) {
        return MinecraftView.of(p.getInventory().getStack(slot));
    }
}
