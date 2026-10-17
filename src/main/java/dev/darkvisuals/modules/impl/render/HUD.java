package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.ui.hud.HudEditorScreen;
import dev.darkvisuals.client.ui.hud.HudStyle;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import dev.darkvisuals.client.ui.hud.WatermarkStyle;

public class HUD extends Module {

    private static HUD instance;

    private final EnumSetting<HudStyle> style = new EnumSetting<>("Style", HudStyle.GLOWING);
    private final EnumSetting<WatermarkStyle> watermarkStyle = new EnumSetting<>("Стиль ватермарки", WatermarkStyle.DEFAULT);

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

    private final StringSetting watermarkName = new StringSetting("Название", "Dark Visuals", false);
    private final BooleanSetting customLogo = new BooleanSetting("Свой логотип", false);
    private final StringSetting logoFile = new StringSetting("Файл логотипа", "logo.png", () -> customLogo.getValue(), false);
    private final ButtonSetting openEditor = new ButtonSetting("Редактор HUD",
            () -> mc.send(() -> mc.setScreen(new HudEditorScreen())));

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

    public BooleanSetting getElementSetting(String name) {
        for (var setting : getSettings()) {
            if (setting instanceof BooleanSetting bool && bool.getName().equalsIgnoreCase(name)) {
                return bool;
            }
        }
        return null;
    }

    public void setElementVisible(String name, boolean visible) {
        BooleanSetting setting = getElementSetting(name);
        if (setting != null) {
            setting.setValue(visible);
        }
    }

    public String getWatermarkName() {
        String v = watermarkName.getValue();
        return v == null || v.isBlank() ? "Dark Visuals" : v;
    }

    public boolean isCustomLogo() {
        return customLogo.getValue();
    }

    public String getLogoFile() {
        return logoFile.getValue();
    }

    public void setWatermarkName(String s) {
        watermarkName.setValue(s);
    }

    public void setCustomLogo(boolean b) {
        customLogo.setValue(b);
    }

    public void setLogoFile(String f) {
        logoFile.setValue(f);
    }

    public WatermarkStyle getWatermarkStyle() {
        return watermarkStyle.getValue();
    }
}
