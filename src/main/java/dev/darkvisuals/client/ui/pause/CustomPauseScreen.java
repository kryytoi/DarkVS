package dev.darkvisuals.client.ui.pause;

import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.client.ui.cosmetics.CosmeticsScreen;
import dev.darkvisuals.client.ui.mainmenu.MainMenu;
import dev.darkvisuals.client.util.animations.Animation;
import dev.darkvisuals.client.util.animations.Easing;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.impl.render.Cosmetics;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class CustomPauseScreen extends Screen {

    private final Animation entrance = new Animation(350L, 1.0, true, Easing.EASE_OUT_CUBIC);
    private final List<PauseButton> buttons = new ArrayList<>();
    private long openedAt = 0L;

    public CustomPauseScreen() {
        super(Text.of("Pause"));
    }

    @Override
    protected void init() {
        super.init();
        openedAt = System.currentTimeMillis();
        buttons.clear();

        float btnW = 210f;
        float btnH = 24f;
        float gap = 7f;

        float totalH = 5 * btnH + 4 * gap;
        float startY = this.height / 2f - totalH / 2f + 20f;
        float btnX = this.width / 2f - btnW / 2f;

        buttons.add(new PauseButton(btnX, startY, btnW, btnH, "Продолжить игру", () -> {
            if (this.client != null) this.client.setScreen(null);
        }));

        buttons.add(new PauseButton(btnX, startY + (btnH + gap), btnW, btnH, "Настройки", () -> {
            if (this.client != null) this.client.setScreen(new OptionsScreen(this, this.client.options));
        }));

        buttons.add(new PauseButton(btnX, startY + (btnH + gap) * 2f, btnW, btnH, "Косметика", () -> {
            if (this.client != null) {
                Cosmetics c = darkvisuals.getInstance().getModuleManager().getModule(Cosmetics.class);
                if (c != null) this.client.setScreen(new CosmeticsScreen(c));
            }
        }));

        buttons.add(new PauseButton(btnX, startY + (btnH + gap) * 3f, btnW, btnH, "Статистика", () -> {
            if (this.client != null && this.client.player != null) {
                this.client.setScreen(new StatsScreen(this, this.client.player.getStatHandler()));
            }
        }));

        buttons.add(new PauseButton(btnX, startY + (btnH + gap) * 4f, btnW, btnH, "Отключиться", () -> {
            if (this.client != null) {
                boolean singleplayer = this.client.isInSingleplayer();
                if (this.client.world != null) this.client.world.disconnect();
                if (singleplayer) {
                    this.client.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
                } else {
                    this.client.disconnect();
                }
                this.client.setScreen(new MainMenu());
            }
        }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float t = (float) entrance.getValue();
        MatrixStack stack = context.getMatrices();

        Render2D.drawRect(stack, 0, 0, this.width, this.height, new Color(10, 8, 16, (int) (175 * t)));
        Render2D.drawBlurredRect(stack, 0, 0, this.width, this.height, 0f, 16f, new Color(15, 10, 25, (int) (80 * t)));

        float panelW = 260f;
        float panelH = 220f;
        float panelX = this.width / 2f - panelW / 2f;
        float panelY = this.height / 2f - panelH / 2f + 5f;

        Render2D.drawBlurredRect(stack, panelX - 4f, panelY - 4f, panelW + 8f, panelH + 8f, 14f, 16f,
                new Color(153, 0, 255, (int) (35 * t)));
        Render2D.drawRoundedRect(stack, panelX, panelY, panelW, panelH, 12f,
                new Color(20, 16, 28, (int) (220 * t)));
        Render2D.drawBorder(stack, panelX, panelY, panelW, panelH, 12f, 1.2f, 0f,
                new Color(153, 0, 255, (int) (80 * t)));

        float darkSz = 20f;
        String darkText = "DARK";
        float darkW = Fonts.BOLD.getWidth(darkText, darkSz);
        float visualsSz = 10f;
        String visualsText = "V I S U A L S";
        float visualsW = Fonts.BOLD.getWidth(visualsText, visualsSz);

        float titleW = darkW + 6f + visualsW;
        float titleX = this.width / 2f - titleW / 2f;
        float titleY = panelY + 16f;

        Render2D.drawFont(stack, Fonts.BOLD.getFont(darkSz), darkText,
                titleX, titleY, new Color(245, 245, 250, (int) (245 * t)));
        Render2D.drawFont(stack, Fonts.BOLD.getFont(visualsSz), visualsText,
                titleX + darkW + 6f, titleY + (Fonts.BOLD.getHeight(darkSz) - Fonts.BOLD.getHeight(visualsSz)) / 2f,
                new Color(168, 85, 247, (int) (235 * t)));

        String subText = "МЕНЮ ПАУЗЫ";
        float subSz = 6.5f;
        float subW = Fonts.REGULAR.getWidth(subText, subSz);
        Render2D.drawFont(stack, Fonts.REGULAR.getFont(subSz), subText,
                this.width / 2f - subW / 2f, titleY + Fonts.BOLD.getHeight(darkSz) + 3f,
                new Color(160, 160, 180, (int) (180 * t)));

        for (PauseButton btn : buttons) {
            btn.render(stack, mouseX, mouseY, t);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            for (PauseButton b : buttons) {
                if (b.isHovered((int) mx, (int) my)) {
                    b.action.run();
                    return true;
                }
            }
        }
        return false;
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
            if (this.client != null) this.client.setScreen(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    private static class PauseButton {
        final float x, y, w, h;
        final String text;
        final Runnable action;
        final Animation hover = new Animation(160L, 1.0, false, Easing.EASE_OUT_CUBIC);

        PauseButton(float x, float y, float w, float h, String text, Runnable action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.text = text;
            this.action = action;
        }

        boolean isHovered(int mx, int my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }

        void render(MatrixStack stack, int mx, int my, float t) {
            boolean hov = isHovered(mx, my);
            hover.update(hov);
            float hVal = (float) hover.getValue();

            int bgR = (int) (30 + 20 * hVal);
            int bgG = (int) (26 + 15 * hVal);
            int bgB = (int) (42 + 35 * hVal);
            int bgA = (int) ((170 + 60 * hVal) * t);

            Render2D.drawRoundedRect(stack, x, y, w, h, 6f, new Color(bgR, bgG, bgB, bgA));
            Render2D.drawBorder(stack, x, y, w, h, 6f, 1f, 0f,
                    new Color(153, 0, 255, (int) ((60 + 130 * hVal) * t)));

            float sz = 8f;
            float tw = Fonts.MEDIUM.getWidth(text, sz);
            float th = Fonts.MEDIUM.getHeight(sz);
            float tx = x + w / 2f - tw / 2f;
            float ty = y + h / 2f - th / 2f;

            Color textCol = hov ? new Color(255, 255, 255, (int) (255 * t))
                    : new Color(220, 220, 235, (int) (220 * t));
            Render2D.drawFont(stack, Fonts.MEDIUM.getFont(sz), text, tx, ty, textCol);
        }
    }
}
