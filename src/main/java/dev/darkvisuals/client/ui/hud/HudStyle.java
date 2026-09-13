package dev.darkvisuals.client.ui.hud;

import dev.darkvisuals.modules.impl.render.HUD;
import dev.darkvisuals.modules.settings.api.Nameable;

 
public enum HudStyle implements Nameable {
    GLOWING("Glowing"),
    MINIMALISTIC("Minimalistic");

    private final String displayName;

    HudStyle(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return displayName;
    }

      
    public static HudStyle current() {
        HUD hud = HUD.getInstance();
        if (hud != null) {
            return hud.getHudStyle();
        }
        return GLOWING;
    }

    public static boolean isGlowing() {
        return current() == GLOWING;
    }

    public static boolean isMinimalistic() {
        return current() == MINIMALISTIC;
    }
}