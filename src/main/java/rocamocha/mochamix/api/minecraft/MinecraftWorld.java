package rocamocha.mochamix.api.minecraft;

import rocamocha.mochamix.impl.NativeAccess;

public interface MinecraftWorld extends NativeAccess {

    default Time time() { return this.time(); }
    default Dimension dimension() { return this.dimension(); }
    default Weather weather() { return this.weather(); }
    default Blocks blocks() { return this.blocks(); }

    boolean clientSided();

    interface Time {
        long timeOfDay();
        long worldAge();
        int daysPassed();
    }

    interface Dimension {
        String id();
        String namespace();
        String path();
    }

    interface Weather {
        boolean isClearAt(MinecraftVector3 pos);
        boolean isRainingAt(MinecraftVector3 pos);
        boolean isThunderingAt(MinecraftVector3 pos);
        boolean isSnowingAt(MinecraftVector3 pos);
    }

    interface Blocks {
        String getIdAt(int x, int y, int z);
        String getIdAt(MinecraftVector3 blockPos);
        String getBiomeAt(int x, int y, int z);
        String getBiomeAt(MinecraftVector3 blockPos);
    }


}

