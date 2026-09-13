package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.animations.infinity.InfinityAnimation;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Font;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;

 
public class KeyboardHUD extends HudElement implements ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;
    private Color accentColor;

     
    private static final float KEY = 20f;         
    private static final float GAP = 3f;          
    private static final float RADIUS = 4f;       
    private static final float ROW_W = KEY * 3 + GAP * 2;  
    private static final float MOUSE_H = 24f;     
    private static final float SPACE_H = 8f;      

     
    private final InfinityAnimation wAnim = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation aAnim = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation sAnim = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation dAnim = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation lmbAnim = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation rmbAnim = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation spaceAnim = new InfinityAnimation(Easing.OUT_QUAD);

     
    private final Deque<Long> lmbClicks = new ArrayDeque<>();
    private final Deque<Long> rmbClicks = new ArrayDeque<>();
    private boolean lmbWasDown, rmbWasDown;

    public KeyboardHUD() {
        super("KeyboardHUD");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        this.accentColor = theme.getAccentColor();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

     
    private boolean pressed(net.minecraft.client.option.KeyBinding key) {
        try {
            return key != null && key.isPressed();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private int cps(Deque<Long> clicks, boolean down, boolean[] wasDown) {
        long now = System.currentTimeMillis();
         
        if (down && !wasDown[0]) clicks.addLast(now);
        wasDown[0] = down;
         
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) clicks.pollFirst();
        return clicks.size();
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        Perf.tryBeginFrame();
        try (var __ = Perf.scopeCpu("KeyboardHUD.onRender2D")) {

            MatrixStack matrices = e.getContext().getMatrices();
            boolean chatOpen = mc.currentScreen instanceof ChatScreen;

             
            boolean wDown = !chatOpen && pressed(mc.options.forwardKey);
            boolean aDown = !chatOpen && pressed(mc.options.leftKey);
            boolean sDown = !chatOpen && pressed(mc.options.backKey);
            boolean dDown = !chatOpen && pressed(mc.options.rightKey);
            boolean spaceDown = !chatOpen && pressed(mc.options.jumpKey);
            boolean lmbDown = !chatOpen && pressed(mc.options.attackKey);
            boolean rmbDown = !chatOpen && pressed(mc.options.useKey);

             
            boolean[] lw = {lmbWasDown};
            boolean[] rw = {rmbWasDown};
            int lmbCps = cps(lmbClicks, lmbDown, lw);
            int rmbCps = cps(rmbClicks, rmbDown, rw);
            lmbWasDown = lw[0];
            rmbWasDown = rw[0];

             
            long dur = 120;
            wAnim.animate(wDown ? 1f : 0f, dur);
            aAnim.animate(aDown ? 1f : 0f, dur);
            sAnim.animate(sDown ? 1f : 0f, dur);
            dAnim.animate(dDown ? 1f : 0f, dur);
            spaceAnim.animate(spaceDown ? 1f : 0f, dur);
            lmbAnim.animate(lmbDown ? 1f : 0f, dur);
            rmbAnim.animate(rmbDown ? 1f : 0f, dur);

            float x = getX();
            float y = getY();
            float fade = toggledAnimation.getValue();

             
             
            float wX = x + (ROW_W - KEY) / 2f;
            float row1Y = y;
             
            float row2Y = row1Y + KEY + GAP;
             
            float row3Y = row2Y + KEY + GAP;
            float mouseW = (ROW_W - GAP) / 2f;
             
            float row4Y = row3Y + MOUSE_H + GAP;

            float totalW = ROW_W;
            float totalH = (row4Y + SPACE_H) - y;

             
            drawKey(matrices, wX, row1Y, KEY, KEY, "W", wAnim.getValue(), fade, 7f, null);

            drawKey(matrices, x, row2Y, KEY, KEY, "A", aAnim.getValue(), fade, 7f, null);
            drawKey(matrices, x + KEY + GAP, row2Y, KEY, KEY, "S", sAnim.getValue(), fade, 7f, null);
            drawKey(matrices, x + (KEY + GAP) * 2, row2Y, KEY, KEY, "D", dAnim.getValue(), fade, 7f, null);

            drawKey(matrices, x, row3Y, mouseW, MOUSE_H, "LMB", lmbAnim.getValue(), fade, 7f, lmbCps + " CPS");
            drawKey(matrices, x + mouseW + GAP, row3Y, mouseW, MOUSE_H, "RMB", rmbAnim.getValue(), fade, 7f, rmbCps + " CPS");

            drawKey(matrices, x, row4Y, ROW_W, SPACE_H, "", spaceAnim.getValue(), fade, 6f, null);

            setBounds(x, y, totalW, totalH);
            super.onRender2D(e);
        }
    }

 
    private void drawKey(MatrixStack matrices, float kx, float ky, float kw, float kh,
                         String label, float press, float fade, float fontSize, String subLabel) {

         
        Render2D.drawHudBackground(matrices, kx, ky, kw, kh, RADIUS, fade);

         
        if (press > 0.001f) {
            Color liveAccent = themeManager.getCurrentTheme().getAccentColor();
            int a = Math.max(0, Math.min(255, (int) (255 * press * fade)));
            Render2D.drawRoundedRect(matrices, kx, ky, kw, kh, RADIUS,
                    new Color(liveAccent.getRed(), liveAccent.getGreen(), liveAccent.getBlue(), a));
        }

         
        if (label.isEmpty()) {
            float lineW = kw * 0.35f;
            float lineH = 1.6f;
            Color txt = textColorFor(press, fade);
            Render2D.drawRoundedRect(matrices, kx + (kw - lineW) / 2f, ky + (kh - lineH) / 2f,
                    lineW, lineH, lineH / 2f, txt);
            return;
        }

        Font font = Fonts.BOLD;
        Color txt = textColorFor(press, fade);

        if (subLabel != null) {
             
            Font small = Fonts.MEDIUM;
            float subSize = 5f;
            float labelH = font.getHeight(fontSize);
            float subH = small.getHeight(subSize);
            float blockH = labelH + 1.5f + subH;
            float topY = ky + (kh - blockH) / 2f;

            float labelW = font.getWidth(label, fontSize);
            drawText(matrices, font, fontSize, label, kx + (kw - labelW) / 2f, topY, txt, fade);

            float subW = small.getWidth(subLabel, subSize);
            Color subCol = new Color(txt.getRed(), txt.getGreen(), txt.getBlue(),
                    (int) (txt.getAlpha() * 0.8f));
            drawText(matrices, small, subSize, subLabel, kx + (kw - subW) / 2f,
                    topY + labelH + 1.5f, subCol, fade);
        } else {
            float labelW = font.getWidth(label, fontSize);
            float labelH = font.getHeight(fontSize);
            drawText(matrices, font, fontSize, label,
                    kx + (kw - labelW) / 2f, ky + (kh - labelH) / 2f, txt, fade);
        }
    }

      
    private Color textColorFor(float press, float fade) {
        int alpha = Math.max(0, Math.min(255, (int) (255 * fade)));
        if (press <= 0.5f) return new Color(255, 255, 255, alpha);

        Color accent = themeManager.getCurrentTheme().getAccentColor();
        int brightness = (int) (0.299 * accent.getRed() + 0.587 * accent.getGreen() + 0.114 * accent.getBlue());
        return brightness > 150 ? new Color(20, 20, 20, alpha) : new Color(255, 255, 255, alpha);
    }

    private void drawText(MatrixStack matrices, Font font, float size, String text,
                          float tx, float ty, Color color, float fade) {
        Render2D.drawHudText(matrices, font.getFont(size), text, tx, ty, color);
    }
}
