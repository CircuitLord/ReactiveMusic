package rocamocha.mochamix.commands.player;

import java.util.List;

import com.mojang.brigadier.context.CommandContext;

import rocamocha.reactivemusic.ReactiveMusicDebug.TextBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Formatting;
import rocamocha.mochamix.api.minecraft.MinecraftComponent.*;
import rocamocha.mochamix.api.minecraft.MinecraftEntity;
import rocamocha.mochamix.api.minecraft.MinecraftEntity.MinecraftLivingEntity;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.impl.entity.LivingEntitySocket;
import rocamocha.mochamix.api.io.MinecraftView;

public class PlayerCommandHandlers {
    
    /**
     * Cache of the last fetched nearby entities list.
     * This can be used by other commands to avoid redundant lookups,
     * or for debugging purposes.
     */
    private static List<MinecraftEntity> cachedNearbyEntities = null;

    private static MinecraftPlayer player() {
        return MinecraftView.of(MinecraftClient.getInstance().player);
    }
    
    public static int playerInfo(CommandContext<FabricClientCommandSource> ctx) {
        MinecraftPlayer player = player();
        TextBuilder info = new TextBuilder();
        
        info.line("name", player.name(), Formatting.WHITE);
        info.line("uuid", player.uuid().toString(), Formatting.WHITE);
        
        PlayerCommandHelpers.gamemodeInfo(info, player);
        PlayerCommandHelpers.positionInfo(info, player);
        PlayerCommandHelpers.statusInfo(info, player);
        
        info.line("level", Integer.toString(player.experience().level()), Formatting.GREEN);
            
        ctx.getSource().sendFeedback(info.build());
        return 1;
    }
    
    public static int nearbyEntitiesInfo(CommandContext<FabricClientCommandSource> ctx) {
        
        int radius = 20;
        
        MinecraftPlayer player = player();
        TextBuilder info = new TextBuilder();
        List<MinecraftEntity> nearbyEntities = player.surroundings().nearbyEntities(radius);
        cachedNearbyEntities = nearbyEntities;
        
        if (nearbyEntities.size() > 0) {
            info.line("nearbyEntities("+Integer.toString(radius)+")", Integer.toString(player.surroundings().nearbyEntities(radius).size()), Formatting.YELLOW);
            
            for (int i = 0; i < player.surroundings().nearbyEntities(radius).size(); i++) {
                MinecraftEntity e = player.surroundings().nearbyEntities(radius).get(i);

                if (e == null) continue;
                info.line("typeId (" + Integer.toString(i) + ")", e.typeId(), Formatting.DARK_AQUA);
                
                // Only process living entities for distance and aggro info
                if (e.isLivingEntity()) {
                    try {
                        double squaredDistance = ((LivingEntity) e.asLiving().asNative()).squaredDistanceTo((LivingEntity) player.asNative());
                        int distance = (int) Math.sqrt(squaredDistance);
                        info.line("  distance", Integer.toString(distance), Formatting.GRAY);

                        // Show aggro target if not empty
                        MinecraftLivingEntity aggroTargetEntity = e.asLiving().aggroTarget();
                        if (aggroTargetEntity != null) {
                            info.line("  aggroTarget", aggroTargetEntity.typeId(), Formatting.DARK_RED);
                        }

                        // Show last attacker if not empty
                        MinecraftLivingEntity attackerEntity = e.asLiving().attackedBy();
                        if (attackerEntity != null) {
                            info.line("  lastAttacker", attackerEntity.typeId(), Formatting.LIGHT_PURPLE);
                        }
                        
                        // Show detailed damage source information if available
                        if (e.asLiving() instanceof LivingEntitySocket socket) {
                            String damageInfo = socket.getLastDamageSourceInfo();
                            if (damageInfo != null) {
                                info.line("  damageSource", damageInfo, Formatting.DARK_PURPLE);
                            }
                        }
                        
                        // Additional entity type info for debugging
                        String entityClass = e.asNative().getClass().getSimpleName();
                        info.line("  entityClass", entityClass, Formatting.GRAY);
                        info.newline();
                    } catch (Exception ex) {
                        info.line("  error", "Failed to get entity info: " + ex.getMessage(), Formatting.RED);
                    }
                }
            }
        }
        ctx.getSource().sendFeedback(info.build());
        return 1;
    }

    public static int mainhandInfo(CommandContext<FabricClientCommandSource> ctx) {
        MinecraftPlayer player = player();
        TextBuilder info = new TextBuilder();
        if (player != null && player.inventory() != null && player.inventory().mainhand() != null) {
            info.line("mainhand", player.inventory().mainhand().name(), Formatting.WHITE);
            info.line("  id", player.inventory().mainhand().identity().full(), Formatting.GOLD);
            info.line("  count", Integer.toString(player.inventory().mainhand().count()) + "/" + Integer.toString(player.inventory().mainhand().max()), Formatting.GRAY);
            info.line("  enchantments", Integer.toString(player.inventory().mainhand().enchantments().size()), Formatting.DARK_PURPLE);
            player.inventory().mainhand().enchantments().forEach((k,v) -> {
                info.line("    " + k, Integer.toString(v.level()), Formatting.DARK_AQUA);
            });

            if (player.inventory().mainhand().food() != null) {
                FoodAccess food = player.inventory().mainhand().food();
                info.line("  nutrition", Integer.toString(food.nutrition()), Formatting.AQUA);
                info.line("  alwaysEdible", food.alwaysEdible() ? "YES" : "NO", food.alwaysEdible() ? Formatting.GREEN : Formatting.RED);
            }
        } else {
            info.line("mainhand", "<none>", Formatting.YELLOW);
        }
        ctx.getSource().sendFeedback(info.build());
        return 1;
    }
}
