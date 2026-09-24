package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.ChatUtils;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.ui.clickgui.ClickGui;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.api.Nameable;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.resource.language.I18n;

public class UI extends Module {


    public UI() {
        super("UI", Category.Render, I18n.translate("module.ui.description"));
        setBind(new Bind(GLFW.GLFW_KEY_RIGHT_SHIFT, false));

        getSettings().add(guiSound);
        getSettings().add(darkaApiKey);
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

    /** Стиль клик-гуй: Old — нынешний, New — новый, непохожий на старый. */
    public enum GuiStyle implements Nameable {
        Old("Old"),
        New("New");

        private final String displayName;

        GuiStyle(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getName() {
            return displayName;
        }
    }

    private final EnumSetting<UIMode> uiMode = new EnumSetting<>("UI Mode", UIMode.Minimalist);
    private final EnumSetting<GuiStyle> guiStyle = new EnumSetting<>("GUI Style", GuiStyle.Old);

    // ── Настройки ClickGUI ────────────────────────────────────────────
    /** Звук при включении/выключении функции в клик-гуй. */
    private final BooleanSetting guiSound = new BooleanSetting("GUI Sound", true);

    // ── Настройки DARKA-0.1 ───────────────────────────────────────────
    /** API-ключ Hugging Face для нейросети DARKA-0.1. */
    private final StringSetting darkaApiKey = new StringSetting("DARKA API Key", "", false);

    public UIMode getUiMode() {
        return uiMode.getValue();
    }

    public GuiStyle getGuiStyle() {
        return guiStyle.getValue();
    }

    public boolean isGuiSound() {
        return guiSound.getValue();
    }

    public void setGuiSoundInverted() {
        guiSound.setValue(!guiSound.getValue());
    }

    public String getDarkaApiKey() {
        String v = darkaApiKey.getValue();
        return v == null ? "" : v.trim();
    }

    public void setDarkaApiKey(String v) {
        darkaApiKey.setValue(v == null ? "" : v);
    }
}