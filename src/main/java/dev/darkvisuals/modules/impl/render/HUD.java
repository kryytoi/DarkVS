package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.ui.hud.HudStyle;
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

    public HUD() {
        super("HUD", Category.Render, "Отображение интерфейса клиента");
        instance = this;
         
        setToggled(true);
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
