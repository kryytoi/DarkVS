package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.ui.hud.HudStyle;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.animations.infinity.InfinityAnimation;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Module;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class KeybindsHUD extends HudElement implements ThemeManager.ThemeChangeListener {

    private static final Identifier ICON_KEYBINDS = Identifier.of("darkvisuals", "textures/hud/keybinds.png");

    private final ThemeManager themeManager;
    private final InfinityAnimation heightAnim = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation widthAnim = new InfinityAnimation(Easing.OUT_QUAD);
    private Color textColor = Color.WHITE;

    public KeybindsHUD() {
        super("KeybindsHUD");
        this.themeManager = ThemeManager.getInstance();
        themeManager.addThemeChangeListener(this);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.textColor = Color.WHITE;
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        if (!HudStyle.isMinimalistic()) {
            setBounds(getX(), getY(), 0, 0);
            return;
        }

        Perf.tryBeginFrame();
        try (var __ = Perf.scopeCpu("KeybindsHUD.onRender2D")) {
            MatrixStack matrices = e.getContext().getMatrices();
            boolean chatOpen = mc.currentScreen instanceof ChatScreen;

            List<Module> binds = new ArrayList<>();
            for (Module module : darkvisuals.getInstance().getModuleManager().getModules()) {
                if (module instanceof HudElement) continue;
                if (module.getBind() == null || module.getBind().isEmpty()) continue;
                if (!module.isToggled()) continue;
                binds.add(module);
            }

            boolean preview = chatOpen && binds.isEmpty();
            List<String[]> rows = new ArrayList<>();
            float pad = 5f;
            float headerH = 16f;
            float rowH = 16f;
            float font = 7.5f;
            float titleFont = 8f;
            float gap = 3f;

            float targetW = 90f;
            if (preview) {
                rows.add(new String[]{"Dildo", "R"});
                targetW = Math.max(targetW, pad * 2 + Fonts.MEDIUM.getWidth("Dildo", font) + 24f);
            } else {
                for (Module m : binds) {
                    String name = I18n.translate(m.getName());
                    String key = m.getBind().toString();
                    rows.add(new String[]{name, key});
                    targetW = Math.max(targetW, pad * 2 + Fonts.MEDIUM.getWidth(name, font) + Fonts.MEDIUM.getWidth(key, font) + 20f);
                }
            }

            float targetH = headerH + gap + Math.max(1, rows.size()) * (rowH + 2f) + pad;
            if (rows.isEmpty() && !preview) {
                targetH = headerH + pad;
            }

            widthAnim.animate(targetW, 180);
            heightAnim.animate(targetH, 180);
            float w = widthAnim.getValue();
            float h = heightAnim.getValue();
            float x = getX();
            float y = getY();
            float fade = toggledAnimation.getValue();

            setBounds(x, y, w, h);
            Render2D.drawHudBackground(matrices, x, y, w, h, 8f, fade);

            String title = I18n.hasTranslation("hud.keybinds.title")
                    ? I18n.translate("hud.keybinds.title") : "Binds";
            float titleY = y + headerH / 2f - Fonts.MEDIUM.getHeight(titleFont) / 2f;
            float slashW = Fonts.MEDIUM.getWidth(" / ", titleFont);
            float iconW = titleFont + 3f;
            float iconGap = 4f;
            float blockW = Fonts.MEDIUM.getWidth(title, titleFont) + slashW + iconGap + iconW;
            float titleX = x + (w - blockW) / 2f;

            Render2D.drawHudText(matrices, Fonts.MEDIUM.getFont(titleFont), title, titleX, titleY, withAlpha(textColor, fade));
            Render2D.drawHudText(matrices, Fonts.MEDIUM.getFont(titleFont), " / ",
                    titleX + Fonts.MEDIUM.getWidth(title, titleFont), titleY, withAlpha(new Color(180, 180, 180), fade));
            Render2D.drawTexture(matrices,
                    titleX + Fonts.MEDIUM.getWidth(title, titleFont) + slashW + iconGap,
                    y + headerH / 2f - iconW / 2f, iconW, iconW, 0f, ICON_KEYBINDS, withAlpha(Color.WHITE, fade));

            float curY = y + headerH + gap;
            for (String[] row : rows) {
                float innerX = x + pad;
                float innerW = w - pad * 2f;
                Render2D.drawRoundedRect(matrices, innerX, curY, innerW, rowH, 5f, new Color(0, 0, 0, (int) (90 * fade)));

                float textY = curY + (rowH - Fonts.MEDIUM.getHeight(font)) / 2f;
                Render2D.drawHudText(matrices, Fonts.MEDIUM.getFont(font), row[0], innerX + 5f, textY, withAlpha(textColor, fade));
                float keyW = Fonts.MEDIUM.getWidth(row[1], font);
                Render2D.drawHudText(matrices, Fonts.MEDIUM.getFont(font), row[1],
                        innerX + innerW - keyW - 5f, textY, withAlpha(textColor, fade));
                curY += rowH + 2f;
            }

            super.onRender2D(e);
        }
    }

    private static Color withAlpha(Color c, float fade) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (Math.min(255, c.getAlpha() * fade)));
    }
}
