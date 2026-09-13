package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.modules.settings.api.Nameable;


public enum ParticlesMode implements Nameable {
    METEORITES("Meteorites"),
    LIGHTNING("Lightning");

    private final String displayName;

    ParticlesMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return displayName;
    }
}
