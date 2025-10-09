package rocamocha.mochamix.impl.common;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import rocamocha.mochamix.api.minecraft.util.MinecraftIdentity;

public class IdentityAdapter implements MinecraftIdentity {
    
    private final String namespace;
    private final String path;
    
    public IdentityAdapter(ItemStack itemStack) {
        this.namespace = Registries.ITEM.getId(itemStack.getItem()).getNamespace();
        this.path = Registries.ITEM.getId(itemStack.getItem()).getPath();
    }

    public IdentityAdapter(Block block) {
        this.namespace = Registries.BLOCK.getId(block).getNamespace();
        this.path = Registries.BLOCK.getId(block).getPath();
    }

    public IdentityAdapter(Entity entity) {
        this.namespace = Registries.ENTITY_TYPE.getId(entity.getType()).getNamespace();
        this.path = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
    }

    @Override public String namespace() { return namespace; }
    @Override public String path() { return path; }
    @Override public String full() { return namespace + ":" + path; }
}
