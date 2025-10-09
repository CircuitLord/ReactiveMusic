package rocamocha.mochamix.impl.component.adapter;

import net.minecraft.component.type.FoodComponent;
import rocamocha.mochamix.api.minecraft.MinecraftComponent.FoodAccess;

public class FoodAdapter implements FoodAccess {
    protected final FoodComponent component;
    @Override public FoodComponent asNative() { return component; }
    
    public FoodAdapter(FoodComponent component) {
        this.component = component;
    }

    @Override public int nutrition() { return component.nutrition(); }
    @Override public float saturation() { return component.saturation(); }
    @Override public boolean alwaysEdible() { return component.canAlwaysEat(); }
    @Override public int ticksToEat() { return component.getEatTicks(); }
}
