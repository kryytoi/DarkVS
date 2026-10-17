package dev.darkvisuals.client.ui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.util.Wrapper;
import dev.darkvisuals.client.util.animations.Animation;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.darkvisuals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class MainMenu extends Screen implements Wrapper {

    public static int menuMode = 3;
    public static boolean themePickerOpen = false;

    private static final Identifier BG_MODE_2 = Identifier.of("darkvisuals", "textures/background.png");
    private static final Identifier BG_MODE_3 = Identifier.of("darkvisuals", "textures/background2.png");
    private static final Identifier BG_MODE_4 = Identifier.of("darkvisuals", "textures/background4.png");
    private static final Identifier BG_MODE_5 = Identifier.of("darkvisuals", "textures/background5.png");

    private static final Identifier ICO_SOLO    = Identifier.of("darkvisuals", "textures/mainmenu/solo.png");
    private static final Identifier ICO_MULTI   = Identifier.of("darkvisuals", "textures/mainmenu/multi.png");
    private static final Identifier ICO_OPTIONS = Identifier.of("darkvisuals", "textures/mainmenu/options.png");
    private static final Identifier ICO_EXIT    = Identifier.of("darkvisuals", "textures/mainmenu/exit.png");
    private static final Identifier ICO_PENCIL  = Identifier.of("darkvisuals", "textures/mainmenu/pencil.png");
    private static final Identifier ICO_BRUSH   = Identifier.of("darkvisuals", "textures/mainmenu/brush.png");

    private static final Color BG_BASE = new Color(12, 9, 19);

    private long shownAtMs = 0L;

    private float lastB1X, lastB1Y;
    private float lastB2X, lastB2Y;
    private float lastB3X, lastB3Y;
    private float lastB4X, lastB4Y;
    private float lastBtnW = 160f, lastBtnH = 24f;

    private float lastB5X, lastB5Y;
    private float lastB6X, lastB6Y;
    private float lastSmallSize = 24f;

    private float lastPopupStartX, lastPopupStartY;
    private float lastCardW = 110f, lastCardH = 60f;
    private float lastCardGapX = 12f, lastCardGapY = 12f;

    public MainMenu() {
        super(Text.literal("Main Menu"));
    }

    @Override
    protected void init() {
        super.init();
        shownAtMs = System.currentTimeMillis();
    }

    private Color getRecodeColor(float t, float shimmer) {
        return switch (menuMode) {
            case 1 -> lerpColor(new Color(236, 72, 153), new Color(168, 85, 247), shimmer);
            case 2 -> new Color(168, 85, 247);
            case 3 -> new Color(250, 204, 21);
            case 4 -> new Color(56, 189, 248);
            case 5 -> new Color(249, 115, 22);
            default -> new Color(250, 204, 21);
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.width <= 0 || this.height <= 0) return;

        if (!Fonts.isLoaded() && !Fonts.load()) {
            try {
                Render2D.drawRect(context.getMatrices(), 0, 0, this.width, this.height, BG_BASE);
                if (this.textRenderer != null) {
                    context.drawCenteredTextWithShadow(this.textRenderer, "Dark Visuals: загрузка шрифтов…",
                            this.width / 2, this.height / 2, 0xFFFFFFFF);
                }
            } catch (Throwable ignored) {
            }
            return;
        }

        try {
            renderMenu(context, mouseX, mouseY, delta);
        } catch (Throwable t) {
            darkvisuals.LOGGER.error("[DarkVisuals] Ошибка отрисовки главного меню:", t);
            try {
                Render2D.drawRect(context.getMatrices(), 0, 0, this.width, this.height, BG_BASE);
            } catch (Throwable ignored) {}
        }
    }

    private void renderMenu(DrawContext context, int mouseX, int mouseY, float delta) {
        if (shownAtMs == 0L) shownAtMs = System.currentTimeMillis();
        float t = Math.min(1.0f, Math.max(0.1f, (System.currentTimeMillis() - shownAtMs) / 300.0f));
        MatrixStack stack = context.getMatrices();
        int W = this.width, H = this.height;

        float shimmer = 0.5f + 0.5f * (float) Math.sin((System.currentTimeMillis() - shownAtMs) / 1600.0);
        Color recodeColor = getRecodeColor(t, shimmer);

        if (menuMode == 1) {
            Render2D.drawRect(stack, 0, 0, W, H, BG_BASE);
            Render2D.drawGradientRect(stack, 0, 0, W, H * 0.4f,
                    new Color(0, 0, 0, 140), new Color(0, 0, 0, 0), false);
            Render2D.drawGradientRect(stack, 0, H * 0.6f, W, H * 0.4f,
                    new Color(0, 0, 0, 0), new Color(0, 0, 0, 170), false);
        } else {
            Identifier bgTex = switch (menuMode) {
                case 2 -> BG_MODE_2;
                case 3 -> BG_MODE_3;
                case 4 -> BG_MODE_4;
                case 5 -> BG_MODE_5;
                default -> BG_MODE_3;
            };
            Render2D.drawTexture(stack, 0, 0, W, H, 0f, bgTex, Color.WHITE);
            Render2D.drawRect(stack, 0, 0, W, H, new Color(0, 0, 0, 60));
        }

        float titleX = 26f;
        float titleY = 22f;

        float darkVisSz = 17f;
        String darkVisText = "DARK VISUALS";
        Render2D.drawFont(stack, Fonts.BOLD.getFont(darkVisSz), darkVisText,
                titleX, titleY, withAlpha(new Color(245, 245, 250), (int) (250 * t)));

        float recodeSz = 22f;
        String recodeText = "RECODE";
        float recodeY = titleY + Fonts.BOLD.getHeight(darkVisSz) + 1f;

        stack.push();
        stack.translate(titleX, recodeY, 0f);
        stack.scale(1.08f, 1.0f, 1.0f);
        Render2D.drawFont(stack, Fonts.BOLD.getFont(recodeSz), recodeText,
                0f, 0f, withAlpha(recodeColor, (int) (250 * t)));
        stack.pop();

        float menuX = 26f;
        float menuY = recodeY + Fonts.BOLD.getHeight(recodeSz) + 18f;
        float btnW = 160f;
        float btnH = 24f;
        float btnGap = 7f;

        this.lastBtnW = btnW;
        this.lastBtnH = btnH;

        this.lastB1X = menuX; this.lastB1Y = menuY;
        this.lastB2X = menuX; this.lastB2Y = menuY + btnH + btnGap;
        this.lastB3X = menuX; this.lastB3Y = menuY + (btnH + btnGap) * 2f;
        this.lastB4X = menuX; this.lastB4Y = menuY + (btnH + btnGap) * 3f;

        boolean hov1 = !themePickerOpen && isHovered(mouseX, mouseY, lastB1X, lastB1Y, btnW, btnH);
        boolean hov2 = !themePickerOpen && isHovered(mouseX, mouseY, lastB2X, lastB2Y, btnW, btnH);
        boolean hov3 = !themePickerOpen && isHovered(mouseX, mouseY, lastB3X, lastB3Y, btnW, btnH);
        boolean hov4 = !themePickerOpen && isHovered(mouseX, mouseY, lastB4X, lastB4Y, btnW, btnH);

        drawMenuButton(stack, lastB1X, lastB1Y, btnW, btnH, "Одиночная игра", ICO_SOLO, hov1, t);
        drawMenuButton(stack, lastB2X, lastB2Y, btnW, btnH, "Сетевая игра", ICO_MULTI, hov2, t);
        drawMenuButton(stack, lastB3X, lastB3Y, btnW, btnH, "Настройки", ICO_OPTIONS, hov3, t);
        drawMenuButton(stack, lastB4X, lastB4Y, btnW, btnH, "Выход", ICO_EXIT, hov4, t);

        float smallSize = 24f;
        float smallY = menuY + (btnH + btnGap) * 4f;
        this.lastSmallSize = smallSize;
        this.lastB5X = menuX;
        this.lastB5Y = smallY;
        this.lastB6X = menuX + smallSize + 7f;
        this.lastB6Y = smallY;

        boolean hov5 = !themePickerOpen && isHovered(mouseX, mouseY, lastB5X, lastB5Y, smallSize, smallSize);
        boolean hov6 = !themePickerOpen && isHovered(mouseX, mouseY, lastB6X, lastB6Y, smallSize, smallSize);

        drawSmallButton(stack, lastB5X, lastB5Y, smallSize, ICO_PENCIL, hov5, t);
        drawSmallButton(stack, lastB6X, lastB6Y, smallSize, ICO_BRUSH, hov6 || themePickerOpen, t);

        if (hov5 && !themePickerOpen) {
            String user = mc.getSession() != null && mc.getSession().getUsername() != null ? mc.getSession().getUsername() : "Player";
            String tip = "Аккаунты (" + user + ")";
            float tipSz = 7f;
            float tw = Fonts.REGULAR.getWidth(tip, tipSz);
            float tx = lastB5X + smallSize + 8f;
            float ty = lastB5Y + (smallSize - Fonts.REGULAR.getHeight(tipSz)) / 2f;
            Render2D.drawRoundedRect(stack, tx - 4f, ty - 2f, tw + 8f, Fonts.REGULAR.getHeight(tipSz) + 4f, 4f, new Color(15, 15, 20, 210));
            Render2D.drawFont(stack, Fonts.REGULAR.getFont(tipSz), tip, tx, ty, Color.WHITE);
        }

        if (themePickerOpen) {
            renderThemePicker(stack, W, H, mouseX, mouseY);
        }
    }

    private void renderThemePicker(MatrixStack stack, int W, int H, int mx, int my) {
        Render2D.drawRect(stack, 0, 0, W, H, new Color(0, 0, 0, 110));

        float cardW = 110f;
        float cardH = 60f;
        float gapX = 12f;
        float gapY = 12f;

        this.lastCardW = cardW;
        this.lastCardH = cardH;
        this.lastCardGapX = gapX;
        this.lastCardGapY = gapY;

        float totalW = 3 * cardW + 2 * gapX;
        float totalH = 2 * cardH + gapY;

        float startX = (W - totalW) / 2f + 30f;
        float startY = (H - totalH) / 2f;

        this.lastPopupStartX = startX;
        this.lastPopupStartY = startY;

        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 3; c++) {
                float cx = startX + c * (cardW + gapX);
                float cy = startY + r * (cardH + gapY);

                int modeNum = 1;
                if (r == 0 && c == 0) modeNum = 1;
                else if (r == 0 && c == 1) modeNum = 2;
                else if (r == 0 && c == 2) modeNum = 3;
                else if (r == 1 && c == 0) modeNum = -1;
                else if (r == 1 && c == 1) modeNum = 4;
                else if (r == 1 && c == 2) modeNum = 5;

                boolean isSelected = (modeNum == menuMode);
                boolean hovered = isHovered(mx, my, cx, cy, cardW, cardH);

                drawThemeCard(stack, cx, cy, cardW, cardH, modeNum, isSelected, hovered);
            }
        }
    }

    private void drawThemeCard(MatrixStack stack, float x, float y, float w, float h,
                               int modeNum, boolean isSelected, boolean hovered) {
        Color cardBg = hovered ? new Color(45, 45, 60, 170) : new Color(20, 20, 30, 140);
        Color cardBorder = isSelected ? new Color(74, 222, 128) : (hovered ? new Color(255, 255, 255, 80) : new Color(255, 255, 255, 30));
        float borderThick = isSelected ? 2f : 1f;

        Render2D.drawRoundedRect(stack, x, y, w, h, 8f, cardBg);
        Render2D.drawBorder(stack, x, y, w, h, 8f, borderThick, 0f, cardBorder);

        if (modeNum == 1) {
            String defText = "Default";
            float defSz = 14f;
            float tw = Fonts.BOLD.getWidth(defText, defSz);
            float th = Fonts.BOLD.getHeight(defSz);
            Render2D.drawFont(stack, Fonts.BOLD.getFont(defSz), defText,
                    x + (w - tw) / 2f, y + (h - th) / 2f + 1f, Color.WHITE);
        } else if (modeNum == -1) {
            float ax = x + w / 2f;
            float ay = y + h / 2f;
            Render2D.drawLine(stack, ax - 16f, ay, ax + 16f, ay, 2.5f, Color.WHITE);
            Render2D.drawLine(stack, ax - 16f, ay, ax - 8f, ay - 8f, 2.5f, Color.WHITE);
            Render2D.drawLine(stack, ax - 16f, ay, ax - 8f, ay + 8f, 2.5f, Color.WHITE);
        } else {
            Identifier tex = switch (modeNum) {
                case 2 -> BG_MODE_2;
                case 3 -> BG_MODE_3;
                case 4 -> BG_MODE_4;
                case 5 -> BG_MODE_5;
                default -> BG_MODE_3;
            };
            float tw = 84f;
            float th = 44f;
            float tx = x + (w - tw) / 2f;
            float ty = y + (h - th) / 2f;
            Render2D.drawTexture(stack, tx, ty, tw, th, 6f, tex, Color.WHITE);
            Render2D.drawRoundedRect(stack, tx, ty, tw, th, 6f, new Color(0, 0, 0, 50));

            String numStr = String.valueOf(modeNum);
            float numSz = 18f;
            float nw = Fonts.BOLD.getWidth(numStr, numSz);
            float nh = Fonts.BOLD.getHeight(numSz);
            Render2D.drawFont(stack, Fonts.BOLD.getFont(numSz), numStr,
                    tx + (tw - nw) / 2f, ty + (th - nh) / 2f + 1f, Color.WHITE);
        }
    }

    private void drawMenuButton(MatrixStack stack, float x, float y, float w, float h,
                                String text, Identifier icon, boolean hovered, float alpha) {
        Color bg = hovered ? new Color(45, 45, 60, (int) (180 * alpha)) : new Color(20, 20, 30, (int) (130 * alpha));
        Color border = hovered ? new Color(255, 255, 255, (int) (95 * alpha)) : new Color(255, 255, 255, (int) (35 * alpha));
        Render2D.drawRoundedRect(stack, x, y, w, h, 6f, bg);
        Render2D.drawBorder(stack, x, y, w, h, 6f, 1f, 0f, border);

        if (icon != null) {
            float iconSz = 12f;
            Render2D.drawTexture(stack, x + 7f, y + (h - iconSz) / 2f, iconSz, iconSz, 0f, icon,
                    withAlpha(Color.WHITE, (int) (245 * alpha)));
            Render2D.drawRect(stack, x + 25f, y + 4f, 1f, h - 8f,
                    withAlpha(new Color(255, 255, 255), (int) (45 * alpha)));
            float textX = x + 33f;
            float textY = y + (h - Fonts.BOLD.getHeight(7.5f)) / 2f + 1f;
            Render2D.drawFont(stack, Fonts.BOLD.getFont(7.5f), text, textX, textY,
                    withAlpha(Color.WHITE, (int) (245 * alpha)));
        }
    }

    private void drawSmallButton(MatrixStack stack, float x, float y, float size,
                                 Identifier icon, boolean hovered, float alpha) {
        Color bg = hovered ? new Color(45, 45, 60, (int) (180 * alpha)) : new Color(20, 20, 30, (int) (130 * alpha));
        Color border = hovered ? new Color(255, 255, 255, (int) (95 * alpha)) : new Color(255, 255, 255, (int) (35 * alpha));
        Render2D.drawRoundedRect(stack, x, y, size, size, 6f, bg);
        Render2D.drawBorder(stack, x, y, size, size, 6f, 1f, 0f, border);

        if (icon != null) {
            float iconSz = 12f;
            Render2D.drawTexture(stack, x + (size - iconSz) / 2f, y + (size - iconSz) / 2f, iconSz, iconSz, 0f, icon,
                    withAlpha(Color.WHITE, (int) (245 * alpha)));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return false;

        try {
            if (themePickerOpen) {
                float startX = lastPopupStartX;
                float startY = lastPopupStartY;
                float cardW = lastCardW;
                float cardH = lastCardH;
                float gapX = lastCardGapX;
                float gapY = lastCardGapY;

                for (int r = 0; r < 2; r++) {
                    for (int c = 0; c < 3; c++) {
                        float cx = startX + c * (cardW + gapX);
                        float cy = startY + r * (cardH + gapY);
                        if (isHovered(mx, my, cx, cy, cardW, cardH)) {
                            if (r == 0 && c == 0) {
                                menuMode = 1;
                            } else if (r == 0 && c == 1) {
                                menuMode = 2;
                            } else if (r == 0 && c == 2) {
                                menuMode = 3;
                            } else if (r == 1 && c == 0) {
                                themePickerOpen = false;
                            } else if (r == 1 && c == 1) {
                                menuMode = 4;
                            } else if (r == 1 && c == 2) {
                                menuMode = 5;
                            }
                            return true;
                        }
                    }
                }
                themePickerOpen = false;
                return true;
            }

            if (isHovered(mx, my, lastB1X, lastB1Y, lastBtnW, lastBtnH)) {
                mc.setScreen(new SelectWorldScreen(this));
                return true;
            }
            if (isHovered(mx, my, lastB2X, lastB2Y, lastBtnW, lastBtnH)) {
                mc.setScreen(new MultiplayerScreen(this));
                return true;
            }
            if (isHovered(mx, my, lastB3X, lastB3Y, lastBtnW, lastBtnH)) {
                mc.setScreen(new OptionsScreen(this, mc.options));
                return true;
            }
            if (isHovered(mx, my, lastB4X, lastB4Y, lastBtnW, lastBtnH)) {
                mc.scheduleStop();
                new Thread(() -> {
                    try {
                        Thread.sleep(1500L);
                    } catch (InterruptedException ignored) {
                    }
                    System.exit(0);
                }, "MainMenu-ForceExit").start();
                return true;
            }

            if (isHovered(mx, my, lastB5X, lastB5Y, lastSmallSize, lastSmallSize)) {
                mc.setScreen(new AltManagerScreen(this));
                return true;
            }

            if (isHovered(mx, my, lastB6X, lastB6Y, lastSmallSize, lastSmallSize)) {
                themePickerOpen = true;
                return true;
            }

            return super.mouseClicked(mx, my, btn);
        } catch (Throwable t) {
            darkvisuals.LOGGER.error("[DarkVisuals] Ошибка при клике в главном меню:", t);
            return false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F11) {
            if (this.client != null && this.client.getWindow() != null) {
                this.client.getWindow().toggleFullscreen();
                this.client.options.getFullscreen().setValue(this.client.getWindow().isFullscreen());
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (themePickerOpen) {
                themePickerOpen = false;
                return true;
            }
            return true;
        }
        try {
            return super.keyPressed(keyCode, scanCode, modifiers);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        try {
            return super.keyReleased(keyCode, scanCode, modifiers);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        try {
            return super.charTyped(chr, modifiers);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isHovered(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static Color withAlpha(Color c, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static Color lerpColor(Color a, Color b, float t) {
        int r = (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl, 255);
    }

    public static class AnimatedButton {
        private final float x, y, width, height;
        private final String message;
        private final PressAction action;
        private final Animation hoverAnimation;

        public AnimatedButton(float x, float y, float width, float height, String message, PressAction action) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.message = message;
            this.action = action;
            this.hoverAnimation = new Animation(200L, 1.0, false, Easing.EASE_OUT_CUBIC);
        }

        public void resetAnimations() {
            hoverAnimation.reset();
        }

        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta, float alpha) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            hoverAnimation.update(hovered);

            float hoverProgress = (float) hoverAnimation.getValue();
            MatrixStack stack = drawContext.getMatrices();

            Render2D.drawRoundedRect(stack, x, y, width, height, 8f,
                    new Color(50, 50, 60, clamp255((int) (alpha * 150 + hoverProgress * 100))));

            Color base  = new Color(220, 220, 240);
            Color hover = new Color(255, 255, 255);
            int r = (int) (base.getRed()   + (hover.getRed()   - base.getRed())   * hoverProgress);
            int g = (int) (base.getGreen() + (hover.getGreen() - base.getGreen()) * hoverProgress);
            int b = (int) (base.getBlue()  + (hover.getBlue()  - base.getBlue())  * hoverProgress);

            Color textColor = new Color(r, g, b, clamp255((int) (alpha * 255)));
            float sz = 7f;
            float textX = x + width  / 2f - Fonts.REGULAR.getWidth(message, sz) / 2f;
            float textY = y + height / 2f - sz / 2f;

            Render2D.drawFont(stack, Fonts.REGULAR.getFont(sz), message, textX, textY, textColor);
        }

        public boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        public void onPress() {
            action.onPress(this);
        }

        private static int clamp255(int v) {
            return Math.max(0, Math.min(255, v));
        }
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(AnimatedButton button);
    }
}
