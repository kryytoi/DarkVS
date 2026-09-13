package dev.darkvisuals.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

 
public final class LicenseGuard {

    private static final String SERVER_BASE_URL = "https://darkvisuals.vercel.app";
    private static final Duration RECHECK_INTERVAL = Duration.ofMinutes(20);

    private static final AtomicBoolean VALID = new AtomicBoolean(false);
    private static volatile Instant lastCheck = Instant.EPOCH;

    private LicenseGuard() {}

    public static boolean isValid() {
        return VALID.get();
    }


    public static boolean verifyOnStartup() {
        boolean ok = checkNow();
        VALID.set(ok);
        return ok;
    }


    public static void tick() {

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) return;
        if (Instant.now().isBefore(lastCheck.plus(RECHECK_INTERVAL))) return;
        VALID.set(checkNow());
    }

    private static boolean checkNow() {
        lastCheck = Instant.now();


        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return true;
        }

        try {
            File sessionFile = new File(
                    MinecraftClient.getInstance().runDirectory,
                    "darkvisuals" + File.separator + "session.json");

            if (!sessionFile.exists()) return false;

            JsonObject session = JsonParser.parseString(
                    Files.readString(sessionFile.toPath())).getAsJsonObject();

            String login = session.get("login").getAsString();
            String hwid = session.get("hwid").getAsString();
            String token = session.get("sessionToken").getAsString();

            String body = "{\"login\":\"" + escape(login)
                    + "\",\"hwid\":\"" + escape(hwid)
                    + "\",\"sessionToken\":\"" + escape(token) + "\"}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_BASE_URL + "/api/verify"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) return false;

            JsonObject result = JsonParser.parseString(resp.body()).getAsJsonObject();
            return result.has("valid") && result.get("valid").getAsBoolean();

        } catch (IOException | InterruptedException | RuntimeException e) {

            return false;
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}