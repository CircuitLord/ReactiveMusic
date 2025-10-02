package rocamocha.mochamix.impl.vector3;

import rocamocha.mochamix.api.minecraft.util.MinecraftVector3;

/**
 * Provides math operations for MinecraftVector3 instances.
 * Implements addition, subtraction, length calculation, and offsetting, etc.
 * Uses Vector3Socket for coordinate storage and conversion.
 * 
 * For internal use only. Use MinecraftVector3 interface in API code.
 * @see MinecraftVector3
 * @see Vector3Socket
 */
public class Vector3Math  {

    /** The MinecraftVector3 instance being operated on. */
    private final MinecraftVector3 position;

    /** Constructs a Vector3Math for the given MinecraftVector3. */
    public Vector3Math(MinecraftVector3 position) {
        this.position = position;
    }

    /** Adds two MinecraftVector3 instances and returns a new one. */
    public MinecraftVector3 add(MinecraftVector3 that) {
        var a = this.position.asVec3d();
        var b = that.asVec3d();
        return new Vector3Socket(
            a.xd() + b.xd(),
            a.yd() + b.yd(),
            a.zd() + b.zd()
        );
    }

    /** Subtracts another MinecraftVector3 from this one and returns a new one. */
    public MinecraftVector3 subtract(MinecraftVector3 that) {
        var a = this.position.asVec3d();
        var b = that.asVec3d();
        return new Vector3Socket(
            a.xd() - b.xd(),
            a.yd() - b.yd(),
            a.zd() - b.zd()
        );
    }

    /** Calculates the Euclidean length (magnitude) of this position vector. */
    public double length() {
        var v = this.position.asVec3d();
        return Math.sqrt(v.xd() * v.xd() + v.yd() * v.yd() + v.zd() * v.zd());
    }

    /** Offsets this position by the given integer amounts and returns a new one. */
    public MinecraftVector3 offset(int x, int y, int z) {
        var v = this.position.asVec3i();
        return new Vector3Socket(
            v.xi() + x,
            v.yi() + y,
            v.zi() + z
        );
    }

    /** 
     * Computes the square root using the Babylonian method for better precision.
     * Currently not used, but kept for potential future use.
     */
    @SuppressWarnings("unused")
    private double sqrt(double value) {
        if (value < 0) throw new IllegalArgumentException("Cannot compute square root of negative number");
        if (value == 0) return 0;
        double x = value;
        double y = (x + value / x) / 2;
        while (Math.abs(y - x) > 1e-10) {
            x = y;
            y = (x + value / x) / 2;
        }
        return y;
    }
    
}
