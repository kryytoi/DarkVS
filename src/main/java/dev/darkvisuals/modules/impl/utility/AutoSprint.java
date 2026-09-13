package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.client.events.impl.EventTick;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;

public class AutoSprint extends Module {

    public AutoSprint() {
        super("AutoSprint", Category.Utility, I18n.translate("module.autosprint.description"));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            client.options.sprintKey.setPressed(true);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            client.options.sprintKey.setPressed(false);
        }
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!isToggled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;
         
        client.options.sprintKey.setPressed(true);
    }
}