package dev.darkvisuals.client.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darkvisuals.darkvisuals;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

 public class UserTabManager {

     
     
    private static final String API_BASE = "http://159.194.201.201:8080/";
    private static final String REPORT_URL = API_BASE + "usertab/report";
    private static final String LIST_URL = API_BASE + "usertab/list";

    private static final int POLL_INTERVAL_SECONDS = 30;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Set<UUID> knownUsers = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DarkVisuals-UserTab-Sync");
                t.setDaemon(true);
                return t;
            });

    private static volatile boolean started = false;

    private UserTabManager() {}

    public static void init() {
        if (started) return;
        started = true;

         
         
         
        scheduler.scheduleWithFixedDelay(UserTabManager::syncPresence, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);

         
         
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            scheduler.schedule(UserTabManager::syncPresence, 1, TimeUnit.SECONDS);
            scheduler.schedule(UserTabManager::syncPresence, 5, TimeUnit.SECONDS);
        });
    }

    public static boolean isKnownUser(UUID uuid) {
        return uuid != null && knownUsers.contains(uuid);
    }

    public static void shutdown() {
        knownUsers.clear();
        try {
            scheduler.shutdownNow();
        } catch (Throwable ignored) {}
    }

    private static void syncPresence() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) reportSelf(mc.player.getUuid());
        fetchList();
    }

    private static void reportSelf(UUID uuid) {
        if (uuid == null) return;
        scheduler.execute(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("uuid", uuid.toString());
                HttpRequest request = HttpRequest.newBuilder(URI.create(REPORT_URL))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();
                HTTP.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                darkvisuals.LOGGER.debug("[UserTab] Failed to report self presence: {}", e.toString());
            }
        });
    }

    private static void fetchList() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(LIST_URL))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;

            JsonElement parsed = JsonParser.parseString(response.body());
            JsonArray arr = parsed.isJsonObject() && parsed.getAsJsonObject().has("users")
                    ? parsed.getAsJsonObject().getAsJsonArray("users")
                    : parsed.getAsJsonArray();

            Set<UUID> updated = ConcurrentHashMap.newKeySet();
            for (JsonElement e : arr) {
                try {
                    updated.add(UUID.fromString(e.getAsString()));
                } catch (IllegalArgumentException ignored) {}
            }

            knownUsers.clear();
            knownUsers.addAll(updated);
        } catch (Exception e) {
            darkvisuals.LOGGER.debug("[UserTab] Failed to fetch user list: {}", e.toString());
        }
    }
}
