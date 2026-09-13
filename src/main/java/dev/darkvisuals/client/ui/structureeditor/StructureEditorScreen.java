package dev.darkvisuals.client.ui.structureeditor;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.impl.render.StructureVisualer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

/**
 * Экран Structure Visualer: кнопка «Сделай дом» переносит игрока в пространство
 * стройки (креатив, полёт), где дом строится по-настоящему. После ESC дом
 * сохраняется и этот экран открывается снова — «Сохранить и перенести»
 * ставит построенный дом голограммой к прицелу.
 */
 public class StructureEditorScreen extends Screen {

    private static final Color BG_DIM = new Color(0, 0, 0, 140);
    private static final Color BG_PANEL = new Color(14, 14, 18, 250);

    private final StructureVisualer module;
    private final StructureData data = new StructureData();

    private float panelX, panelY, panelW, panelH;
    private float btnX, btnW, btnH;
    private float buildY, transferY, clearY;

    public StructureEditorScreen(StructureVisualer module) {
        super(Text.of("Structure Visualer"));
        this.module = module;
        module.getStructure().copyInto(data);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void layout() {
        panelW = Math.min(width - 80f, 380f);
        panelH = 236f;
        panelX = (width - panelW) / 2f;
        panelY = (height - panelH) / 2f;
        btnW = panelW - 60f;
        btnH = 26f;
        btnX = panelX + 30f;
        buildY = panelY + 108f;
        transferY = buildY + 34f;
        clearY = transferY + 34f;
    }

    @Override
    protected void init() {
        super.init();
        layout();
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int w, int h) {
        super.resize(client, w, h);
        layout();
    }

    private boolean hover(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (Fonts.REGULAR == null || Fonts.SEMIBOLD == null) return;

        MatrixStack ms = context.getMatrices();
        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        layout();

        // затемнение мира + панель
        Render2D.drawRect(ms, 0, 0, width, height, BG_DIM);
        Render2D.drawRoundedRect(ms, panelX, panelY, panelW, panelH, 12f, BG_PANEL);
        Render2D.drawBorder(ms, panelX, panelY, panelW, panelH, 12f, 1f, 1f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));

        // заголовок
        Render2D.drawFont(ms, Fonts.SEMIBOLD.getFont(12.5f), "Structure Visualer",
                panelX + 24f, panelY + 20f, Color.WHITE);

        // инфо о доме
        Render2D.drawFont(ms, Fonts.REGULAR.getFont(8.5f),
                "Блоков в доме: " + data.size(),
                panelX + 24f, panelY + 46f, new Color(190, 190, 205));

        // подсказка
        float ty = panelY + 66f;
        String[] hint = {
                "Нажмите «Сделай дом» — вас перенесёт в пространство, где",
                "можно построить дом по-настоящему (креатив, полёт).",
                "ESC — сохранить дом и вернуться сюда."
        };
        for (String line : hint) {
            Render2D.drawFont(ms, Fonts.REGULAR.getFont(7.5f), line,
                    panelX + 24f, ty, new Color(140, 140, 155));
            ty += 12f;
        }

        drawButton(ms, btnX, buildY, btnW, btnH, "Сделай дом",
                hover(mouseX, mouseY, btnX, buildY, btnW, btnH), accent, false);
        drawButton(ms, btnX, transferY, btnW, btnH, "Сохранить и перенести",
                hover(mouseX, mouseY, btnX, transferY, btnW, btnH), accent, false);
        drawButton(ms, btnX, clearY, btnW, btnH, "Очистить дом",
                hover(mouseX, mouseY, btnX, clearY, btnW, btnH), accent, true);
    }

    private void drawButton(MatrixStack ms, float x, float y, float w, float h, String text,
                            boolean hovered, Color accent, boolean danger) {
        Color base = danger
                ? new Color(225, 80, 80, hovered ? 235 : 175)
                : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hovered ? 235 : 175);
        Render2D.drawRoundedRect(ms, x, y, w, h, 6f, base);
        if (hovered) {
            Render2D.drawBorder(ms, x, y, w, h, 6f, 1f, 1f, new Color(255, 255, 255, 120));
        }
        float tw = Fonts.SEMIBOLD.getWidth(text, 8f);
        float th = Fonts.SEMIBOLD.getHeight(8f);
        Render2D.drawFont(ms, Fonts.SEMIBOLD.getFont(8f), text,
                x + (w - tw) / 2f, y + (h - th) / 2f, Color.WHITE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (hover(mouseX, mouseY, btnX, buildY, btnW, btnH)) {
                // закрывает экран и входит в пространство стройки
                module.startBuilding();
                return true;
            }
            if (hover(mouseX, mouseY, btnX, transferY, btnW, btnH)) {
                // при пустом доме только покажем уведомление, экран не закрываем
                module.applyStructure(data);
                if (!data.isEmpty()) client.setScreen(null);
                return true;
            }
            if (hover(mouseX, mouseY, btnX, clearY, btnW, btnH)) {
                data.clear();
                module.getStructure().clear();
                module.getStructure().clearAnchor();
                module.saveStructure();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
