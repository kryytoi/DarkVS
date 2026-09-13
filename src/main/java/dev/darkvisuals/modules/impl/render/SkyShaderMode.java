package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.modules.settings.api.Nameable;


public enum SkyShaderMode implements Nameable {
    SKY("Sky"),
    WATER("Water"),
    CAUSTIC("Caustic"),
    AURORA("Aurora"),
    NEBULA("Nebula"),
    VORTEX("Vortex"),
    RAINBOW("Rainbow"),
    STARS("Stars");

    private final String displayName;

    SkyShaderMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return displayName;
    }
}
