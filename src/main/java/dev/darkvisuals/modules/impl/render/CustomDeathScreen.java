package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.ui.death.DarkDeathScreen;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;

public class CustomDeathScreen extends Module {

    private static CustomDeathScreen instance;

    private final BooleanSetting animated = new BooleanSetting("Анимация фона", true);

    public CustomDeathScreen() {
        super("CustomDeathScreen", Category.Render, "Кастомный экран смерти с анимацией");
        getSettings().add(animated);
        instance = this;
    }

    public static CustomDeathScreen getInstance() {
        return instance;
    }

    public boolean isAnimated() {
        return animated.getValue();
    }

    public static boolean shouldReplace() {
        CustomDeathScreen m = instance;
        return m != null && m.isToggled();
    }
}
