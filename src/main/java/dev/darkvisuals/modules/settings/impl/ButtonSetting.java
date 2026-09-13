package dev.darkvisuals.modules.settings.impl;

import dev.darkvisuals.modules.settings.Setting;

 public class ButtonSetting extends Setting<Boolean> {

    private final Runnable action;

    public ButtonSetting(String name, Runnable action) {
        super(name, false);
        this.action = action;
    }

      
    public void click() {
        if (action != null) action.run();
    }
}
