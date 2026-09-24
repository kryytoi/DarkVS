package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.ui.hud.HudStyle;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.animations.Animation;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.client.util.renderer.fonts.Font;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.modules.impl.utility.Optimization;

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

    // плавные значения FPS/пинга — цифры «докручиваются», а не прыгают
    private float displayFps = 60f;
    private float displayPing = 0f;

    // собственная анимация наведения (hover в базовом классе приватный)
    private final Animation wmHover = new Animation(300, 1f, false, Easing.SMOOTH_STEP);

    // Cached HUD text: FPS/ping strings are rebuilt only when the rounded value changes.
    private int cachedFpsShown = -1;
    private int cachedPingShown = -1;
    private String cachedFpsText = "";
    private String cachedPingText = "";
    private long lastCounterUpdateMs = 0L;

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

    /** Цвет FPS: зелёный -> жёлтый -> красный по величине. */
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
            // докручиваем плавные значения к реальным
            if (!Optimization.isHudSlowCounters() || System.currentTimeMillis() - lastCounterUpdateMs >= 250L) {
                lastCounterUpdateMs = System.currentTimeMillis();
                displayFps += (mc.getCurrentFps() - displayFps) * 0.15f;
                displayPing += (getPing() - displayPing) * 0.12f;
            }

            if (HudStyle.isMinimalistic()) {
                renderMinimalistic(e);
            } else {
                renderGlowing(e);
            }
            super.onRender2D(e);
        }
    }

    /** Трансформация появления: выезд сверху + лёгкое увеличение при наведении. */
    private void pushEntrance(MatrixStack matrices, float fade) {
        wmHover.update(mouseX() >= getX() && mouseX() <= getX() + getWidth()
                && mouseY() >= getY() && mouseY() <= getY() + getHeight());

        matrices.push();
        float slide = (1f - fade) * 7f;
        float scale = 1f + 0.03f * wmHover.getValue();
        matrices.translate(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0f);
        matrices.scale(scale, scale, 1f);
        matrices.translate(-(getX() + getWidth() / 2f), -(getY() + getHeight() / 2f), 0f);
        matrices.translate(0f, -slide, 0f);
    }


    private void renderMinimalistic(EventRender2D e) {
        MatrixStack matrices = e.getContext().getMatrices();
        Font font = Fonts.MEDIUM;

        String title = "Dark Visuals";
        int fpsShown = Math.max(0, Math.round(displayFps));
        if (fpsShown != cachedFpsShown) { cachedFpsShown = fpsShown; cachedFpsText = fpsShown + " FPS"; }
        String fps = cachedFpsText;
        int pingShown = Math.max(0, Math.round(displayPing));
        if (pingShown != cachedPingShown) { cachedPingShown = pingShown; cachedPingText = pingShown + " ms"; }
        String ping = cachedPingText;

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
        Color liveAccent = themeManager.getCurrentTheme().getAccentColor();
        Color purple = new Color(0xA0, 0x00, 0xFF, (int) (255 * fade));

        if (Optimization.isHudStaticAnimations()) {
            matrices.push();
        } else {
            pushEntrance(matrices, fade);
        }

        if (Optimization.isHudFastBackground()) {
            Render2D.drawRoundedRect(matrices, x, y, totalWidth, totalHeight, r,
                    new Color(13, 13, 13, (int) (205 * fade)));
        } else {
            Render2D.drawHudBackground(matrices, x, y, totalWidth, totalHeight, r, fade);
        }

        float textY = y + (totalHeight - font.getHeight(fontSize)) / 2f;
        float cursor = x + padX;

        // логотип мягко «дышит»
        float breathe = Optimization.isHudStaticAnimations() ? 1f : 1f + 0.04f * (float) Math.sin(System.currentTimeMillis() / 900.0);
        float logoSize = iconSize * breathe;
        Render2D.drawTexture(matrices, cursor + (iconSize - logoSize) / 2f, y + (totalHeight - logoSize) / 2f,
                logoSize, logoSize, 0f, LOGO_TEXTURE, purple);
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

        String title = "DarkVisuals";
        String username = getUsername();
        int fpsShown = Math.max(0, Math.round(displayFps));
        if (fpsShown != cachedFpsShown) { cachedFpsShown = fpsShown; cachedFpsText = fpsShown + " fps"; }
        String fps = cachedFpsText;
        int pingShown = Math.max(0, Math.round(displayPing));
        if (pingShown != cachedPingShown) { cachedPingShown = pingShown; cachedPingText = pingShown + " ms"; }
        String ping = cachedPingText;

        float fontSize = 7f;
        float dotGap = 8f;
        float dotSize = 2.2f;
        float paddingX = 9f;
        float iconSize = 16f;
        float iconGap = 8f;

        float titleW = font.getWidth(title, fontSize);
        float userW  = font.getWidth(username, fontSize);
        float fpsW   = font.getWidth(fps, fontSize);
        float pingW  = font.getWidth(ping, fontSize);

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

        if (Optimization.isHudStaticAnimations()) {
            matrices.push();
        } else {
            pushEntrance(matrices, fade);
        }

        if (Optimization.isHudFastBackground()) {
            Render2D.drawRoundedRect(matrices, x, y, totalWidth, totalHeight, r,
                    new Color(13, 13, 13, (int) (205 * fade)));
        } else {
            Render2D.drawHudBackground(matrices, x, y, totalWidth, totalHeight, r, fade);
        }

        float iconX = x + paddingX;
        float iconY = y + (totalHeight - iconSize) / 2f;
        // логотип мягко «дышит»
        float breathe = Optimization.isHudStaticAnimations() ? 1f : 1f + 0.05f * (float) Math.sin(System.currentTimeMillis() / 850.0);
        float logoSize = iconSize * breathe;
        Render2D.drawTexture(matrices, iconX + (iconSize - logoSize) / 2f, iconY + (iconSize - logoSize) / 2f,
                logoSize, logoSize, 0f, LOGO_TEXTURE, Color.WHITE);

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
        // точки-разделители пульсируют по очереди
        float pulse = Optimization.isHudStaticAnimations() ? 0.65f : 0.65f + 0.35f * (float) Math.sin(now / 500.0 + cursorX * 0.15);
        float dot = dotSize * (0.8f + 0.35f * pulse);
        float dotX = cursorX + dotGap + (dotSize - dot) / 2f;
        float dotY = y + (h - dot) / 2f;
        Color c = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (255 * fade * (0.55f + 0.45f * pulse)));
        Render2D.drawRoundedRect(matrices, dotX, dotY, dot, dot, dot / 2f, c);
        return dotX + dot + dotGap;
    }
}
