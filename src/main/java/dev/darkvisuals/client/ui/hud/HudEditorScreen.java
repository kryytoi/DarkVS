package dev.darkvisuals.client.ui.hud;

import dev.darkvisuals.client.managers.HudManager;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * HudEditorScreen - полноэкранный редактор HUD.
 *
 * Открывается по бинду из модуля HUD (по умолчанию J). Пока редактор открыт,
 * все HUD-элементы можно свободно перетаскивать мышью (используется штатный
 * механизм перетаскивания HudElement, для которого редактор считается
 * edit-контекстом наравне с чатом). ПКМ по элементу открывает его настройки,
 * ПКМ по пустому месту - общий список видимости элементов.
 *
 * ESC - выход. Кнопка "Сбросить" раскладывает элементы каскадом.
 */
public class HudEditorScreen extends Screen {

    private static boolean open = false;

    private boolean resetHovered = false;
    private boolean closeHovered = false;

    public HudEditorScreen() {
        super(Text.of("darkvisuals-hud-editor"));
    }

    /** true, пока открыт редактор - HudElement/HudManager считают это edit-контекстом. */
    public static boolean isOpen() {
        return open;
    }

    @Override
    protected void init() {
        super.init();
        open = true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        open = false;
    }

    @Override
    public void close() {
        open = false;
        if (this.client != null) this.client.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack stack = context.getMatrices();
        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();

        // Затемнение мира за редактором
        Render2D.drawRoundedRect(stack, 0, 0, this.width, this.height, 0f, new Color(5, 5, 8, 150));

        // Сетка
        int step = 40;
        Color grid = new Color(255, 255, 255, 18);
        for (int x = step; x < this.width; x += step) {
            Render2D.drawRoundedRect(stack, x, 0, 1f, this.height, 0f, grid);
        }
        for (int y = step; y < this.height; y += step) {
            Render2D.drawRoundedRect(stack, 0, y, this.width, 1f, 0f, grid);
        }
        // Центральные оси
        Render2D.drawRoundedRect(stack, this.width / 2f - 0.5f, 0, 1f, this.height, 0f, new Color(255, 255, 255, 45));
        Render2D.drawRoundedRect(stack, 0, this.height / 2f - 0.5f, this.width, 1f, 0f, new Color(255, 255, 255, 45));

        // Заголовок
        String title = "HUD Editor";
        float ts = 10f;
        float tw = Fonts.SEMIBOLD.getWidth(title, ts);
        Render2D.drawFont(stack, Fonts.SEMIBOLD.getFont(ts), title, this.width / 2f - tw / 2f, 10f,
                new Color(255, 255, 255, 235));

        String hint = "ЛКМ - двигать | ПКМ по элементу - настройки | ПКМ по фону - список | ESC - выход";
        float hs = 7f;
        float hw = Fonts.MEDIUM.getWidth(hint, hs);
        Render2D.drawFont(stack, Fonts.MEDIUM.getFont(hs), hint, this.width / 2f - hw / 2f, 26f,
                new Color(255, 255, 255, 140));

        // Кнопки: Сбросить (слева внизу), Закрыть (справа внизу)
        float btnW = 90f, btnH = 22f, pad = 12f;
        float resetX = pad, btnY = this.height - btnH - pad;
        float closeX = this.width - btnW - pad;

        resetHovered = mouseX >= resetX && mouseX <= resetX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        closeHovered = mouseX >= closeX && mouseX <= closeX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        drawButton(stack, "Сбросить", resetX, btnY, btnW, btnH,
                resetHovered ? accent : new Color(18, 18, 24, 220));
        drawButton(stack, "Закрыть", closeX, btnY, btnW, btnH,
                closeHovered ? new Color(200, 60, 60, 235) : new Color(18, 18, 24, 220));
    }

    private void drawButton(MatrixStack stack, String text, float x, float y, float w, float h, Color bg) {
        Render2D.drawRoundedRect(stack, x, y, w, h, 6f, bg);
        float fs = 7.5f;
        float tw = Fonts.MEDIUM.getWidth(text, fs);
        Render2D.drawFont(stack, Fonts.MEDIUM.getFont(fs), text, x + (w - tw) / 2f, y + (h - Fonts.MEDIUM.getHeight(fs)) / 2f,
                new Color(255, 255, 255, 240));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float btnW = 90f, btnH = 22f, pad = 12f;
        float btnY = this.height - btnH - pad;

        if (button == 0) {
            // Сбросить
            if (mouseX >= pad && mouseX <= pad + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                resetLayout();
                return true;
            }
            // Закрыть
            float closeX = this.width - btnW - pad;
            if (mouseX >= closeX && mouseX <= closeX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                close();
                return true;
            }
        }
        // Клик по пустому месту / элементам: не глотаем, чтобы штатный
        // механизм перетаскивания HudElement (через EventMouse) работал.
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Каскадно раскладывает все элементы слева сверху. */
    private void resetLayout() {
        HudManager hudManager = darkvisuals.getInstance().getHudManager();
        if (hudManager == null) return;

        List<HudElement> elements = hudManager.getHudElements();
        float sw = this.client.getWindow().getScaledWidth();
        float sh = this.client.getWindow().getScaledHeight();

        float x = 12f;
        float y = 40f;
        for (HudElement element : elements) {
            float w = Math.max(1f, element.getWidth());
            float h = Math.max(1f, element.getHeight());
            if (y + h > sh - 30f) {
                y = 40f;
                x += w + 16f;
            }
            element.getPosition().getValue().setX(x / sw);
            element.getPosition().getValue().setY(y / sh);
            y += h + 10f;
        }
        try {
            if (darkvisuals.getInstance().getAutoSaveManager() != null)
                darkvisuals.getInstance().getAutoSaveManager().scheduleAutoSave();
        } catch (Throwable ignored) {}
    }
}
