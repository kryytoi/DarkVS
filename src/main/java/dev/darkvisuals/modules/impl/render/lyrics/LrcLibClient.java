package dev.darkvisuals.modules.impl.render.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
    private static final String NETEASE_SEARCH = "https://music.163.com/api/search/get/web";
    private static final String NETEASE_LYRIC = "https://music.163.com/api/song/lyric";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    // /api/get отвечает быстро (~1 c), /api/search у lrclib регулярно думает 20+ c.
    // Раньше на поиск стоял общий таймаут 14 c — он не выдерживался никогда,
    // и каждая попытка падала с "request timed out". Разводим таймауты по типу запроса.
    private static final Duration TIMEOUT_GET = Duration.ofSeconds(15);
    private static final Duration TIMEOUT_SEARCH = Duration.ofSeconds(30);

    // In-memory cache to avoid repeated HTTP calls on loop/rewind
    private static final Map<String, List<LyricLine>> LYRICS_CACHE = new ConcurrentHashMap<>();

    // Результат последнего запроса для диагностики ("200, synced=0 строк" / "404" / "timeout" и т.п.)
    private static volatile String lastResult = "";

    public static String getLastResult() {
        return lastResult;
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private LrcLibClient() {}

    /**
     * Асинхронно достаёт синхронизированный текст трека.
     * Каскад: lrclib /get (точное совпадение, самый быстрый) -> lrclib /search -> NetEase.
     * Если синхронизированного текста нигде нет, последним шансом берём plain-текст
     * и раскладываем его по длительности трека.
     *
     * @param durationSec длительность трека в секундах (&lt;= 0 если неизвестна) — сильно
     *                    повышает точность подбора на lrclib.
     */
    public static CompletableFuture<List<LyricLine>> fetchLyricsAsync(String trackName, String artistName,
                                                                     long durationSec, boolean debugMode) {
        if (trackName == null || trackName.isBlank()) {
            lastResult = "пустое название трека";
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String cacheKey = (trackName.trim() + " - " + (artistName != null ? artistName.trim() : "")).toLowerCase();
        List<LyricLine> cached = LYRICS_CACHE.get(cacheKey);
        if (cached != null) {
            if (debugMode) {
                System.out.printf("[KineticLyrics] Взято из кэша: %s (%d строк)%n", cacheKey, cached.size());
            }
            lastResult = "из кэша: " + cached.size() + " строк";
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            String track = cleanTrackName(trackName);
            String artist = cleanArtistName(artistName);
            List<String> tried = new ArrayList<>();

            // Кандидат "на крайний случай": несинхронизированный текст.
            List<String> plainFallback = new ArrayList<>();

            // 1. Точное совпадение с длительностью — самый надёжный и быстрый путь.
            if (durationSec > 0) {
                List<LyricLine> r = lrcLibGet(track, artist, durationSec, plainFallback, tried, debugMode);
                if (!r.isEmpty()) return cache(cacheKey, r, "lrclib get+duration");
            }

            // 2. Точное совпадение без длительности.
            List<LyricLine> r = lrcLibGet(track, artist, -1, plainFallback, tried, debugMode);
            if (!r.isEmpty()) return cache(cacheKey, r, "lrclib get");

            // 3. Структурированный поиск (терпим к разнице в написании).
            r = lrcLibSearch("?track_name=" + enc(track) + "&artist_name=" + enc(artist),
                    "search(track+artist)", durationSec, plainFallback, tried, debugMode);
            if (!r.isEmpty()) return cache(cacheKey, r, "lrclib search(track+artist)");

            // 4. Свободный поиск одной строкой.
            r = lrcLibSearch("?q=" + enc((track + " " + artist).trim()),
                    "search(q)", durationSec, plainFallback, tried, debugMode);
            if (!r.isEmpty()) return cache(cacheKey, r, "lrclib search(q)");

            // 5. Поиск только по названию — спасает, когда Windows отдаёт мусорного исполнителя.
            if (!artist.isEmpty()) {
                r = lrcLibSearch("?q=" + enc(track), "search(title)", durationSec, plainFallback, tried, debugMode);
                if (!r.isEmpty()) return cache(cacheKey, r, "lrclib search(title)");
            }

            // 6. Сторонний провайдер.
            r = tryFetchNetEase(track, artist, tried, debugMode);
            if (!r.isEmpty()) return cache(cacheKey, r, "netease");

            // 7. Совсем ничего синхронизированного — раскладываем plain-текст по времени.
            if (!plainFallback.isEmpty()) {
                List<LyricLine> spread = spreadPlainLyrics(plainFallback.get(0), durationSec);
                if (!spread.isEmpty()) {
                    lastResult = "только несинхронизированный текст: " + spread.size()
                            + " строк (подгоните «Смещение»)";
                    LYRICS_CACHE.put(cacheKey, spread);
                    return spread;
                }
            }

            lastResult = "не найдено; " + String.join("; ", tried);
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

    // ---------------------------------------------------------------- lrclib

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

            // Сначала кандидаты с подходящей длительностью (±3 c), затем все остальные.
            List<JsonObject> ordered = new ArrayList<>();
            List<JsonObject> rest = new ArrayList<>();
            for (JsonElement elem : array) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();
                if (durationSec > 0 && obj.has("duration") && !obj.get("duration").isJsonNull()) {
                    double d = obj.get("duration").getAsDouble();
                    if (Math.abs(d - durationSec) <= 3.0) {
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

    /** Достаёт syncedLyrics из записи lrclib; plainLyrics складывает про запас. */
    private static List<LyricLine> extractFromEntry(JsonObject obj, String label,
                                                    List<String> plainFallback, List<String> tried) {
        if (obj.has("instrumental") && !obj.get("instrumental").isJsonNull()
                && obj.get("instrumental").getAsBoolean()) {
            tried.add(label + ": трек помечен инструментальным");
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

    // --------------------------------------------------------------- NetEase

    private static List<LyricLine> tryFetchNetEase(String track, String artist, List<String> tried, boolean debugMode) {
        String searchQuery = (track + " " + (artist != null ? artist : "")).trim();
        if (searchQuery.isBlank()) return Collections.emptyList();

        try {
            String url = NETEASE_SEARCH + "?s=" + enc(searchQuery) + "&type=1&limit=5&offset=0";
            HttpResponse<String> response = send(url, TIMEOUT_GET, "https://music.163.com");
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                tried.add("netease: HTTP " + response.statusCode());
                return Collections.emptyList();
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!json.has("result") || !json.get("result").isJsonObject()) {
                tried.add("netease: пустой результат");
                return Collections.emptyList();
            }

            JsonObject result = json.getAsJsonObject("result");
            if (!result.has("songs") || !result.get("songs").isJsonArray()) {
                tried.add("netease: songs не найдены");
                return Collections.emptyList();
            }

            // Берём до 3 песен из выдачи — первая может оказаться кавером/инструменталом
            JsonArray songs = result.getAsJsonArray("songs");
            int limit = Math.min(3, songs.size());
            for (int i = 0; i < limit; i++) {
                JsonObject song = songs.get(i).getAsJsonObject();
                if (!song.has("id")) continue;
                long songId = song.get("id").getAsLong();

                List<LyricLine> parsed = fetchNetEaseLyric(songId);
                if (!parsed.isEmpty()) {
                    if (debugMode) {
                        System.out.println("[KineticLyrics] Найден текст через NetEase (songId=" + songId + ")");
                    }
                    return parsed;
                }
            }
            tried.add("netease: синхротекста нет");
        } catch (Exception e) {
            tried.add("netease: " + shortErr(e));
        }
        return Collections.emptyList();
    }

    private static List<LyricLine> fetchNetEaseLyric(long songId) {
        try {
            String url = NETEASE_LYRIC + "?id=" + songId + "&lv=1&kv=1&tv=-1";
            HttpResponse<String> response = send(url, TIMEOUT_GET, "https://music.163.com");
            if (response.statusCode() != 200) return Collections.emptyList();

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!json.has("lrc") || !json.get("lrc").isJsonObject()) return Collections.emptyList();
            JsonObject lrc = json.getAsJsonObject("lrc");
            if (!lrc.has("lyric") || lrc.get("lyric").isJsonNull()) return Collections.emptyList();

            String lrcText = lrc.get("lyric").getAsString();
            if (lrcText == null || lrcText.isBlank()) return Collections.emptyList();

            return LrcParser.parse(lrcText);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    // ----------------------------------------------------------------- utils

    private static HttpResponse<String> send(String url, Duration timeout) throws Exception {
        return send(url, timeout, null);
    }

    private static HttpResponse<String> send(String url, Duration timeout, String referer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .timeout(timeout)
                .GET();
        if (referer != null) b.header("Referer", referer);
        return HTTP_CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String shortErr(Exception e) {
        String msg = e.getMessage();
        return e.getClass().getSimpleName() + (msg != null && !msg.isBlank() ? " — " + msg : "");
    }

    /**
     * Равномерно раскладывает несинхронизированный текст по длительности трека.
     * Это заведомо приблизительно, зато лучше пустого экрана — точную подгонку
     * пользователь делает настройкой «Смещение (мс)».
     */
    private static List<LyricLine> spreadPlainLyrics(String plain, long durationSec) {
        if (plain == null || plain.isBlank()) return Collections.emptyList();

        List<String> texts = new ArrayList<>();
        for (String raw : plain.split("\\r?\\n")) {
            String t = raw.trim();
            if (!t.isEmpty()) texts.add(t);
        }
        if (texts.isEmpty()) return Collections.emptyList();

        // Без известной длительности считаем ~3.5 c на строку.
        long totalMs = durationSec > 0 ? durationSec * 1000L : texts.size() * 3500L;
        // Небольшой отступ от начала: почти во всех треках есть интро.
        long leadInMs = Math.min(6000L, totalMs / 12);
        long usableMs = Math.max(1000L, totalMs - leadInMs);
        long stepMs = Math.max(900L, usableMs / texts.size());

        List<LyricLine> out = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            out.add(new LyricLine(leadInMs + i * stepMs, texts.get(i)));
        }
        return out;
    }

    /**
     * Приводит название трека к виду, который реально ищется на lrclib.
     * Windows отдаёт заголовок как есть — с "(feat. X)", "- Remastered 2011",
     * "[Official Video]" и прочим мусором, из-за которого точный поиск даёт 404.
     */
    private static String cleanTrackName(String trackName) {
        if (trackName == null) return "";
        String s = trackName;

        s = s.replaceAll("(?i)\\(\\s*\\b(?:feat|ft|with|prod)\\b\\.?[^)]*\\)", " ");
        s = s.replaceAll("(?i)\\[\\s*\\b(?:feat|ft|with|prod)\\b\\.?[^\\]]*\\]", " ");
        s = s.replaceAll("(?i)\\((?:official|lyric|audio|video|music video|visualizer|remaster|remastered|hd|4k|explicit)[^)]*\\)", " ");
        s = s.replaceAll("(?i)\\[(?:official|lyric|audio|video|music video|visualizer|remaster|remastered|hd|4k|explicit)[^\\]]*\\]", " ");
        // Хвосты после дефиса: "Song - Remastered 2011", "Song - Radio Edit"
        s = s.replaceAll("(?i)\\s-\\s(?:remaster(?:ed)?|radio edit|single version|album version|bonus track|live)\\b.*$", " ");
        // \b обязателен: без него "ft" матчится внутри слов ("Soft rain" -> "So").
        s = s.replaceAll("(?i)\\s*[-–—]?\\s*\\b(?:feat|ft)\\b\\.?\\s+.*$", " ");
        s = s.replaceAll("\\s{2,}", " ");

        String cleaned = s.trim();
        // Если чистка съела всё — возвращаем исходник, иначе искать будет нечего.
        return cleaned.isEmpty() ? trackName.trim() : cleaned;
    }

    /** У исполнителя убираем перечисления через запятую/&amp; — lrclib хранит только основного. */
    private static String cleanArtistName(String artistName) {
        if (artistName == null) return "";
        String s = artistName.split("(?i)\\s*(?:,|&|;|\\bfeat\\.?\\b|\\bft\\.?\\b)\\s*")[0];
        String cleaned = s.trim();
        return cleaned.isEmpty() ? artistName.trim() : cleaned;
    }
}
