package rocamocha.mochamix.impl.component.adapter;

import net.minecraft.util.Rarity;
import rocamocha.mochamix.api.minecraft.MinecraftComponent.RarityAccess;

public class RarityAdapter implements RarityAccess {
    protected final Rarity rarity;

    public RarityAdapter(Rarity rarity) {
        this.rarity = rarity;
    }

    @Override public String name() { return rarity.name(); }
    @Override public int value() { return name() == "common" ? 0 : name() == "uncommon" ? 1 : name() == "rare" ? 2 : name() == "epic" ? 3 : -1; }
}
