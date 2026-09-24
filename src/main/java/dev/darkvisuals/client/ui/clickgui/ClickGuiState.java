package dev.darkvisuals.client.ui.clickgui;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

public class ClickGuiState {
    private final Map<Category, Float> scrollMap = new EnumMap<>(Category.class);
    // отдельный скролл для колонки настроек в стиле New — иначе список
    // модулей и настройки листаются одновременно
    private final Map<Category, Float> settingsScrollMap = new EnumMap<>(Category.class);

    public float getScroll(Category category) {
        return scrollMap.getOrDefault(category, 0f);
    }

    public void setScroll(Category category, float scroll) {
        scrollMap.put(category, scroll);
    }

    public float getSettingsScroll(Category category) {
        return settingsScrollMap.getOrDefault(category, 0f);
    }

    public void setSettingsScroll(Category category, float scroll) {
        settingsScrollMap.put(category, scroll);
    }

    public List<Module> getModules(Category category) {
    if (category == Category.Theme) return Collections.emptyList();
    List<Module> out = new ArrayList<>();
    for (Module m : darkvisuals.getInstance().getModuleManager().getModules(category)) {
        if (dev.darkvisuals.liteapi.LiteApi.isBlocked(m.getName())) continue; // <- вставить
        out.add(m);
    }
    return out;
    }
}