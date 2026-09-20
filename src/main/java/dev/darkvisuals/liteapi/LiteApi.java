package dev.darkvisuals.liteapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Module;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class LiteApi {

    /** Стабильный id мода. НЕ меняй между версиями — по нему хранятся персональные блокировки. */
    private static final String CLIENT_ID = "darkvisuals";

    private static final long MIN_INTERVAL_MS = 10_000L; // rate limit: 1 запрос / 10 сек
    private static final long TIMEOUT_SEC = 5L;

    private static final Map<String, CompletableFuture<JsonObject>> PENDING = new ConcurrentHashMap<>();
    private static final Set<String> BLOCKLIST = ConcurrentHashMap.newKeySet();
    private static volatile long lastRequestAt = 0L;

    private LiteApi() {
    }

    public static void init() {
        PayloadTypeRegistry.playC2S().register(LiteApiPayload.ID, LiteApiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LiteApiPayload.ID, LiteApiPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(LiteApiPayload.ID,
                (payload, context) -> handleResponse(payload.json()));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            BLOCKLIST.clear();
            checkAllFeatures();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BLOCKLIST.clear();
            PENDING.values().forEach(f -> f.cancel(true));
            PENDING.clear();
        });
    }

    public static boolean isBlocked(String featureName) {
        return featureName != null && BLOCKLIST.contains(featureName.toLowerCase(Locale.ROOT));
    }

    public static Set<String> blocklist() {
        return Collections.unmodifiableSet(BLOCKLIST);
    }

    /** Проверка всех фич мода. Троттлится сама (не чаще 1 раза в 10 сек). */
    public static void checkAllFeatures() {
        if (!ClientPlayNetworking.canSend(LiteApiPayload.ID)) return; // не HolyWorld — молча выходим

        long now = System.currentTimeMillis();
        if (now - lastRequestAt < MIN_INTERVAL_MS) return;
        lastRequestAt = now;

        List<String> features = new ArrayList<>();
        for (Module m : darkvisuals.getInstance().getModuleManager().getModules()) {
            features.add(m.getName().toLowerCase(Locale.ROOT));
        }

        String id = UUID.randomUUID().toString();
        JsonObject payload = new JsonObject();
        payload.addProperty("client", CLIENT_ID);
        JsonArray arr = new JsonArray();
        features.forEach(arr::add);
        payload.add("features", arr);

        JsonObject req = new JsonObject();
        req.addProperty("id", id);
        req.addProperty("method", "checkFeatures");
        req.add("payload", payload);

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        PENDING.put(id, future);
        ClientPlayNetworking.send(new LiteApiPayload(req.toString()));

        MinecraftClient client = MinecraftClient.getInstance();
        future.orTimeout(TIMEOUT_SEC, TimeUnit.SECONDS).whenComplete((resp, ex) -> {
            PENDING.remove(id);
            if (ex != null || resp == null) return;
            client.execute(() -> applyBlocklist(resp));
        });
    }

    private static void handleResponse(String json) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return;
        }
        if (root.has("event")) return; // push-событие, не ответ — игнорим
        if (!root.has("id") || !root.has("ok")) return;
        CompletableFuture<JsonObject> future = PENDING.get(root.get("id").getAsString());
        if (future != null) future.complete(root);
    }

    private static void applyBlocklist(JsonObject resp) {
        if (!resp.get("ok").getAsBoolean()) {
            String err = resp.has("error") ? resp.get("error").getAsString() : "unknown";
            darkvisuals.LOGGER.warn("[LiteAPI] checkFeatures error: {}", err);
            return;
        }
        BLOCKLIST.clear();
        for (JsonElement el : resp.getAsJsonObject("payload").getAsJsonArray("blocklist")) {
            BLOCKLIST.add(el.getAsString().toLowerCase(Locale.ROOT));
        }
        // Выключаем заблокированное прямо сейчас
        for (Module m : darkvisuals.getInstance().getModuleManager().getModules()) {
            if (m.isToggled() && isBlocked(m.getName())) m.setToggled(false);
        }
        darkvisuals.LOGGER.info("[LiteAPI] blocklist: {}", BLOCKLIST);
    }
}