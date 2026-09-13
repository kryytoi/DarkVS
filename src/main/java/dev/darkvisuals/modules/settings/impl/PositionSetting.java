package dev.darkvisuals.modules.settings.impl;

import dev.darkvisuals.modules.settings.Setting;
import dev.darkvisuals.modules.settings.api.Position;

import java.util.function.Supplier;

public class PositionSetting extends Setting<Position> {

    public PositionSetting(String name, Position defaultValue) {
        super(name, defaultValue);
    }

    public PositionSetting(String name, Position defaultValue, Supplier<Boolean> visible) {
        super(name, defaultValue, visible);
    }
}