package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.WaypointManager;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Font;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.client.util.world.WorldUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class WaypointOverlay implements Wrapper, ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;
    private Color textColor;
    private Color accent;

    public WaypointOverlay() {
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        this.textColor = theme.getTextColor();
        this.accent = theme.getAccentColor();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = e.getContext().getMatrices();
        int winW = mc.getWindow().getScaledWidth();
        int winH = mc.getWindow().getScaledHeight();

        Font nameFont = Fonts.MEDIUM;
        Font metaFont = Fonts.REGULAR;
        float nameSize = 7.5f;
        float metaSize = 7f;

         
        float padX = 7f, padY = 4f;
        float iconSize = 4.5f;    
        float iconGap = 5f;
        float lineGap = 2f;

        for (WaypointManager.Waypoint w : WaypointManager.list()) {
            Vec3d screen = WorldUtils.getPosition(w.pos.add(0, 1.8, 0));
             
            if (!(screen.z > 0)) continue;

            String label = w.name;
            int meters = (int) Math.floor(mc.player.getPos().distanceTo(w.pos));
            String meta = meters + "m";

             
            float nameW = nameFont.getWidth(label, nameSize);
            float metaW = metaFont.getWidth(meta, metaSize);
            float topRowW = iconSize + iconGap + nameW;       
            float width = Math.max(topRowW, metaW) + padX * 2f;
            float height = padY + nameFont.getHeight(nameSize) + lineGap
                    + metaFont.getHeight(metaSize) + padY;
            float r = 5f;  

            float x = (float) screen.x - width / 2f;
            float y = (float) screen.y - height - 8f;

             
            x = Math.max(5f, Math.min(x, winW - width - 5f));
            y = Math.max(5f, Math.min(y, winH - height - 5f));

             
            drawLiquidGlass(matrices, x, y, width, height, r);

             
            float dotX = Math.max(3f, Math.min((float) screen.x, winW - 3f)) - 2f;
            float dotY = Math.max(3f, Math.min((float) screen.y, winH - 3f)) - 2f;
            Render2D.drawRoundedRect(matrices, dotX, dotY, 4f, 4f, 2f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220));
            Render2D.drawBorder(matrices, dotX, dotY, 4f, 4f, 2f, 0f, 1f,
                    new Color(255, 255, 255, 90));

             
            float rowY = y + padY;

             
            float iconY = rowY + (nameFont.getHeight(nameSize) - iconSize) / 2f;
            Render2D.drawRoundedRect(matrices, x + padX, iconY, iconSize, iconSize, iconSize / 2f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 235));
            Render2D.drawBorder(matrices, x + padX, iconY, iconSize, iconSize, iconSize / 2f, 0f, 1f,
                    new Color(255, 255, 255, 80));

             
            drawGlassText(matrices, nameFont, nameSize, label,
                    x + padX + iconSize + iconGap, rowY,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 255));

             
            drawGlassText(matrices, metaFont, metaSize, meta,
                    x + padX, rowY + nameFont.getHeight(nameSize) + lineGap,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 210));
        }
    }

       
    private void drawLiquidGlass(MatrixStack matrices, float x, float y, float w, float h, float r) {
        Render2D.drawHudBackground(matrices, x, y, w, h, r, 1f);
    }

      
    private void drawGlassText(MatrixStack matrices, Font fontFamily, float fontSize,
                               String text, float x, float y, Color color) {
        Render2D.drawHudText(matrices, fontFamily.getFont(fontSize), text, x, y, color);
    }

    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
    }
}