package dev.darkvisuals.client.ui.clickgui;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class ClickGuiState {
    private final Map<Category, Float> scrollMap = new EnumMap<>(Category.class);

    public float getScroll(Category category) {
        return scrollMap.getOrDefault(category, 0f);
    }

    public void setScroll(Category category, float scroll) {
        scrollMap.put(category, scroll);
    }

    public List<Module> getModules(Category category) {
        if (category == Category.Theme) return Collections.emptyList();  
        return darkvisuals.getInstance().getModuleManager().getModules(category);
    }
}