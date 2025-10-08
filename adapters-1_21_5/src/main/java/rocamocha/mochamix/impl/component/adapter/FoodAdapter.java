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
    @Override public int ticksToEat() { throw new UnsupportedOperationException("ticksToEat is not supported by FoodComponent in this Minecraft 1.21.5"); }
}
