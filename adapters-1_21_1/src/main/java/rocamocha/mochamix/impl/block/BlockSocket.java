package rocamocha.mochamix.impl.block;

import net.minecraft.block.Block;
import rocamocha.mochamix.api.minecraft.MinecraftBlock;
import rocamocha.mochamix.impl.common.IdentityAdapter;

public class BlockSocket implements MinecraftBlock {
    protected final Block b;
    protected final IdentityAdapter identity;
    
    public BlockSocket(Block b) {
        this.b = b;
        this.identity = new IdentityAdapter(b);
    }

    @Override public Block asNative() { return b; }
}
