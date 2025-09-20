package rocamocha.mochamix.impl.box;

import net.minecraft.util.math.Box;
import rocamocha.mochamix.api.minecraft.MinecraftVector3;
import rocamocha.mochamix.impl.vector3.Vector3Socket;
import rocamocha.mochamix.api.minecraft.MinecraftBox;
import rocamocha.mochamix.api.io.MinecraftView;

public class BoxView implements MinecraftBox {
    protected final Box box;

    protected final MinecraftVector3.Vector3d min;
    protected final MinecraftVector3.Vector3d max;

    BoxView(MinecraftVector3 min, MinecraftVector3 max) {
        this.min = (MinecraftVector3.Vector3d) min.asVec3d();
        this.max = (MinecraftVector3.Vector3d) max.asVec3d();
        this.box = new Box(
            this.min.xd(),
            this.min.yd(),
            this.min.zd(),
            
            this.max.xd(),
            this.max.yd(),
            this.max.zd()
        );
    }

    @Override public Box asNative() { return box; }

    @Override public MinecraftVector3 min() { return min; }
    @Override public MinecraftVector3 max() { return max; }
    @Override public MinecraftVector3 center() { return MinecraftView.of(box.getCenter()); }
    @Override public MinecraftVector3 size() { return new Vector3Socket(box.getLengthX(), box.getLengthY(), box.getLengthZ()); }

    @Override public int width() { return (int) box.getLengthX(); }
    @Override public int height() { return (int) box.getLengthY(); }
    @Override public int depth() { return (int) box.getLengthZ(); }
}
