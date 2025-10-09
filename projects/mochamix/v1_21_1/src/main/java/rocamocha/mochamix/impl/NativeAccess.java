package rocamocha.mochamix.impl;

/**
 * This interface is used internally when the native Minecraft object is needed from a returned adapter.
 * Used in particular for passing the native object back to native Minecraft methods.
 * 
 * This should be used sparingly, as it breaks the abstraction and portability of the API.
 * The preferred way is to use the adapter methods to access needed functionality.
 * Mainly useful for advanced use cases and interop with other mods, or utilities requiring native access.
 * 
 * The native object may not be available in all contexts, so the default implementation throws.
 */
public interface NativeAccess {
    // Return the native Minecraft object, e.g., World, PlayerEntity, ItemStack, etc.
    // The object should be recastable to the appropriate native type for internal use.
    default Object asNative() { throw new UnsupportedOperationException("The native object is not available."); };

}
