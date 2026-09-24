package dev.darkvisuals.client.ui.hud;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.impl.render.HUD;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public final class CustomLogo {

    private static final Identifier ID = Identifier.of("darkvisuals", "dynamic/custom_logo");
    private static String loadedPath = "";
    private static long loadedStamp = -1;
    private static long lastCheck;
    private static boolean ready;

    private CustomLogo() {}

    public static Identifier get(Identifier fallback) {
        HUD hud = HUD.getInstance();
        if (hud == null || !hud.isCustomLogo()) return fallback;

        long now = System.currentTimeMillis();
        if (now - lastCheck > 1000) {
            lastCheck = now;
            File f = new File(darkvisuals.getInstance().getGlobalsDir(), hud.getLogoFile());
            if (!f.isFile()) {
                ready = false;
                loadedPath = "";
            } else if (!f.getAbsolutePath().equals(loadedPath) || f.lastModified() != loadedStamp) {
                load(f);
                loadedPath = f.getAbsolutePath();
                loadedStamp = f.lastModified();
            }
        }
        return ready ? ID : fallback;
    }

    public static void invalidate() {
        lastCheck = 0;
        loadedStamp = -1;
    }

    private static void load(File f) {
        try (InputStream in = new FileInputStream(f)) {
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            MinecraftClient.getInstance().getTextureManager().registerTexture(ID, tex);
            tex.setFilter(true, false);
            ready = true;
        } catch (Exception e) {
            ready = false;
            darkvisuals.LOGGER.warn("[DarkVisuals] Логотип не загрузился {}: {}", f, e.getMessage());
        }
    }
}