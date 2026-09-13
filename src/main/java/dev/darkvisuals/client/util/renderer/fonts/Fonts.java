package dev.darkvisuals.client.util.renderer.fonts;

import dev.darkvisuals.client.render.msdf.MsdfFont;
import dev.darkvisuals.darkvisuals;

 public final class Fonts {

    public static Font BOLD;
    public static Font MEDIUM;
    public static Font REGULAR;
    public static Font SEMIBOLD;
    public static Font ICONS;
    public static Font GEISTMONO;

    private static volatile boolean loaded = false;
      
    private static volatile boolean loggedError = false;

    private Fonts() {}

      
    public static boolean isLoaded() {
        return loaded;
    }

     public static synchronized void reload() {
        loaded = false;
        loggedError = false;
        load();
    }

     public static synchronized boolean load() {
        if (loaded) return true;

        try {
             
             
             
            Font bold      = new Font(MsdfFont.builder().atlas("sf_bold").data("sf_bold").build());
            Font medium    = new Font(MsdfFont.builder().atlas("sf_medium").data("sf_medium").build());
            Font regular   = new Font(MsdfFont.builder().atlas("sf_regular").data("sf_regular").build());
            Font semibold  = new Font(MsdfFont.builder().atlas("sf_semibold").data("sf_semibold").build());
            Font icons     = new Font(MsdfFont.builder().atlas("icons").data("icons").build());
            Font geistmono = new Font(MsdfFont.builder().atlas("geistmono-black").data("geistmono-black").build());

            BOLD = bold;
            MEDIUM = medium;
            REGULAR = regular;
            SEMIBOLD = semibold;
            ICONS = icons;
            GEISTMONO = geistmono;

            loaded = true;
            darkvisuals.LOGGER.info("[DarkVisuals] Fonts loaded successfully.");
            return true;
        } catch (Throwable t) {
             
             
            if (!loggedError) {
                 
                 
                loggedError = true;
                darkvisuals.LOGGER.error("[DarkVisuals] Не удалось собрать шрифты (реальная причина ниже):", t);
            } else {
                darkvisuals.LOGGER.warn("[DarkVisuals] Fonts not ready yet, will retry: {}", t.toString());
            }
            return false;
        }
    }
}
