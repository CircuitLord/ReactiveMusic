package rocamocha.reactivemusic.commands;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class AwaitValueCommandUtility {
    private AwaitValueCommandUtility() {}

    private static final class Waiter {
        final FabricClientCommandSource source;
        final long expiresAtTick;
        Waiter(FabricClientCommandSource source, long expiresAtTick) {
            this.source = source;
            this.expiresAtTick = expiresAtTick;
        }
    }

    private static final Map<String, List<Waiter>> WAITERS = new ConcurrentHashMap<>();
    private static volatile long tick;

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    /** Call once in onInitializeClient(). */
    public static void initClientEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick++;
            expireTimeouts();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WAITERS.clear());
    }

    /** Start waiting for a value identified by key (non-blocking). */
    public static void await(String key, FabricClientCommandSource source, long timeoutTicks) {
        long expiresAt = tick + Math.max(1, timeoutTicks);
        WAITERS.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
               .add(new Waiter(source, expiresAt));
        source.sendFeedback(Text.literal("Waiting (client) for value '" + key + "'..."));
    }

    /** Fulfill all waiters for key; safe to call from any thread. */
    public static void complete(String key, String value) {
        List<Waiter> list = WAITERS.remove(key);
        if (list == null || list.isEmpty()) return;

        MinecraftClient client = mc();
        if (client == null) return; // very early init guard

        client.execute(() -> {
            List<Waiter> snapshot;
            synchronized (list) { snapshot = new ArrayList<>(list); }
            for (Waiter w : snapshot) {
                w.source.sendFeedback(Text.literal("Value for '" + key + "': " + value));
            }
        });
    }

    /** Fulfill all waiters for key with a custom Text message. */
    public static void complete(String key, Text message) {
        List<Waiter> list = WAITERS.remove(key);
        if (list == null || list.isEmpty()) return;

        MinecraftClient client = mc();
        if (client == null) return;

        client.execute(() -> {
            List<Waiter> snapshot;
            synchronized (list) { snapshot = new ArrayList<>(list); }
            for (Waiter w : snapshot) {
                w.source.sendFeedback(message);
            }
        });
    }

    private static void expireTimeouts() {
        MinecraftClient client = mc();
        if (client == null) return;

        List<Runnable> toRun = new ArrayList<>();
        for (Map.Entry<String, List<Waiter>> e : WAITERS.entrySet()) {
            String key = e.getKey();
            List<Waiter> list = e.getValue();
            List<Waiter> expired = new ArrayList<>();
            synchronized (list) {
                Iterator<Waiter> it = list.iterator();
                while (it.hasNext()) {
                    Waiter w = it.next();
                    if (tick >= w.expiresAtTick) {
                        expired.add(w);
                        it.remove();
                    }
                }
            }
            if (!expired.isEmpty()) {
                if (list.isEmpty()) WAITERS.remove(key);
                toRun.add(() -> {
                    for (Waiter w : expired) {
                        w.source.sendFeedback(Text.literal("Timed out (client) waiting for '" + key + "'."));
                    }
                });
            }
        }
        if (!toRun.isEmpty()) client.execute(() -> toRun.forEach(Runnable::run));
    }
}
