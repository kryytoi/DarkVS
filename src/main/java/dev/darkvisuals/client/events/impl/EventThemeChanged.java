package dev.darkvisuals.client.events.impl;

import dev.darkvisuals.client.events.Event;
import dev.darkvisuals.client.managers.ThemeManager;

public class EventThemeChanged extends Event {
    private final ThemeManager.Theme theme;
    
    public EventThemeChanged(ThemeManager.Theme theme) {
        this.theme = theme;
    }
    
    public ThemeManager.Theme getTheme() {
        return theme;
    }
} 