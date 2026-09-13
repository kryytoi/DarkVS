package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventKey;
import dev.darkvisuals.client.ui.emotes.EmoteWheelScreen;
import dev.darkvisuals.emotes.Emote;
import dev.darkvisuals.emotes.EmoteRegistry;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.impl.BindSetting;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ListSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Emotes extends Module {

      
    private final BindSetting wheelBind = new BindSetting("setting.emoteWheelBind", new Bind(GLFW.GLFW_KEY_B, false));

      
    private final Map<String, BooleanSetting> emoteToggles = new LinkedHashMap<>();
    private final ListSetting enabledEmotes;

    public Emotes() {
        super("Emotes", Category.Render, I18n.translate("module.emotes.description"));

        List<BooleanSetting> toggles = new ArrayList<>();
        for (Emote emote : EmoteRegistry.all()) {
            BooleanSetting toggle = new BooleanSetting(emote.translationKey(), true);
            emoteToggles.put(emote.id(), toggle);
            toggles.add(toggle);
        }
         
        enabledEmotes = new ListSetting("setting.emoteList", toggles.toArray(new BooleanSetting[0]));

        getSettings().add(wheelBind);
        getSettings().add(enabledEmotes);
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (fullNullCheck()) return;
        if (mc.currentScreen != null) return;

        Bind bind = wheelBind.getValue();
        if (bind == null || bind.isEmpty() || bind.isMouse()) return;
        if (e.getKey() != bind.getKey()) return;

        if (e.getAction() == GLFW.GLFW_PRESS) {
            mc.setScreen(new EmoteWheelScreen(this));
        }
    }

    public BindSetting getWheelBind() {
        return wheelBind;
    }

    public boolean isEmoteEnabled(String emoteId) {
        BooleanSetting toggle = emoteToggles.get(emoteId);
        return toggle != null && toggle.getValue();
    }

      
    public List<Emote> getWheelEmotes() {
        List<Emote> result = new ArrayList<>();
        for (Emote emote : EmoteRegistry.all()) {
            if (isEmoteEnabled(emote.id())) result.add(emote);
        }
        return result;
    }
}