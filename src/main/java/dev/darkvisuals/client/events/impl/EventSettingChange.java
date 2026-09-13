package dev.darkvisuals.client.events.impl;

import dev.darkvisuals.client.events.Event;
import dev.darkvisuals.modules.settings.Setting;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class EventSettingChange extends Event {
    private final Setting<?> setting;
}