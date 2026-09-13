package dev.darkvisuals.client.util.macro;

import dev.darkvisuals.modules.settings.api.Bind;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class Macro {
    private String name, command;
    private Bind bind;
}