package dev.darkvisuals.client.util.achievements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.renderer.Render2D;
import net.minecraft.client.texture.AbstractTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

 
public final class AchievementManager implements Wrapper {

    private static final String SERVER_BASE_URL = "https://darkvisuals.vercel.app";

    private static volatile List<Achievement> achievements = new CopyOnWriteArrayList<>();
    private static volatile boolean loading = false;
    private static volatile boolean loaded = false;

    private static final Map<String, AbstractTexture> iconCache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> iconLoadAttempted = new ConcurrentHashMap<>();

    private AchievementManager() {}

    public static List<Achievement> getAchievements() {
        return achievements;
    }

    public static boolean isLoaded() {
        return loaded;
    }

      
    public static void loadIfNeeded() {
        if (loading || loaded) return;
        loading = true;
        new Thread(AchievementManager::fetchAchievements, "AchievementManager-Fetch").start();
    }

      
    public static void reload() {
        loaded = false;
        loading = false;
        loadIfNeeded();
    }

    private static void fetchAchievements() {
        try {
            File sessionFile = new File(mc.runDirectory, "darkvisuals" + File.separator + "session.json");
            if (!sessionFile.exists()) {
                loading = false;
                loaded = true;
                return;
            }

            JsonObject session = JsonParser.parseString(Files.readString(sessionFile.toPath())).getAsJsonObject();
            String login = session.get("login").getAsString();
            String hwid = session.get("hwid").getAsString();
            String token = session.get("sessionToken").getAsString();

            String body = "{\"login\":\"" + escape(login)
                    + "\",\"hwid\":\"" + escape(hwid)
                    + "\",\"sessionToken\":\"" + escape(token) + "\"}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_BASE_URL + "/api/achievements"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                loading = false;
                loaded = true;
                return;
            }

            JsonObject result = JsonParser.parseString(resp.body()).getAsJsonObject();
            List<Achievement> parsed = new ArrayList<>();

            if (result.has("valid") && result.get("valid").getAsBoolean() && result.has("achievements")) {
                JsonArray arr = result.getAsJsonArray("achievements");
                for (JsonElement el : arr) {
                    JsonObject o = el.getAsJsonObject();
                    parsed.add(new Achievement(
                            getStr(o, "code"),
                            getStr(o, "name"),
                            getStr(o, "description"),
                            getStr(o, "imageUrl"),
                            getStr(o, "unlockFeature"),
                            getStr(o, "grantedAt")
                    ));
                }
            }

            achievements = new CopyOnWriteArrayList<>(parsed);

             
            for (Achievement a : parsed) {
                loadIconAsync(a);
            }
        } catch (Throwable ignored) {
            achievements = new CopyOnWriteArrayList<>(Collections.emptyList());
        } finally {
            loading = false;
            loaded = true;
        }
    }

      
    public static AbstractTexture getIcon(Achievement achievement) {
        if (achievement.imageUrl == null || achievement.imageUrl.isEmpty()) return null;
        AbstractTexture cached = iconCache.get(achievement.imageUrl);
        if (cached != null) return cached;
        loadIconAsync(achievement);
        return null;
    }

    private static void loadIconAsync(Achievement achievement) {
        String url = achievement.imageUrl;
        if (url == null || url.isEmpty()) return;
        if (iconLoadAttempted.putIfAbsent(url, Boolean.TRUE) != null) return;

        new Thread(() -> {
            try {
                BufferedImage img = ImageIO.read(new URL(url));
                if (img == null) return;
                mc.execute(() -> {
                    try {
                        iconCache.put(url, Render2D.convert(img));
                    } catch (Throwable ignored) {
                    }
                });
            } catch (Throwable ignored) {
            }
        }, "AchievementManager-Icon").start();
    }

    private static String getStr(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}