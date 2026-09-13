package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import net.minecraft.client.resource.language.I18n;

public class Fullbright extends Module {

    public Fullbright() {
        super("Fullbright", Category.Render, I18n.translate("module.fullbright.description"));
    }
}