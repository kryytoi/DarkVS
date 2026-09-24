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
 * DARKA-0.1 — встроенный ИИ-ассистент на базе Hugging Face Inference Providers.
 * Использует OpenAI-совместимый роутер router.huggingface.co/v1/chat/completions
 * (легаси-эндпоинт api-inference.huggingface.co отключён HF).
 * Хранит историю диалога и отправляет запросы асинхронно, не блокируя рендер.
 */
public class DarkaManager {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // OpenAI-совместимый роутер Hugging Face.
    private static final String CHAT_URL = "https://router.huggingface.co/v1/chat/completions";

    // Chat-модель по умолчанию: не закрыта лицензией, хорошо говорит по-русски.
    // Можно будет добавить выбор в настройки позже.
    private static final String DEFAULT_MODEL = "Qwen/Qwen2.5-7B-Instruct";

    public static class Message {
        public final boolean user;   // true — игрок, false — DARKA
        public final String text;
        public Message(boolean user, String text) { this.user = user; this.text = text; }
    }

    private static final List<Message> history = new ArrayList<>();

    /** Системный промпт — личность ассистента внутри клиента DarkVS. */
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

    /** API-ключ хранится в настройках модуля UI. */
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

        String key = apiKey();
        if (key == null || key.isBlank()) {
            busy = false;
            history.add(new Message(false, "API-ключ не задан. Открой настройки DARKA-0.1 и вставь токен Hugging Face."));
            return CompletableFuture.completedFuture(null);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_URL))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildChatBody()))
                .timeout(Duration.ofSeconds(45))
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> onMain(() -> {
                    busy = false;
                    if (resp.statusCode() == 200) {
                        String answer = parseAnswer(resp.body());
                        history.add(new Message(false, answer.isEmpty() ? "Пустой ответ модели." : answer));
                    } else {
                        history.add(new Message(false, describeError(resp.statusCode(), resp.body())));
                    }
                }))
                .exceptionally(e -> {
                    busy = false;
                    String msg = e.getMessage();
                    if (e.getCause() != null && e.getCause().getMessage() != null) msg = e.getCause().getMessage();
                    final String fmsg = msg;
                    onMain(() -> history.add(new Message(false, "Сетевая ошибка: " + fmsg)));
                    return null;
                });
    }

    /** Человекочитаемое описание ошибки по HTTP-коду и телу ответа. */
    private static String describeError(int code, String body) {
        String apiError = extractApiError(body);
        switch (code) {
            case 401: return "Ключ отклонён (401). Проверь токен Hugging Face в настройках DARKA-0.1.";
            case 403: return "Доступ запрещён (403)." + (apiError.isEmpty() ? " Возможно, модель закрыта лицензией." : " " + apiError);
            case 404: return "Модель недоступна (404). Попробуй другую модель или повтори позже.";
            case 429: return "Слишком много запросов (429). Подожди немного и попробуй снова.";
            case 503: return "Сервер перегружен (503). Попробуй снова через 20 секунд.";
            default: return "Ошибка " + code + (apiError.isEmpty() ? ". Проверь API-ключ и интернет." : ". " + apiError);
        }
    }

    /** Достаёт поле error/detail из JSON-ответа об ошибке. */
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

    /** Собирает JSON-тело запроса в OpenAI-формате chat-completions. */
    private static String buildChatBody() {
        // Берём последние 6 сообщений + системный промпт, чтобы не превышать лимит токенов
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
        root.addProperty("model", DEFAULT_MODEL);
        root.add("messages", messages);
        root.addProperty("max_tokens", 400);
        root.addProperty("temperature", 0.7);
        return root.toString();
    }

    /** Разбирает ответ роутера: {"choices":[{"message":{"content":"..."}}]}. */
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
                    // Некоторые провайдеры возвращают текст в поле text
                    if (first.has("text")) return first.get("text").getAsString().trim();
                }
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private static void onMain(Runnable r) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) r.run(); else client.execute(r);
    }
}
