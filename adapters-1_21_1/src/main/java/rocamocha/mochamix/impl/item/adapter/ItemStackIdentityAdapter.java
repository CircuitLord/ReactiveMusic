package rocamocha.mochamix.impl.item.adapter;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import rocamocha.mochamix.api.minecraft.MinecraftItemStack.Identity;

public class ItemStackIdentityAdapter implements Identity {

    @SuppressWarnings("unused")
    private final ItemStack itemStack;
    
    private final String namespace;
    private final String path;
    
    public ItemStackIdentityAdapter(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.namespace = Registries.ITEM.getId(itemStack.getItem()).getNamespace();
        this.path = Registries.ITEM.getId(itemStack.getItem()).getPath();
    }

    @Override public String namespace() { return namespace; }
    @Override public String path() { return path; }
    @Override public String full() { return namespace + ":" + path; }
}
