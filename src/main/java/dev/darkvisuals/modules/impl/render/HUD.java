package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventKey;
import dev.darkvisuals.client.ui.hud.HudEditorScreen;
import dev.darkvisuals.client.ui.hud.HudStyle;
import dev.darkvisuals.modules.settings.api.Bind;
import dev.darkvisuals.modules.settings.impl.BindSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;

 
public class HUD extends Module {

    private static HUD instance;

     
    private final EnumSetting<HudStyle> style = new EnumSetting<>("Style", HudStyle.GLOWING);

    private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    private final BooleanSetting targetHud = new BooleanSetting("TargetHud", true);
    private final BooleanSetting potions = new BooleanSetting("Potions", true);
    private final BooleanSetting armorHud = new BooleanSetting("ArmorHUD", true);
    private final BooleanSetting hotbar = new BooleanSetting("Hotbar", true);
    private final BooleanSetting music = new BooleanSetting("Music", true);
    private final BooleanSetting betterNear = new BooleanSetting("BetterNearHUD", true);
    private final BooleanSetting keyboard = new BooleanSetting("KeyboardHUD", true);
    private final BooleanSetting keybinds = new BooleanSetting("KeybindsHUD", true);
    private final BooleanSetting inventory = new BooleanSetting("Inventory", true);
    private final BindSetting editorBind = new BindSetting("HUD Editor bind", new Bind(GLFW.GLFW_KEY_J, false));

    public HUD() {
        super("HUD", Category.Render, "Отображение интерфейса клиента");
        instance = this;
         
        getSettings().add(editorBind);
        setToggled(true);
    }

    public BindSetting getEditorBind() {
        return editorBind;
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (mc.currentScreen != null) return;
        Bind bind = editorBind.getValue();
        if (bind == null || bind.isEmpty() || bind.isMouse()) return;
        if (e.getKey() != bind.getKey()) return;
        if (e.getAction() != GLFW.GLFW_PRESS) return;
        mc.setScreen(new HudEditorScreen());
    }

    public static HUD getInstance() {
        return instance;
    }

      
    public HudStyle getHudStyle() {
        return style.getValue();
    }

 
    public boolean isElementVisible(String name) {
        if (!isToggled()) return false;

        BooleanSetting setting = getElementSetting(name);
         
        return setting == null || setting.getValue();
    }

 
    public boolean hasElementSetting(String name) {
        return getElementSetting(name) != null;
    }

    private BooleanSetting getElementSetting(String name) {
        for (var setting : getSettings()) {
            if (setting instanceof BooleanSetting bool && bool.getName().equalsIgnoreCase(name)) {
                return bool;
            }
        }
        return null;
    }
}
