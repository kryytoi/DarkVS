package dev.darkvisuals.modules.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public enum Category {
    Theme("I"),
    Render("H"),
    Utility("I"),
    Friends("F"),
    Markers("M"),    
    Hud("LOL"),
    Config("C");

    private final String icon;
}
