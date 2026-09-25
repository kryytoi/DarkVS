package dev.darkvisuals.client.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darkvisuals.darkvisuals;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * DARKA-0.1 — встроенный ИИ-ассистент клиента DarkVS.
 * Поддерживает Hugging Face Serverless, OpenRouter и автоматический бесплатный Fallback.
 */
public class DarkaManager {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Заголовок браузера для обхода WAF/Cloudflare блокировок (403)
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    // Бесплатный Serverless роутер Hugging Face (с обязательным префиксом /hf-inference/)
    private static final String HF_CHAT_URL = "https://router.huggingface.co/hf-inference/v1/chat/completions";
    private static final String HF_DEFAULT_MODEL = "deepseek-ai/DeepSeek-V4.1-Flash";

    // OpenRouter (на случай ключей sk-or-... / sk-...)
    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String OPENROUTER_DEFAULT_MODEL = "qwen/qwen-2.5-7b-instruct";

    // Открытый бесплатный резервный шлюз без необходимости API-ключа
    private static final String FREE_FALLBACK_URL = "https://text.pollinations.ai/openai/chat/completions";
    private static final String FREE_DEFAULT_MODEL = "qwen";

    public static class Message {
        public final boolean user;
        public final String text;
        public Message(boolean user, String text) { this.user = user; this.text = text; }
    }

    private static final List<Message> history = new ArrayList<>();

    private static String systemPrompt() {
        return "Ты — DARKA-0.1, встроенный ИИ-ассистент клиента DarkVS для Minecraft. "
                + "Отвечай кратко, на русском языке, дружелюбно. Помогаешь игроку с клиентом, "
                + "модами и общими вопросами по Minecraft.";
    }

    private static volatile boolean busy = false;

    private DarkaManager() {}

    public static boolean isBusy() { return busy; }

    public static List<Message> getHistory() { return history; }

    public static void clearHistory() { history.clear(); }

    private static String apiKey() {
        var ui = darkvisuals.getInstance().getModuleManager()
                .getModule(dev.darkvisuals.modules.impl.render.UI.class);
        return ui == null ? "" : ui.getDarkaApiKey();
    }

    public static boolean hasApiKey() {
        String k = apiKey();
        return k != null && !k.isBlank();
    }

    /** Добавить сообщение игрока и отправить запрос модели. */
    public static CompletableFuture<Void> ask(String userText) {
        if (userText == null || userText.isBlank()) return CompletableFuture.completedFuture(null);
        history.add(new Message(true, userText));
        if (busy) return CompletableFuture.completedFuture(null);
        busy = true;

        String rawKey = apiKey();
        String key = rawKey != null ? rawKey.trim() : "";

        // Если ключ не задан, сразу используем бесплатный шлюз
        if (key.isEmpty()) {
            return executeChatRequest(FREE_FALLBACK_URL, null, FREE_DEFAULT_MODEL, false);
        }

        // Если ключ от OpenRouter
        if (key.startsWith("sk-or-") || key.startsWith("sk-")) {
            return executeChatRequest(OPENROUTER_URL, key, OPENROUTER_DEFAULT_MODEL, true);
        }

        // По умолчанию для Hugging Face
        return executeChatRequest(HF_CHAT_URL, key, HF_DEFAULT_MODEL, true);
    }

    private static CompletableFuture<Void> executeChatRequest(String url, String key, String model, boolean allowFallback) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(35));

        if (key != null && !key.isBlank()) {
            builder.header("Authorization", "Bearer " + key);
        }

        if (url.contains("openrouter.ai")) {
            builder.header("HTTP-Referer", "https://github.com/kryytoi/darkvs");
            builder.header("X-Title", "DarkVS");
        }

        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(buildChatBody(model)))
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(resp -> {
                    if (resp.statusCode() == 200) {
                        String answer = parseAnswer(resp.body());
                        if (!answer.isEmpty() && !answer.startsWith("Ошибка API:")) {
                            onMain(() -> {
                                busy = false;
                                history.add(new Message(false, answer));
                            });
                            return CompletableFuture.completedFuture(null);
                        }
                    }

                    // Если провайдер вернул 401, 403 (запрет доступа) или ошибку — переключаемся на Fallback
                    if (allowFallback) {
                        return executeChatRequest(FREE_FALLBACK_URL, null, FREE_DEFAULT_MODEL, false);
                    }

                    onMain(() -> {
                        busy = false;
                        history.add(new Message(false, describeError(resp.statusCode(), resp.body())));
                    });
                    return CompletableFuture.completedFuture(null);
                })
                .exceptionallyCompose(e -> {
                    if (allowFallback) {
                        return executeChatRequest(FREE_FALLBACK_URL, null, FREE_DEFAULT_MODEL, false);
                    }
                    onMain(() -> {
                        busy = false;
                        String msg = e.getMessage();
                        if (e.getCause() != null && e.getCause().getMessage() != null) msg = e.getCause().getMessage();
                        history.add(new Message(false, "Сетевая ошибка: " + msg));
                    });
                    return CompletableFuture.completedFuture(null);
                });
    }

    private static String describeError(int code, String body) {
        String apiError = extractApiError(body);
        switch (code) {
            case 401: return "Ключ отклонён (401). Проверь токен в настройках DARKA-0.1.";
            case 403: return "Доступ запрещён (403)." + (apiError.isEmpty() ? " Проверь права токена (Inference API)." : " " + apiError);
            case 404: return "Модель недоступна (404). Попробуй позже.";
            case 429: return "Слишком много запросов (429). Подожди немного.";
            case 503: return "Сервер перегружен (503). Попробуй через 20 секунд.";
            default: return "Ошибка " + code + (apiError.isEmpty() ? ". Проверь интернет." : ". " + apiError);
        }
    }

    private static String extractApiError(String body) {
        try {
            var element = JsonParser.parseString(body);
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                var f = obj.get("error");
                if (f == null || f.isJsonNull()) f = obj.get("detail");
                if (f != null && !f.isJsonNull()) {
                    return f.isJsonPrimitive() ? f.getAsString() : f.toString();
                }
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private static String buildChatBody(String model) {
        int start = Math.max(0, history.size() - 6);
        List<Message> recent = history.subList(start, history.size());

        JsonArray messages = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt());
        messages.add(sys);

        for (Message m : recent) {
            JsonObject msg = new JsonObject();
            msg.addProperty("role", m.user ? "user" : "assistant");
            msg.addProperty("content", m.text);
            messages.add(msg);
        }

        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.add("messages", messages);
        root.addProperty("max_tokens", 400);
        root.addProperty("temperature", 0.7);
        return root.toString();
    }

    private static String parseAnswer(String body) {
        try {
            var element = JsonParser.parseString(body);
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("error")) return "Ошибка API: " + extractApiError(body);
                if (obj.has("choices") && obj.getAsJsonArray("choices").size() > 0) {
                    JsonObject first = obj.getAsJsonArray("choices").get(0).getAsJsonObject();
                    if (first.has("message") && first.getAsJsonObject("message").has("content")) {
                        return first.getAsJsonObject("message").get("content").getAsString().trim();
                    }
                    if (first.has("text")) return first.get("text").getAsString().trim();
                }
            }
        } catch (Throwable ignored) {}

        if (body != null && !body.isBlank() && !body.trim().startsWith("<") && !body.trim().startsWith("{")) {
            return body.trim();
        }
        return "";
    }

    private static void onMain(Runnable r) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) r.run(); else client.execute(r);
    }
}