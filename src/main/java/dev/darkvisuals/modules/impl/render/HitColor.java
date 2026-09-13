package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.NumberSetting;

public class HitColor extends Module {

    public final NumberSetting alpha = new NumberSetting("setting.alpha", 0.8f, 0.1f, 1.0f, 0.05f);

    public HitColor() {
        super("HitColor", Category.Render, "module.hitcolor.description");
        getSettings().add(alpha);
    }
}
