package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.ChatUtils;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.ui.clickgui.ClickGui;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.api.Nameable;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.resource.language.I18n;

public class UI extends Module {


    public UI() {
        super("UI", Category.Render, I18n.translate("module.ui.description"));
        setBind(new Bind(GLFW.GLFW_KEY_RIGHT_SHIFT, false));

    }


    @EventHandler
    public void onTick(EventTick e) {
        if (!(mc.currentScreen instanceof ClickGui) && !(mc.currentScreen instanceof ClickGui)) {
            setToggled(false);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

         
        if (mc.player == null || mc.world == null) {
            ChatUtils.sendMessage(I18n.translate("darkvisuals.ui.onlyInWorld"));
            setToggled(false);
            return;
        }

        mc.setScreen(darkvisuals.getInstance().getClickGui());


    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.currentScreen instanceof ClickGui) {
            ((ClickGui) mc.currentScreen).close();
        }
    }

    public enum UIMode implements Nameable {
        Minimalist("Minimalist"),
        LiquidGlass("Liquid Glass");

        private final String displayName;

        UIMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getName() {
            return displayName;
        }
    }

    private final EnumSetting<UIMode> uiMode = new EnumSetting<>("UI Mode", UIMode.Minimalist);

    public UIMode getUiMode() {
        return uiMode.getValue();
    }
}