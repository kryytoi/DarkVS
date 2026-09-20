package dev.darkvisuals.client.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.emotes.EmoteManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

 public final class CosmeticsSyncManager {

     
    public static String API_BASE = "http://159.194.201.201:8080";

    private static final String REPORT_URL = API_BASE + "/cosmetics/report";
    private static final String LIST_URL = API_BASE + "/cosmetics/list";
    private static final String WS_URL = API_BASE.replace("http", "ws") + "/ws";

    private static final int POLL_INTERVAL_SECONDS = 10;

      
    private static final Map<UUID, Set<String>> REMOTE_COSMETICS = new ConcurrentHashMap<>();

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DarkVisuals-Bridge-Sync");
                t.setDaemon(true);
                return t;
            });

    private static WebSocket ws;
    private static volatile boolean started = false;
    private static volatile boolean wsConnected = false;
    private static volatile UUID lastRegisteredWsUuid = null;

    private CosmeticsSyncManager() {}

    public static void init() {
        if (started) return;
        started = true;

         
        scheduler.scheduleWithFixedDelay(CosmeticsSyncManager::syncCosmetics,
                1, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);

         
        connectWs();

         
         
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            scheduler.schedule(CosmeticsSyncManager::syncCosmetics, 1, TimeUnit.SECONDS);
            scheduler.schedule(CosmeticsSyncManager::syncCosmetics, 5, TimeUnit.SECONDS);
        });

         
         
        ClientTickEvents.END_CLIENT_TICK.register(client -> registerWsSelfIfPossible());
    }

      
    public static Set<String> getRemoteCosmetics(UUID uuid) {
        Set<String> s = REMOTE_COSMETICS.get(uuid);
        return s == null ? Set.of() : s;
    }

    public static boolean hasRemoteCosmetic(UUID uuid, String name) {
        Set<String> s = REMOTE_COSMETICS.get(uuid);
        return s != null && s.contains(name);
    }

    public static void shutdown() {
        wsConnected = false;
        lastRegisteredWsUuid = null;
        REMOTE_COSMETICS.clear();
        try {
            if (ws != null) ws.sendClose(WebSocket.NORMAL_CLOSURE, "Minecraft shutdown").join();
        } catch (Throwable ignored) {}
        ws = null;
        try {
            scheduler.shutdownNow();
        } catch (Throwable ignored) {}
    }

     

    private static void syncCosmetics() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        UUID myUuid = mc.player.getUuid();
        String username = mc.player.getGameProfile().getName();

        registerWsSelfIfPossible();
        reportSelf(myUuid, username);
        fetchList();
    }

    private static void reportSelf(UUID uuid, String username) {
        scheduler.execute(() -> {
            try {
                Set<String> cosmetics = currentCosmetics();
                JsonArray arr = new JsonArray();
                for (String c : cosmetics) arr.add(c);

                JsonObject body = new JsonObject();
                body.addProperty("uuid", uuid.toString());
                body.addProperty("username", username);
                body.add("cosmetics", arr);

                HttpRequest request = HttpRequest.newBuilder(URI.create(REPORT_URL))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();
                HTTP.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                darkvisuals.LOGGER.debug("[Bridge] Failed to report cosmetics: {}", e.toString());
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
            if (!parsed.isJsonObject()) return;
            JsonArray arr = parsed.getAsJsonObject().getAsJsonArray("online");
            if (arr == null) return;

            Map<UUID, Set<String>> updated = new ConcurrentHashMap<>();
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                try {
                    UUID uuid = UUID.fromString(o.get("uuid").getAsString());
                    Set<String> set = new HashSet<>();
                    JsonArray cos = o.getAsJsonArray("cosmetics");
                    for (JsonElement ce : cos) set.add(ce.getAsString());
                    updated.put(uuid, set);
                } catch (Exception ignored) {}
            }

            REMOTE_COSMETICS.clear();
            REMOTE_COSMETICS.putAll(updated);
        } catch (Exception e) {
            darkvisuals.LOGGER.debug("[Bridge] Failed to fetch cosmetics list: {}", e.toString());
        }
    }

      
    private static Set<String> currentCosmetics() {
        Set<String> set = new HashSet<>();
        try {
            var mod = darkvisuals.getInstance().getModuleManager()
                    .getModule(dev.darkvisuals.modules.impl.render.Cosmetics.class);
            if (mod == null || !mod.isToggled()) return set;
            if (mod.getWingsSetting().getValue()) set.add("w1");
            if (mod.getWings2Setting().getValue()) set.add("w2");
            if (mod.getBlackWingsSetting().getValue()) set.add("bw");
            if (mod.getNimbusSetting().getValue()) set.add("nimbus");
            if (mod.getCapSetting().getValue()) set.add("cap");
            if (mod.getChinaHatSetting().getValue()) set.add("china");
            if (mod.getKaguneSetting().getValue()) set.add("kagune");
            if (mod.getMortyPatchSetting().getValue()) set.add("morty");
            if (mod.getNikeCapSetting().getValue()) set.add("nike");
            if (mod.getRobotTentaclesSetting().getValue()) set.add("robot_tentacles");
        } catch (Throwable ignored) {}
        return set;
    }

     

    private static void connectWs() {
        scheduler.execute(() -> {
            try {
                ws = HTTP.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .buildAsync(URI.create(WS_URL), new Listener())
                        .join();
                wsConnected = true;
                darkvisuals.LOGGER.info("[Bridge] WebSocket подключён: {}", WS_URL);

                registerWsSelfIfPossible();
            } catch (Exception e) {
                wsConnected = false;
                darkvisuals.LOGGER.warn("[Bridge] WebSocket не подключён, повтор через 10с: {}", e.toString());
                scheduler.schedule(CosmeticsSyncManager::connectWs, 10, TimeUnit.SECONDS);
            }
        });
    }

    private static void registerWsSelfIfPossible() {
        if (!wsConnected || ws == null) return;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            UUID uuid = mc.player.getUuid();
            if (uuid.equals(lastRegisteredWsUuid)) return;

            JsonObject reg = new JsonObject();
            reg.addProperty("type", "register");
            reg.addProperty("uuid", uuid.toString());
            ws.sendText(reg.toString(), true);
            lastRegisteredWsUuid = uuid;
        } catch (Exception e) {
            darkvisuals.LOGGER.debug("[Bridge] Failed to register WS uuid: {}", e.toString());
        }
    }

      
    public static void sendEmote(String emoteId) {
        if (!wsConnected || ws == null) return;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "emote");
            obj.addProperty("uuid", mc.player.getUuid().toString());
            obj.addProperty("emote_id", emoteId);
            ws.sendText(obj.toString(), true);
        } catch (Exception e) {
            darkvisuals.LOGGER.debug("[Bridge] Failed to send emote: {}", e.toString());
        }
    }

      
    private static final class Listener implements WebSocket.Listener {

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            try {
                JsonObject obj = JsonParser.parseString(data.toString()).getAsJsonObject();
                String type = obj.get("type").getAsString();
                if ("emote".equals(type)) {
                    UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                    String emoteId = obj.get("emote_id").getAsString();

                     
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player != null && mc.player.getUuid().equals(uuid)) {
                        return CompletableFuture.completedFuture(null);
                    }

                    if (emoteId.isEmpty()) {
                        EmoteManager.stop(uuid);
                    } else {
                        EmoteManager.playRemote(uuid, emoteId);
                    }
                }
            } catch (Exception ignored) {}
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            wsConnected = false;
            lastRegisteredWsUuid = null;
            scheduler.schedule(CosmeticsSyncManager::connectWs, 10, TimeUnit.SECONDS);
            return CompletableFuture.completedFuture(null);
        }

        public void onError(WebSocket webSocket, Throwable error) {
            wsConnected = false;
            lastRegisteredWsUuid = null;
            scheduler.schedule(CosmeticsSyncManager::connectWs, 10, TimeUnit.SECONDS);
        }
    }
}
