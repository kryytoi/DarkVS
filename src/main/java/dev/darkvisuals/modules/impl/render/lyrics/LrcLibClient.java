package dev.darkvisuals.modules.impl.render.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class LrcLibClient {
    private static final String API_GET = "https://lrclib.net/api/get";
    private static final String API_SEARCH = "https://lrclib.net/api/search";
    private static final String UA = "DarkVisuals/1.0 (Minecraft Mod; https://github.com/kryytoi/DarkVS)";

    private static final Duration TIMEOUT_GET = Duration.ofSeconds(5);
    private static final Duration TIMEOUT_SEARCH = Duration.ofSeconds(6);

    private static final Map<String, List<LyricLine>> LYRICS_CACHE = new ConcurrentHashMap<>();

    private static volatile String lastResult = "";

    public static String getLastResult() {
        return lastResult;
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private LrcLibClient() {}

    public static CompletableFuture<List<LyricLine>> fetchLyricsAsync(String trackName, String artistName,
                                                                     long durationSec, boolean debugMode) {
        if (trackName == null || trackName.isBlank()) {
            lastResult = "пустое название";
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String cacheKey = (trackName.trim() + " - " + (artistName != null ? artistName.trim() : "")).toLowerCase();
        List<LyricLine> cached = LYRICS_CACHE.get(cacheKey);
        if (cached != null) {
            if (debugMode) {
                System.out.printf("[KineticLyrics] Из кэша: %s (%d строк)%n", cacheKey, cached.size());
            }
            lastResult = "кэш: " + cached.size() + " строк";
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            String rawTrack = trackName.trim();
            String rawArtist = artistName != null ? artistName.trim() : "";

            if (isGenericArtist(rawArtist)) {
                rawArtist = "";
            }

            List<String[]> candidates = new ArrayList<>();

            String[] splitParts = rawTrack.split("\\s+[-–—|:]\\s+", 2);
            if (splitParts.length == 2 && !splitParts[0].isBlank() && !splitParts[1].isBlank()) {
                candidates.add(new String[]{cleanTrackName(splitParts[1]), cleanArtistName(splitParts[0])});
                candidates.add(new String[]{cleanTrackName(splitParts[0]), cleanArtistName(splitParts[1])});
            }

            String cTrack = cleanTrackName(rawTrack);
            String cArtist = cleanArtistName(rawArtist);
            candidates.add(new String[]{cTrack, cArtist});

            if (!rawTrack.equalsIgnoreCase(cTrack) || !rawArtist.equalsIgnoreCase(cArtist)) {
                candidates.add(new String[]{rawTrack, rawArtist});
            }

            for (String[] cand : candidates) {
                List<LyricLine> local = checkLocalLyrics(cand[0], cand[1]);
                if (!local.isEmpty()) return cache(cacheKey, local, "локальный .lrc");
            }

            List<String> tried = new ArrayList<>();
            List<String> plainFallback = new ArrayList<>();

            for (String[] cand : candidates) {
                String t = cand[0];
                String a = cand[1];
                if (t.isBlank()) continue;

                String qStr = (a.isBlank() ? t : a + " " + t).trim();
                List<LyricLine> r = lrcLibSearch("?q=" + enc(qStr), "search(" + qStr + ")", durationSec, plainFallback, tried, debugMode);
                if (!r.isEmpty()) return cache(cacheKey, r, "lrclib search(q)");

                if (!a.isBlank()) {
                    String qRev = (t + " " + a).trim();
                    r = lrcLibSearch("?q=" + enc(qRev), "search(" + qRev + ")", durationSec, plainFallback, tried, debugMode);
                    if (!r.isEmpty()) return cache(cacheKey, r, "lrclib search(rev)");

                    if (durationSec > 0) {
                        r = lrcLibGet(t, a, durationSec, plainFallback, tried, debugMode);
                        if (!r.isEmpty()) return cache(cacheKey, r, "lrclib get+dur");
                    }
                    r = lrcLibGet(t, a, -1, plainFallback, tried, debugMode);
                    if (!r.isEmpty()) return cache(cacheKey, r, "lrclib get");
                }

                if (!t.equalsIgnoreCase(qStr)) {
                    r = lrcLibSearch("?q=" + enc(t), "search(" + t + ")", durationSec, plainFallback, tried, debugMode);
                    if (!r.isEmpty()) return cache(cacheKey, r, "lrclib search(title)");
                }
            }

            if (!plainFallback.isEmpty()) {
                List<LyricLine> spread = spreadPlainLyrics(plainFallback.get(0), durationSec);
                if (!spread.isEmpty()) {
                    lastResult = "найден текст: " + spread.size() + " строк";
                    LYRICS_CACHE.put(cacheKey, spread);
                    return spread;
                }
            }

            lastResult = "не найдено (" + (candidates.isEmpty() ? rawTrack : candidates.get(0)[0]) + ")";
            return Collections.emptyList();
        });
    }

    private static List<LyricLine> cache(String key, List<LyricLine> lines, String source) {
        LYRICS_CACHE.put(key, lines);
        lastResult = "OK (" + source + "): " + lines.size() + " строк";
        return lines;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static List<LyricLine> checkLocalLyrics(String track, String artist) {
        if (track == null || track.isBlank()) return Collections.emptyList();
        File[] dirs = new File[] {
                new File("darkvisuals/lyrics"),
                new File("lyrics"),
                new File("config/darkvisuals/lyrics")
        };
        String normT = track.toLowerCase().replaceAll("[^a-zA-Zа-яА-Я0-9]", "");
        String normA = artist != null ? artist.toLowerCase().replaceAll("[^a-zA-Zа-яА-Я0-9]", "") : "";

        for (File dir : dirs) {
            if (!dir.exists() || !dir.isDirectory()) continue;
            File[] files = dir.listFiles((d, name) -> {
                String l = name.toLowerCase();
                return l.endsWith(".lrc") || l.endsWith(".txt");
            });
            if (files == null) continue;
            for (File file : files) {
                String fname = file.getName().toLowerCase().replaceAll("[^a-zA-Zа-яА-Я0-9]", "");
                if (fname.contains(normT) && (normA.isBlank() || fname.contains(normA))) {
                    try {
                        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                        List<LyricLine> parsed = LrcParser.parse(content);
                        if (!parsed.isEmpty()) return parsed;
                    } catch (Exception ignored) {}
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<LyricLine> lrcLibGet(String track, String artist, long durationSec,
                                             List<String> plainFallback, List<String> tried, boolean debugMode) {
        StringBuilder q = new StringBuilder("?track_name=").append(enc(track))
                .append("&artist_name=").append(enc(artist));
        if (durationSec > 0) {
            q.append("&duration=").append(durationSec);
        }
        String label = durationSec > 0 ? "get+dur" : "get";

        try {
            HttpResponse<String> response = send(API_GET + q, TIMEOUT_GET);
            if (response.statusCode() != 200) {
                tried.add(label + ": HTTP " + response.statusCode());
                return Collections.emptyList();
            }

            JsonElement parsed = JsonParser.parseString(response.body());
            if (!parsed.isJsonObject()) {
                tried.add(label + ": не объект");
                return Collections.emptyList();
            }
            return extractFromEntry(parsed.getAsJsonObject(), label, plainFallback, tried);
        } catch (Exception e) {
            tried.add(label + ": " + shortErr(e));
            if (debugMode) {
                System.err.println("[KineticLyrics/Debug] " + label + " не удался: " + e);
            }
            return Collections.emptyList();
        }
    }

    private static List<LyricLine> lrcLibSearch(String query, String label, long durationSec,
                                                List<String> plainFallback, List<String> tried, boolean debugMode) {
        try {
            HttpResponse<String> response = send(API_SEARCH + query, TIMEOUT_SEARCH);
            if (response.statusCode() != 200) {
                tried.add(label + ": HTTP " + response.statusCode());
                return Collections.emptyList();
            }

            JsonElement parsed = JsonParser.parseString(response.body());
            if (!parsed.isJsonArray()) {
                tried.add(label + ": не массив");
                return Collections.emptyList();
            }

            JsonArray array = parsed.getAsJsonArray();
            if (array.size() == 0) {
                tried.add(label + ": 0 результатов");
                return Collections.emptyList();
            }

            List<JsonObject> ordered = new ArrayList<>();
            List<JsonObject> rest = new ArrayList<>();
            for (JsonElement elem : array) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();
                if (durationSec > 0 && obj.has("duration") && !obj.get("duration").isJsonNull()) {
                    double d = obj.get("duration").getAsDouble();
                    if (Math.abs(d - durationSec) <= 4.0) {
                        ordered.add(obj);
                        continue;
                    }
                }
                rest.add(obj);
            }
            ordered.addAll(rest);

            for (JsonObject obj : ordered) {
                List<LyricLine> found = extractFromEntry(obj, label, plainFallback, tried);
                if (!found.isEmpty()) {
                    if (debugMode) {
                        System.out.println("[KineticLyrics] Текст найден через " + label);
                    }
                    return found;
                }
            }

            tried.add(label + ": " + array.size() + " результатов, синхротекста нет");
        } catch (Exception e) {
            tried.add(label + ": " + shortErr(e));
            if (debugMode) {
                System.err.println("[KineticLyrics/Debug] " + label + " не удался: " + e);
            }
        }
        return Collections.emptyList();
    }

    private static List<LyricLine> extractFromEntry(JsonObject obj, String label,
                                                    List<String> plainFallback, List<String> tried) {
        if (obj.has("instrumental") && !obj.get("instrumental").isJsonNull()
                && obj.get("instrumental").getAsBoolean()) {
            tried.add(label + ": инструментал");
            return Collections.emptyList();
        }

        if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull()) {
            String synced = obj.get("syncedLyrics").getAsString();
            List<LyricLine> lines = LrcParser.parse(synced);
            if (!lines.isEmpty()) return lines;
        }

        if (obj.has("plainLyrics") && !obj.get("plainLyrics").isJsonNull()) {
            String plain = obj.get("plainLyrics").getAsString();
            if (plain != null && !plain.isBlank() && plainFallback.isEmpty()) {
                plainFallback.add(plain);
            }
        }
        return Collections.emptyList();
    }

    private static HttpResponse<String> send(String url, Duration timeout) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .timeout(timeout)
                .GET()
                .build();
        return HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static String shortErr(Exception e) {
        String msg = e.getMessage();
        return e.getClass().getSimpleName() + (msg != null && !msg.isBlank() ? " — " + msg : "");
    }

    private static List<LyricLine> spreadPlainLyrics(String plain, long durationSec) {
        if (plain == null || plain.isBlank()) return Collections.emptyList();

        List<String> texts = new ArrayList<>();
        for (String raw : plain.split("\\r?\\n")) {
            String t = raw.trim();
            if (!t.isEmpty()) texts.add(t);
        }
        if (texts.isEmpty()) return Collections.emptyList();

        long totalMs = durationSec > 0 ? durationSec * 1000L : texts.size() * 3500L;

        long leadInMs = Math.min(6000L, totalMs / 12);
        long usableMs = Math.max(1000L, totalMs - leadInMs);
        long stepMs = Math.max(900L, usableMs / texts.size());

        List<LyricLine> out = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            out.add(new LyricLine(leadInMs + i * stepMs, texts.get(i)));
        }
        return out;
    }

    private static String cleanTrackName(String trackName) {
        if (trackName == null) return "";
        String s = trackName;

        s = s.replaceAll("(?i)\\(\\s*\\b(?:feat|ft|with|prod)\\b\\.?[^)]*\\)", " ");
        s = s.replaceAll("(?i)\\[\\s*\\b(?:feat|ft|with|prod)\\b\\.?[^\\]]*\\]", " ");
        s = s.replaceAll("(?i)\\((?:official|lyric|audio|video|music video|visualizer|remaster|remastered|hd|4k|explicit|slowed|reverb|speed up|nightcore|клип|премьера|текст|слова|трек)[^)]*\\)", " ");
        s = s.replaceAll("(?i)\\[(?:official|lyric|audio|video|music video|visualizer|remaster|remastered|hd|4k|explicit|slowed|reverb|speed up|nightcore|клип|премьера|текст|слова|трек)[^\\]]*\\]", " ");

        s = s.replaceAll("(?i)\\s*[-–—|]\\s*(?:official|lyric|audio|video|remaster(?:ed)?|radio edit|single version|album version|bonus track|live|клип|премьера|текст).*$", " ");
        s = s.replaceAll("(?i)\\s*[-–—]?\\s*\\b(?:feat|ft)\\b\\.?\\s+.*$", " ");
        s = s.replaceAll("(?i)[«»\"']", " ");
        s = s.replaceAll("\\s{2,}", " ");

        String cleaned = s.trim();
        return cleaned.isEmpty() ? trackName.trim() : cleaned;
    }

    private static String cleanArtistName(String artistName) {
        if (artistName == null) return "";
        String s = artistName.split("(?i)\\s*(?:,|&|;|/|\\bfeat\\.?\\b|\\bft\\.?\\b)\\s*")[0];
        s = s.replaceAll("(?i)\\s*[-–—]?\\s*(?:topic|тема|канал|channel)$", "");
        s = s.replaceAll("(?i)[«»\"']", "");
        String cleaned = s.trim();
        return cleaned.isEmpty() ? artistName.trim() : cleaned;
    }

    private static boolean isGenericArtist(String a) {
        if (a == null || a.isBlank()) return true;
        String lower = a.toLowerCase();
        return lower.contains("youtube") || lower.contains("chrome") || lower.contains("edge")
                || lower.contains("opera") || lower.contains("firefox") || lower.contains("browser")
                || lower.contains("yandex") || lower.contains("яндекс") || lower.contains("музык")
                || lower.contains("music") || lower.contains("vlc") || lower.equals("spotify")
                || lower.equals("unknown") || lower.contains("плеер") || lower.contains("player")
                || lower.contains("вк") || lower.contains("vk") || lower.contains("media player")
                || lower.contains("неизвестный");
    }
}
