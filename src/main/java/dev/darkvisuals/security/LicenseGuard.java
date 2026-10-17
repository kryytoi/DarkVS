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
        return true;
    }

    public static boolean verifyOnStartup() {
        VALID.set(true);
        return true;
    }

    public static void tick() {
    }

    private static boolean checkNow() {
        VALID.set(true);
        return true;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
