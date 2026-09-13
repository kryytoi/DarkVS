package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.modules.settings.api.Nameable;

public enum JumpCircleMode implements Nameable {
    JAGGED_RING("Jagged Ring"),
    THICK_RING("Thick Ring"),
    OCTAGON("Octagon"),
    LATTICE("Lattice"),
    NOVA("Nova");

    private final String displayName;

    JumpCircleMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getName() {
        return displayName;
    }
}
