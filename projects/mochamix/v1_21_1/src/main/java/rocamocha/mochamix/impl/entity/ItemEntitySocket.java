package rocamocha.mochamix.impl.entity;

import net.minecraft.entity.ItemEntity;
import rocamocha.mochamix.api.minecraft.MinecraftEntity.MinecraftItemEntity;
import rocamocha.mochamix.api.minecraft.MinecraftItemStack;
import rocamocha.mochamix.impl.item.ItemStackSocket;

public class ItemEntitySocket extends EntitySocket implements MinecraftItemEntity {
    public ItemEntitySocket(ItemEntity e) { super(e); }
    
    @Override public ItemEntity asNative() { return (ItemEntity) super.asNative(); }
    @Override public MinecraftItemStack itemStack() { return new ItemStackSocket(asNative().getStack()); }
    @Override public int age() { return asNative().getItemAge(); }
    
}
