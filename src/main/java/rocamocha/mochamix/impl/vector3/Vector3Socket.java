package rocamocha.mochamix.impl.vector3;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rocamocha.mochamix.api.minecraft.MinecraftVector3;
import rocamocha.mochamix.api.minecraft.MinecraftVector3.*;

/**
 * A view of a Minecraft position, which can be a Vec3d, Vec3i, or BlockPos.
 * Implements all interfaces of MinecraftVector3.
 * Provides conversions between different position types.
 * Delegates math operations to Vector3Math.
 * 
 * For internal use only. Use MinecraftVector3 interface in API code.
 * @see MinecraftVector3
 * @see Vector3Math
 */
public class Vector3Socket implements Vector3d, BlockPosition {
    private final double x;
    private final double y;
    private final double z;
    
    private final Vector3Math math;

    /**
     * Abstract math operations to Vector3Math
     * @return a cached Vector3Math instance for this position
     * @see Vector3Math
     * @see MinecraftVector3
     */
    @Override public Vector3Math doMath() { return math; }


    // Conversions between different position types
    @Override public Vector3d asVec3d() {
        Vec3d that = new Vec3d(x, y, z);
        return new Vector3Socket(that);
    }
    @Override public Vector3i asVec3i() {
        Vec3i that = new Vec3i((int) x, (int) y, (int) z);
        return new Vector3Socket(that);
    }
    @Override public BlockPosition asBlockPos() {
        BlockPos that = new BlockPos((int) x, (int) y, (int) z);
        return new Vector3Socket(that);
    }

    // Constructors for different position types and raw coordinates
    public Vector3Socket(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.math = new Vector3Math(this);
    }
    public Vector3Socket(net.minecraft.util.math.Vec3d vec) {
        this.x = vec.getX();
        this.y = vec.getY();
        this.z = vec.getZ();

        this.math = new Vector3Math(this);
    }

    public Vector3Socket(net.minecraft.util.math.Vec3i vec) {
        this.x = vec.getX();
        this.y = vec.getY();
        this.z = vec.getZ();

        this.math = new Vector3Math(this);
    }

    public Vector3Socket(net.minecraft.util.math.BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();

        this.math = new Vector3Math(this);
    }

    /**
     * Casts a Vec3d, Vec3i, or BlockPos to a Vector3View.
     * @param pos
     * @return a Vector3View
     * @throws IllegalArgumentException if the object is not a supported type
     */
    public static Vector3Socket from(Object pos) {
        if (pos instanceof net.minecraft.util.math.Vec3d) {
            return new Vector3Socket((net.minecraft.util.math.Vec3d) pos);
        } else if (pos instanceof net.minecraft.util.math.Vec3i) {
            return new Vector3Socket((net.minecraft.util.math.Vec3i) pos);
        } else if (pos instanceof net.minecraft.util.math.BlockPos) {
            return new Vector3Socket((net.minecraft.util.math.BlockPos) pos);
        } else if (pos instanceof Vector3Socket) {
            return (Vector3Socket) pos;
        } else {
            throw new IllegalArgumentException("Attempted to cast unsupported type as MinecraftVector3: " + pos.getClass());
        }
    }

    // Native conversions to Minecraft types for interop with Minecraft code and mods
    @Override public BlockPos asNativeBlockPos() { return new BlockPos((int) x, (int) y, (int) z); }
    @Override public Vec3i asNativeVec3i() { return new Vec3i((int) x, (int) y, (int) z); }
    @Override public Vec3d asNativeVec3d() { return new Vec3d(x, y, z); }

    // Getters for coordinates in double and integer forms
    @Override public double xd() { return x; }
    @Override public double yd() { return y; }
    @Override public double zd() { return z; }
    @Override public int xi() { return (int) x; }
    @Override public int yi() { return (int) y; }
    @Override public int zi() { return (int) z; }
}
