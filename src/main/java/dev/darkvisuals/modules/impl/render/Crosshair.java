package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.crosshaireditor.CrosshairEditorScreen;
import dev.darkvisuals.client.util.crosshair.CrosshairPattern;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.api.Nameable;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import dev.darkvisuals.modules.settings.impl.EnumSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.HitResult;

import java.awt.*;

public class Crosshair extends Module implements ThemeManager.ThemeChangeListener {

    private static Crosshair instance;  

     
    public enum Mode implements Nameable {
        CLASSIC("Классический"),
        CUSTOM("Свой");

        private final String displayName;
        Mode(String displayName) { this.displayName = displayName; }
        @Override public String getName() { return displayName; }
    }

    private final EnumSetting<Mode> mode = new EnumSetting<>("Режим", Mode.CLASSIC);

     
    private final NumberSetting thickness = new NumberSetting("Толщина", 1f, 0.5f, 3f, 0.1f,
            () -> mode.getValue() == Mode.CLASSIC);
    private final NumberSetting length = new NumberSetting("Длина", 3f, 1f, 8f, 0.5f,
            () -> mode.getValue() == Mode.CLASSIC);
    private final NumberSetting gap = new NumberSetting("Разрыв", 2f, 0f, 5f, 0.5f,
            () -> mode.getValue() == Mode.CLASSIC);
    private final BooleanSetting dynamicGap = new BooleanSetting("Динамический разрыв", false,
            () -> mode.getValue() == Mode.CLASSIC);

     
    private final ButtonSetting configureButton = new ButtonSetting("Настроить", this::openEditor);
    private final NumberSetting pixelScale = new NumberSetting("Масштаб", 1f, 0.5f, 3f, 0.5f,
            () -> mode.getValue() == Mode.CUSTOM);
     
    private final StringSetting patternData = new StringSetting("PatternData", "", () -> false, false);

     
    private final BooleanSetting useEntityColor = new BooleanSetting("Цвет при наведении", false);

     
    private final ThemeManager themeManager;
    private Color currentColor;
    private final Color entityColor = new Color(255, 0, 0);

     
    private CrosshairPattern cachedPattern;
    private String cachedPatternSource;

    public Crosshair() {
        super("Crosshair", Category.Render, "Кастомный прицел");
        getSettings().add(mode);
        getSettings().add(thickness);
        getSettings().add(length);
        getSettings().add(gap);
        getSettings().add(dynamicGap);
        getSettings().add(configureButton);
        getSettings().add(pixelScale);
        getSettings().add(useEntityColor);
        getSettings().add(patternData);
        instance = this;
        themeManager = ThemeManager.getInstance();
        currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);
    }

    public static Crosshair getInstance() {
        return instance;
    }

     
     
     

      
    private void openEditor() {
        mode.setValue(Mode.CUSTOM);  
        mc.setScreen(new CrosshairEditorScreen(this));
    }

      
    public CrosshairPattern getPattern() {
        String src = patternData.getValue();
        if (cachedPattern == null || !src.equals(cachedPatternSource)) {
            cachedPattern = CrosshairPattern.deserialize(src);
            cachedPatternSource = src;
        }
        return cachedPattern;
    }

      
    public void applyPattern(CrosshairPattern pattern) {
        cachedPattern = pattern;
        cachedPatternSource = pattern.serialize();
        patternData.setValue(cachedPatternSource);  
        if (!isToggled()) setToggled(true);         
    }

     
     
     

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;
        if (mc.currentScreen instanceof CrosshairEditorScreen) return;  

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        float x = sw * 0.5f;
        float y = sh * 0.5f;

        Color color = currentColor;
        if (useEntityColor.getValue() && mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            color = entityColor;
        }

        var matrices = e.getContext().getMatrices();

        if (mode.getValue() == Mode.CUSTOM) {
            CrosshairPattern p = getPattern();
            if (p != null && !p.isEmpty()) {
                renderCustom(matrices, x, y, p, color);
                return;
            }
             
        }

        renderClassic(matrices, x, y, color);
    }

    private void renderClassic(net.minecraft.client.util.math.MatrixStack matrices,
                               float x, float y, Color color) {
        float currentGap = gap.getValue();
        if (dynamicGap.getValue()) {
            float cooldown = 1f - mc.player.getAttackCooldownProgress(0);
            currentGap = Math.min(currentGap + 8f * cooldown, 10f);
        }
        float w = thickness.getValue();
        float l = length.getValue();

        Render2D.drawRect(matrices, x - w / 2, y - currentGap - l, w, l, color);
        Render2D.drawRect(matrices, x - w / 2, y + currentGap, w, l, color);
        Render2D.drawRect(matrices, x - currentGap - l, y - w / 2, l, w, color);
        Render2D.drawRect(matrices, x + currentGap, y - w / 2, l, w, color);
    }

    private void renderCustom(net.minecraft.client.util.math.MatrixStack matrices,
                              float cx, float cy, CrosshairPattern p, Color hoverTint) {
        float scale = pixelScale.getValue();
        int size = p.getSize();
        float half = size * scale / 2f;
        boolean tint = useEntityColor.getValue() && hoverTint == entityColor;

        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                int argb = p.get(px, py);
                int alpha = argb >>> 24;
                if (alpha == 0) continue;
                Color c = tint
                        ? new Color(entityColor.getRed(), entityColor.getGreen(), entityColor.getBlue(), alpha)
                        : new Color(argb, true);
                Render2D.drawRect(matrices,
                        cx - half + px * scale, cy - half + py * scale,
                        scale, scale, c);
            }
        }
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor = theme.getBackgroundColor();
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }
}