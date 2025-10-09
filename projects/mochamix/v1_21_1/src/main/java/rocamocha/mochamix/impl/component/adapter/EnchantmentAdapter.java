package rocamocha.mochamix.impl.component.adapter;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import rocamocha.mochamix.api.minecraft.MinecraftComponent.EnchantmentAccess;;

public class EnchantmentAdapter implements EnchantmentAccess {
    protected final RegistryEntry<Enchantment> entry;
    @Override public RegistryEntry<Enchantment> asNative() { return entry; }
    
    protected final Enchantment enchantment;
    
    private final String namespace;
    private final String path;
    private final int level;

    /**
     * Constructor for levelled enchantment.
     * @param namespace Namespace of the enchantment.
     * @param path Path of the enchantment.
     * @param level Level of the enchantment.
     */
    public EnchantmentAdapter(RegistryEntry<Enchantment> entry, int level) {
        this.entry = entry;
        this.enchantment = entry.value();
        this.namespace = entry.getIdAsString().split(":")[0];
        this.path = entry.getIdAsString().split(":")[1];
        this.level = level;
    }


    @Override public String namespace() { return namespace; }
    @Override public String path() { return path; }
    @Override public String full() { return namespace + ":" + path; }
    @Override public int level() { return level; }
    
}
