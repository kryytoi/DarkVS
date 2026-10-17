package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.ui.hud.CustomLogo;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.ui.hud.HudStyle;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.animations.Animation;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.client.util.renderer.fonts.Font;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.modules.impl.render.HUD;
import dev.darkvisuals.client.ui.hud.WatermarkStyle;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import java.awt.Color;

public class Watermark extends HudElement implements ThemeManager.ThemeChangeListener {

    private static final Identifier LOGO_TEXTURE = Identifier.of("darkvisuals", "hud/logo.png");
    private static final Identifier ICON_FPS = Identifier.of("darkvisuals", "textures/hud/fps.png");
    private static final Identifier ICON_PING = Identifier.of("darkvisuals", "textures/hud/ping.png");

    private final ThemeManager themeManager;
    private Color textColor;
    private Color accentColor;

    private float totalWidth, totalHeight;

    private float displayFps = 60f;
    private float displayPing = 0f;

    private final Animation wmHover = new Animation(300, 1f, false, Easing.SMOOTH_STEP);
    private double discoAngle = 0.0;
    private long lastDiscoTime = System.currentTimeMillis();

    public Watermark() {
        super("Watermark");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        this.textColor = Color.WHITE;
        this.accentColor = theme.getAccentColor();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    private String wmTitle() {
        HUD hud = HUD.getInstance();
        return hud != null ? hud.getWatermarkName() : "Dark Visuals";
    }

    private String getUsername() {
        try {
            if (mc.player != null && mc.player.getGameProfile() != null) {
                String name = mc.player.getGameProfile().getName();
                if (name != null && !name.isEmpty()) return name;
            }
        } catch (Throwable ignored) {}
        return "Player";
    }

    private int getPing() {
        try {
            if (mc.player != null && mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) return entry.getLatency();
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private Color fpsColor(float fps) {
        if (fps >= 120f) return new Color(110, 235, 130);
        if (fps >= 60f) return new Color(235, 205, 100);
        return new Color(240, 110, 110);
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        Perf.tryBeginFrame();
        try (var __ = Perf.scopeCpu("Watermark.onRender2D")) {
            displayFps += (mc.getCurrentFps() - displayFps) * 0.15f;
            displayPing += (getPing() - displayPing) * 0.12f;

            HUD hud = HUD.getInstance();
            WatermarkStyle wmStyle = hud != null ? hud.getWatermarkStyle() : WatermarkStyle.DEFAULT;
            if (wmStyle == WatermarkStyle.DISCO) {
                renderDisco(e);
            } else if (HudStyle.isMinimalistic()) {
                renderMinimalistic(e);
            } else {
                renderGlowing(e);
            }
            super.onRender2D(e);
        }
    }

    private void pushEntrance(MatrixStack matrices, float fade) {
        wmHover.update(mouseX() >= getX() && mouseX() <= getX() + getWidth()
                && mouseY() >= getY() && mouseY() <= getY() + getHeight());

        matrices.push();
        float slide = (1f - fade) * 7f;
        float scale = (1f + 0.03f * wmHover.getValue()) * getScale();
        matrices.translate(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0f);
        matrices.scale(scale, scale, 1f);
        matrices.translate(-(getX() + getWidth() / 2f), -(getY() + getHeight() / 2f), 0f);
        matrices.translate(0f, -slide, 0f);
    }

    private void drawSweep(EventRender2D e, float x, float y, float w, float h, float fade) {
        if (fade < 0.05f) return;
        long now = System.currentTimeMillis();
        float sweepT = (now % 2800L) / 2800f;
        float segW = Math.max(24f, w * 0.3f);
        float segX = x + sweepT * (w + segW) - segW;

        var context = e.getContext();
        context.enableScissor((int) x, (int) (y + h - 3f), (int) (x + w), (int) (y + h + 1f));
        Color accent = themeManager.getCurrentTheme().getAccentColor();
        Render2D.drawRoundedRect(context.getMatrices(), segX, y + h - 1.6f, segW, 1.3f, 0.65f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (190 * fade)));
        context.disableScissor();
    }

    private void renderMinimalistic(EventRender2D e) {
        MatrixStack matrices = e.getContext().getMatrices();
        Font font = Fonts.MEDIUM;

        String title = wmTitle();
        int fpsShown = Math.max(0, Math.round(displayFps));
        String fps = fpsShown + " FPS";
        int pingShown = Math.max(0, Math.round(displayPing));
        String ping = pingShown + " ms";

        float fontSize = 7f;
        float iconSize = 15f;
        float padX = 8f;
        float padY = 5f;
        float gap = 5f;
        float sepGap = 7f;

        float titleW = font.getWidth(title, fontSize);
        float fpsW = font.getWidth(fps, fontSize);
        float pingW = font.getWidth(ping, fontSize);
        float fpsIconW = iconSize;
        float pingIconW = iconSize;

        totalHeight = padY * 2f + Math.max(font.getHeight(fontSize), iconSize);
        totalWidth = padX
                + iconSize + gap + titleW + sepGap
                + 1f + sepGap
                + fpsIconW + gap + fpsW + sepGap
                + 1f + sepGap
                + pingIconW + gap + pingW
                + padX;

        setBounds(getX(), getY(), totalWidth, totalHeight);

        float x = getX(), y = getY();
        float r = totalHeight / 2f;
        float fade = toggledAnimation.getValue();
        Color purple = new Color(0xA0, 0x00, 0xFF, (int) (255 * fade));

        pushEntrance(matrices, fade);

        Render2D.drawHudBackground(matrices, x, y, totalWidth, totalHeight, r, fade);
        drawSweep(e, x, y, totalWidth, totalHeight, fade);

        float textY = y + (totalHeight - font.getHeight(fontSize)) / 2f;
        float cursor = x + padX;

        Identifier logo = CustomLogo.get(LOGO_TEXTURE);
        Color logoTint = logo == LOGO_TEXTURE ? purple : withAlpha(Color.WHITE, fade);
        float breathe = 1f + 0.04f * (float) Math.sin(System.currentTimeMillis() / 900.0);
        float logoSize = iconSize * breathe;
        Render2D.drawTexture(matrices, cursor + (iconSize - logoSize) / 2f, y + (totalHeight - logoSize) / 2f,
                logoSize, logoSize, 0f, logo, logoTint);
        cursor += iconSize + gap;
        Render2D.drawHudText(matrices, font.getFont(fontSize), title, cursor, textY, withAlpha(textColor, fade));
        cursor += titleW + sepGap;

        cursor = drawVertSep(matrices, cursor, y, totalHeight, fade);
        cursor += sepGap;

        Render2D.drawTexture(matrices, cursor, y + (totalHeight - iconSize) / 2f, iconSize, iconSize, 0f, ICON_FPS, withAlpha(Color.WHITE, fade));
        cursor += fpsIconW + gap;
        Render2D.drawHudText(matrices, font.getFont(fontSize), fps, cursor, textY,
                withAlpha(fpsColor(displayFps), fade));
        cursor += fpsW + sepGap;

        cursor = drawVertSep(matrices, cursor, y, totalHeight, fade);
        cursor += sepGap;

        Render2D.drawTexture(matrices, cursor, y + (totalHeight - iconSize) / 2f, iconSize, iconSize, 0f, ICON_PING, withAlpha(Color.WHITE, fade));
        cursor += pingIconW + gap;
        Render2D.drawHudText(matrices, font.getFont(fontSize), ping, cursor, textY, withAlpha(textColor, fade));

        matrices.pop();
    }

    private float drawVertSep(MatrixStack matrices, float x, float y, float h, float fade) {
        float sepH = h * 0.45f;
        Render2D.drawRoundedRect(matrices, x, y + (h - sepH) / 2f, 1f, sepH, 0.5f,
                new Color(120, 120, 120, (int) (140 * fade)));
        return x + 1f;
    }

    private static Color withAlpha(Color c, float fade) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * fade));
    }

    private void renderGlowing(EventRender2D e) {
        var matrices = e.getContext().getMatrices();
        Font font = Fonts.MEDIUM;

        String title = wmTitle();
        String username = getUsername();
        int fpsShown = Math.max(0, Math.round(displayFps));
        String fps = fpsShown + " fps";
        int pingShown = Math.max(0, Math.round(displayPing));
        String ping = pingShown + " ms";

        float fontSize = 7f;
        float dotGap = 8f;
        float dotSize = 2.2f;
        float paddingX = 9f;
        float iconSize = 16f;
        float iconGap = 8f;

        float titleW = font.getWidth(title, fontSize);
        float userW = font.getWidth(username, fontSize);
        float fpsW = font.getWidth(fps, fontSize);
        float pingW = font.getWidth(ping, fontSize);

        float sepW = dotGap + dotSize + dotGap;

        totalHeight = 20f;
        totalWidth = paddingX + iconSize + iconGap
                + titleW + sepW
                + userW + sepW
                + fpsW + sepW
                + pingW + paddingX;

        setBounds(getX(), getY(), totalWidth, totalHeight);

        float x = getX(), y = getY();
        float r = totalHeight / 2f;
        Color liveAccent = themeManager.getCurrentTheme().getAccentColor();
        float fade = toggledAnimation.getValue();

        pushEntrance(matrices, fade);

        Render2D.drawHudBackground(matrices, x, y, totalWidth, totalHeight, r, fade);
        drawSweep(e, x, y, totalWidth, totalHeight, fade);

        float iconX = x + paddingX;
        float iconY = y + (totalHeight - iconSize) / 2f;
        Identifier logo = CustomLogo.get(LOGO_TEXTURE);
        float breathe = 1f + 0.05f * (float) Math.sin(System.currentTimeMillis() / 850.0);
        float logoSize = iconSize * breathe;
        Render2D.drawTexture(matrices, iconX + (iconSize - logoSize) / 2f, iconY + (iconSize - logoSize) / 2f,
                logoSize, logoSize, 0f, logo, Color.WHITE);

        float textY = y + (totalHeight - font.getHeight(fontSize)) / 2f;
        float cursorX = iconX + iconSize + iconGap;

        drawGlassText(matrices, font, fontSize, title, cursorX, textY, fade);
        cursorX += titleW;
        cursorX = drawSep(matrices, cursorX, y, totalHeight, dotGap, dotSize, liveAccent, fade);

        drawGlassText(matrices, font, fontSize, username, cursorX, textY, fade);
        cursorX += userW;
        cursorX = drawSep(matrices, cursorX, y, totalHeight, dotGap, dotSize, liveAccent, fade);

        drawGlassText(matrices, font, fontSize, fps, cursorX, textY,
                fade, withAlpha(fpsColor(displayFps), fade));
        cursorX += fpsW;
        cursorX = drawSep(matrices, cursorX, y, totalHeight, dotGap, dotSize, liveAccent, fade);

        drawGlassText(matrices, font, fontSize, ping, cursorX, textY, fade);

        matrices.pop();
    }

    private void drawGlassText(net.minecraft.client.util.math.MatrixStack matrices,
                                Font font, float fontSize, String text, float x, float y, float fade) {
        drawGlassText(matrices, font, fontSize, text, x, y, fade, withAlpha(textColor, fade));
    }

    private void drawGlassText(net.minecraft.client.util.math.MatrixStack matrices,
                                Font font, float fontSize, String text, float x, float y, float fade, Color color) {
        Render2D.drawHudText(matrices, font.getFont(fontSize), text, x, y, color);
    }

    private float drawSep(net.minecraft.client.util.math.MatrixStack matrices,
                           float cursorX, float y, float h, float dotGap, float dotSize, Color accent, float fade) {
        long now = System.currentTimeMillis();
        float pulse = 0.65f + 0.35f * (float) Math.sin(now / 500.0 + cursorX * 0.15);
        float dot = dotSize * (0.8f + 0.35f * pulse);
        float dotX = cursorX + dotGap + (dotSize - dot) / 2f;
        float dotY = y + (h - dot) / 2f;
        Color c = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (255 * fade * (0.55f + 0.45f * pulse)));
        Render2D.drawRoundedRect(matrices, dotX, dotY, dot, dot, dot / 2f, c);
        return dotX + dot + dotGap;
    }

    private void renderDisco(EventRender2D e) {
        var matrices = e.getContext().getMatrices();
        Font font = Fonts.MEDIUM;

        long now = System.currentTimeMillis();
        long dt = now - lastDiscoTime;
        lastDiscoTime = now;
        discoAngle += dt * 0.0025;

        float hue = (float)((now % 3000L) / 3000.0);
        Color rainbowBg = Color.getHSBColor(hue, 0.7f, 0.15f);
        Color rainbowAccent = Color.getHSBColor(hue, 0.9f, 1.0f);

        String title = wmTitle();
        float fontSize = 7f;
        float dotRadius = 2.5f;
        int len = title.length();

        float totalWidth = Math.max(80f, len * 12f + 20f);
        float totalHeight = 22f;

        setBounds(getX(), getY(), totalWidth, totalHeight);

        float x = getX(), y = getY();
        float fade = toggledAnimation.getValue();
        if (fade <= 0f) return;

        pushEntrance(matrices, fade);

        Color bgColor = new Color(rainbowBg.getRed(), rainbowBg.getGreen(), rainbowBg.getBlue(), (int)(220 * fade));
        float r = totalHeight / 2f;
        Render2D.drawRoundedRect(matrices, x, y, totalWidth, totalHeight, r, bgColor);

        Color borderColor = new Color(rainbowAccent.getRed(), rainbowAccent.getGreen(), rainbowAccent.getBlue(), (int)(180 * fade));
        Render2D.drawRoundedRect(matrices, x, y, totalWidth, 1f, 0f, borderColor);
        Render2D.drawRoundedRect(matrices, x, y + totalHeight - 1f, totalWidth, 1f, 0f, borderColor);

        float centerX = x + totalWidth / 2f;
        float centerY = y + totalHeight / 2f;
        float orbitW = (totalWidth / 2f - 10f) * 0.85f;
        float orbitH = (totalHeight / 2f - 2f) * 0.75f;

        for (int i = 0; i < len; i++) {
            float charAngle = (float)(discoAngle + (i * 2.0 * Math.PI / len));
            float cx = centerX + orbitW * (float)Math.cos(charAngle);
            float cy = centerY + orbitH * (float)Math.sin(charAngle);

            float charHue = (hue + i * (1.0f / Math.max(1, len))) % 1.0f;
            Color dotColor = Color.getHSBColor(charHue, 0.9f, 1.0f);
            Color dotColorFaded = new Color(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), (int)(255 * fade));

            float pulse = 1f + 0.25f * (float)Math.sin(discoAngle * 3 + i);
            float dr = dotRadius * pulse;
            Render2D.drawRoundedRect(matrices, cx - dr, cy - dr, dr * 2, dr * 2, dr, dotColorFaded);

            String ch = String.valueOf(title.charAt(i));
            float chW = font.getWidth(ch, 5f);
            float chH = font.getHeight(5f);
            Color textColorDisco = new Color(255, 255, 255, (int)(220 * fade));
            Render2D.drawHudText(matrices, font.getFont(5f), ch, cx - chW / 2f, cy - chH / 2f, textColorDisco);
        }

        for (int i = 0; i < 6; i++) {
            float sparkAngle = (float)(discoAngle * 0.7 + i * Math.PI / 3.0);
            float sr = orbitW * 0.45f;
            float sx = centerX + sr * (float)Math.cos(sparkAngle);
            float sy = centerY + (orbitH * 0.5f) * (float)Math.sin(sparkAngle * 1.5f);
            if (sx < x + 2 || sx > x + totalWidth - 2) continue;
            if (sy < y + 2 || sy > y + totalHeight - 2) continue;
            float sparkHue = (hue + i * (1f / 6f)) % 1.0f;
            Color sparkColor = Color.getHSBColor(sparkHue, 1f, 1f);
            Color sparkFaded = new Color(sparkColor.getRed(), sparkColor.getGreen(), sparkColor.getBlue(), (int)(100 * fade));
            float sd = 1.0f;
            Render2D.drawRoundedRect(matrices, sx - sd, sy - sd, sd * 2, sd * 2, sd, sparkFaded);
        }

        matrices.pop();
    }
}
