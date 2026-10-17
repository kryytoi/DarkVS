package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.ui.games.GamesScreen;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;

public class Games extends Module {

    private final ButtonSetting playButton = new ButtonSetting("Играть", () -> {
        if (mc != null) {
            mc.setScreen(new GamesScreen());
        }
    });

    public Games() {
        super("Games", Category.Utility, "Мини-игры: Змейка и Крестики-Нолики против бота");
        getSettings().add(playButton);
    }
}
