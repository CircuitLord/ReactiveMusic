package rocamocha.mochamix.api;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import rocamocha.mochamix.api.minecraft.MinecraftBox;
import rocamocha.mochamix.api.minecraft.MinecraftEntity;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

public interface PluginContext {
    Optional<MinecraftEntity> entity(UUID id);
    Stream<MinecraftEntity> entitiesIn(MinecraftWorld w, MinecraftBox box, Predicate<MinecraftEntity> filter);
}
