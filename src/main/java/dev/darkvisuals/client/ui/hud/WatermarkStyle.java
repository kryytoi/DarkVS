package dev.darkvisuals.client.ui.hud;

import dev.darkvisuals.modules.settings.api.Nameable;

public enum WatermarkStyle implements Nameable {
    DEFAULT("Обычный"),
    DISCO("Диско");

    private final String displayName;

    WatermarkStyle(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return displayName;
    }
}
