package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.resource.language.I18n;

public class NoFluid extends Module {

    private final BooleanSetting water = new BooleanSetting("Вода", true);
    private final BooleanSetting lava = new BooleanSetting("Лава", true);

    public NoFluid() {
        super("NoFluid", Category.Render, safeTranslate("module.nofluid.description", "Убирает туман при погружении в воду или лаву"));

        getSettings().add(water);
        getSettings().add(lava);
    }

    public boolean shouldRemoveFog(CameraSubmersionType submersionType) {
        if (!isToggled()) return false;

        if (submersionType == CameraSubmersionType.WATER && water.getValue()) {
            return true;
        }

        if (submersionType == CameraSubmersionType.LAVA && lava.getValue()) {
            return true;
        }

        return false;
    }

    public boolean isWater() {
        return water.getValue();
    }

    public boolean isLava() {
        return lava.getValue();
    }

    private static String safeTranslate(String key, String fallback) {
        try {
            String translated = I18n.translate(key);
            return (translated == null || translated.equals(key)) ? fallback : translated;
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}