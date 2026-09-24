package dev.darkvisuals.client.ui.graffity;

import dev.darkvisuals.client.managers.GraffityManager;
import dev.darkvisuals.client.managers.NotifyManager;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.notify.Notify;
import dev.darkvisuals.client.util.notify.NotifyIcons;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.darkvisuals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * GraffityEditorScreen - пиксельный редактор граффити.
 *
 * Рисуете спрей на сетке 32x32, сохраняете - PNG пишется в
 * {gameDir}/darkvisuals/grafi/, регистрируется в GraffityManager
 * и сразу появляется в колесе выбора. Также подхватывается при
 * перезагрузке ресурсов (F3+T).
 *
 * Управление: ЛКМ - рисовать, ПКМ - стирать, колесо/кнопки - палитра,
 * E - ластик, B - кисть, F - заливка, Ctrl+Z - отмена, ESC - выход.
 */
public class GraffityEditorScreen extends Screen {

    private static final int GRID = 32;
    private static final int[] PALETTE = {
            0xFFFFFFFF, 0xFF000000, 0xFFE03131, 0xFFFF7043, 0xFFFFD43B, 0xFF69DB7C,
            0xFF38D9A9, 0xFF4DABF7, 0xFF4C6EF5, 0xFF9775FA, 0xFFF783AC, 0xFF8D5524,
            0xFFADB5BD, 0xFF495057, 0xFF74C0FC, 0xFFFFA8A8
    };

    private final int[][] pixels = new int[GRID][GRID];
    private final Deque<int[][]> undoStack = new ArrayDeque<>();

    private int colorIndex = 4;
    private boolean eraser = false;
    private boolean painting = false;
    private boolean paintErase = false;
    private boolean fillMode = false;

    private boolean saveHovered = false, clearHovered = false, undoHovered = false,
            brushHovered = false, eraserHovered = false, fillHovered = false;
    private final boolean[] paletteHovered = new boolean[PALETTE.length];

    private float canvasX, canvasY, cell, canvasSize;

    public GraffityEditorScreen() {
        super(Text.of("darkvisuals-graffity-editor"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack stack = context.getMatrices();
        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();

        Render2D.drawRoundedRect(stack, 0, 0, this.width, this.height, 0f, new Color(8, 8, 12, 190));

        // Заголовок
        float ts = 10f;
        String title = "Редактор граффити";
        float tw = Fonts.SEMIBOLD.getWidth(title, ts);
        Render2D.drawFont(stack, Fonts.SEMIBOLD.getFont(ts), title, this.width / 2f - tw / 2f, 10f,
                new Color(255, 255, 255, 240));

        // Позиционирование канваса
        canvasSize = Math.min(this.width, this.height) * 0.55f;
        cell = canvasSize / GRID;
        canvasX = this.width / 2f - canvasSize / 2f;
        canvasY = 30f;

        // Фон канваса (шахматка) и сетка
        Render2D.drawRoundedRect(stack, canvasX - 2f, canvasY - 2f, canvasSize + 4f, canvasSize + 4f, 4f,
                new Color(0, 0, 0, 230));
        for (int gy = 0; gy < GRID; gy++) {
            for (int gx = 0; gx < GRID; gx++) {
                boolean checker = ((gx / 4) + (gy / 4)) % 2 == 0;
                int base = checker ? 38 : 30;
                int argb = pixels[gx][gy];
                int c = (argb & 0xFF000000) != 0 ? argb : ((base) << 16 | (base) << 8 | base | 0xFF000000);
                float a = ((c >>> 24) & 0xFF) / 255f;
                Render2D.drawRoundedRect(stack,
                        canvasX + gx * cell, canvasY + gy * cell, cell + 0.3f, cell + 0.3f, 0f,
                        new Color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, (int) (a * 255)));
            }
        }

        // Рамка канваса в цвет темы
        Render2D.drawRoundedRect(stack, canvasX - 2f, canvasY - 2f, canvasSize + 4f, 1.6f, 0.8f, accent);
        Render2D.drawRoundedRect(stack, canvasX - 2f, canvasY + canvasSize + 2.4f, canvasSize + 4f, 1.6f, 0.8f, accent);

        // Панель инструментов под канвасом
        float toolY = canvasY + canvasSize + 12f;
        float btnW = 58f, btnH = 18f, gap = 8f;
        float bx = this.width / 2f - (btnW * 3 + gap * 2) / 2f;

        brushHovered = hover(mouseX, mouseY, bx, toolY, btnW, btnH);
        eraserHovered = hover(mouseX, mouseY, bx + btnW + gap, toolY, btnW, btnH);
        fillHovered = hover(mouseX, mouseY, bx + (btnW + gap) * 2, toolY, btnW, btnH);

        drawToolButton(stack, "Кисть (B)", bx, toolY, btnW, btnH, !eraser && !fillMode, brushHovered);
        drawToolButton(stack, "Ластик (E)", bx + btnW + gap, toolY, btnW, btnH, eraser, eraserHovered);
        drawToolButton(stack, "Заливка (F)", bx + (btnW + gap) * 2, toolY, btnW, btnH, fillMode, fillHovered);

        // Действия
        float actY = toolY + btnH + gap;
        undoHovered = hover(mouseX, mouseY, bx, actY, btnW, btnH);
        clearHovered = hover(mouseX, mouseY, bx + btnW + gap, actY, btnW, btnH);
        saveHovered = hover(mouseX, mouseY, bx + (btnW + gap) * 2, actY, btnW, btnH);

        drawToolButton(stack, "Отмена (Z)", bx, actY, btnW, btnH, false, undoHovered);
        drawToolButton(stack, "Очистить", bx + btnW + gap, actY, btnW, btnH, false, clearHovered);
        drawToolButton(stack, "Сохранить", bx + (btnW + gap) * 2, actY, btnW, btnH, false, saveHovered);

        // Палитра справа
        float palX = canvasX + canvasSize + 18f;
        float palSize = 16f, palGap = 5f;
        if (palX + palSize * 2 + palGap * 3 < this.width) {
            for (int i = 0; i < PALETTE.length; i++) {
                int col = i % 2, row = i / 2;
                float px = palX + col * (palSize + palGap);
                float py = canvasY + row * (palSize + palGap);
                paletteHovered[i] = hover(mouseX, mouseY, px, py, palSize, palSize);
                boolean selected = i == colorIndex && !eraser;
                Render2D.drawRoundedRect(stack, px - (selected ? 1.5f : 0), py - (selected ? 1.5f : 0),
                        palSize + (selected ? 3f : 0), palSize + (selected ? 3f : 0), 3f,
                        selected ? accent : new Color(255, 255, 255, 40));
                int c = PALETTE[i];
                Render2D.drawRoundedRect(stack, px, py, palSize, palSize, 2.5f,
                        new Color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 255));
            }
        }

        // Подсказка
        String hint = "ЛКМ - рисовать | ПКМ - стирать | ESC - выход | Спрей появится в колесе (G)";
        float hsz = 6.5f;
        float hw = Fonts.MEDIUM.getWidth(hint, hsz);
        Render2D.drawFont(stack, Fonts.MEDIUM.getFont(hsz), hint, this.width / 2f - hw / 2f, this.height - 14f,
                new Color(255, 255, 255, 120));
    }

    private boolean hover(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void drawToolButton(MatrixStack stack, String text, float x, float y, float w, float h, boolean active, boolean hovered) {
        Color bg = active
                ? new Color(45, 45, 60, 235)
                : (hovered ? new Color(35, 35, 48, 230) : new Color(22, 22, 30, 220));
        Render2D.drawRoundedRect(stack, x, y, w, h, 5f, bg);
        float fs = 6.5f;
        float tw = Fonts.MEDIUM.getWidth(text, fs);
        Render2D.drawFont(stack, Fonts.MEDIUM.getFont(fs), text, x + (w - tw) / 2f, y + (h - Fonts.MEDIUM.getHeight(fs)) / 2f,
                new Color(255, 255, 255, active ? 250 : 200));
    }

    private int cellAt(double mx, double my) {
        int gx = (int) ((mx - canvasX) / cell);
        int gy = (int) ((my - canvasY) / cell);
        if (gx < 0 || gy < 0 || gx >= GRID || gy >= GRID) return -1;
        return gy * GRID + gx;
    }

    private void paintAt(double mx, double my, boolean erase) {
        int idx = cellAt(mx, my);
        if (idx < 0) return;
        pixels[idx % GRID][idx / GRID] = erase ? 0 : PALETTE[colorIndex];
    }

    private void floodFill(int gx, int gy, int target, int replacement) {
        if (target == replacement) return;
        if (gx < 0 || gy < 0 || gx >= GRID || gy >= GRID) return;
        if (pixels[gx][gy] != target) return;
        pixels[gx][gy] = replacement;
        floodFill(gx + 1, gy, target, replacement);
        floodFill(gx - 1, gy, target, replacement);
        floodFill(gx, gy + 1, target, replacement);
        floodFill(gx, gy - 1, target, replacement);
    }

    private void snapshot() {
        int[][] copy = new int[GRID][GRID];
        for (int y = 0; y < GRID; y++) System.arraycopy(pixels[y], 0, copy[y], 0, GRID);
        if (undoStack.size() >= 24) undoStack.pollFirst();
        undoStack.addLast(copy);
    }

    private void undo() {
        int[][] prev = undoStack.pollLast();
        if (prev == null) return;
        for (int y = 0; y < GRID; y++) System.arraycopy(prev[y], 0, pixels[y], 0, GRID);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Палитра
        float palX = canvasX + canvasSize + 18f;
        float palSize = 16f, palGap = 5f;
        for (int i = 0; i < PALETTE.length; i++) {
            int col = i % 2, row = i / 2;
            float px = palX + col * (palSize + palGap);
            float py = canvasY + row * (palSize + palGap);
            if (hover(mouseX, mouseY, px, py, palSize, palSize)) {
                colorIndex = i;
                eraser = false;
                fillMode = false;
                return true;
            }
        }

        // Кнопки
        float toolY = canvasY + canvasSize + 12f;
        float btnW = 58f, btnH = 18f, gap = 8f;
        float bx = this.width / 2f - (btnW * 3 + gap * 2) / 2f;
        float actY = toolY + btnH + gap;

        if (hover(mouseX, mouseY, bx, toolY, btnW, btnH)) { eraser = false; fillMode = false; return true; }
        if (hover(mouseX, mouseY, bx + btnW + gap, toolY, btnW, btnH)) { eraser = true; fillMode = false; return true; }
        if (hover(mouseX, mouseY, bx + (btnW + gap) * 2, toolY, btnW, btnH)) { fillMode = true; return true; }

        if (hover(mouseX, mouseY, bx, actY, btnW, btnH)) { undo(); return true; }
        if (hover(mouseX, mouseY, bx + btnW + gap, actY, btnW, btnH)) {
            snapshot();
            for (int y = 0; y < GRID; y++) for (int x = 0; x < GRID; x++) pixels[x][y] = 0;
            return true;
        }
        if (hover(mouseX, mouseY, bx + (btnW + gap) * 2, actY, btnW, btnH)) { save(); return true; }

        // Рисование по канвасу
        int idx = cellAt(mouseX, mouseY);
        if (idx >= 0) {
            snapshot();
            painting = true;
            paintErase = button == 1 || eraser;
            if (fillMode && !paintErase) {
                int gx = idx % GRID, gy = idx / GRID;
                floodFill(gx, gy, pixels[gx][gy], PALETTE[colorIndex]);
                painting = false;
            } else {
                paintAt(mouseX, mouseY, paintErase);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (painting) paintAt(mouseX, mouseY, paintErase);
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        painting = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean ctrl = (modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL) != 0;
        switch (keyCode) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_E -> { eraser = true; fillMode = false; return true; }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_B -> { eraser = false; fillMode = false; return true; }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_F -> { fillMode = true; return true; }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_Z -> { if (ctrl) { undo(); return true; } }
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE -> { close(); return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Сохраняет спрей: PNG в darkvisuals/grafi/ + мгновенная регистрация текстуры. */
    private void save() {
        boolean empty = true;
        for (int y = 0; y < GRID && empty; y++)
            for (int x = 0; x < GRID; x++)
                if (pixels[x][y] != 0) { empty = false; break; }
        if (empty) return;

        NativeImage image = new NativeImage(GRID, GRID, true);
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int argb = pixels[x][y];
                // ABGR (NativeImage) <- ARGB
                int abgr = (argb & 0xFF000000) | ((argb & 0xFF) << 16) | (argb & 0xFF00) | ((argb >> 16) & 0xFF);
                image.setColorArgb(x, y, abgr);
            }
        }

        try {
            Path dir = dev.darkvisuals.darkvisuals.getInstance().getGlobalsDir().toPath().resolve("grafi");
            Files.createDirectories(dir);
            int n = 1;
            while (Files.exists(dir.resolve("my_spray" + n + ".png"))) n++;
            String name = "my_spray" + n;
            image.writeTo(dir.resolve(name + ".png"));

            GraffityManager.registerDynamic(name, image);

            NotifyManager nm = darkvisuals.getInstance().getNotifyManager();
            if (nm != null) nm.add(new Notify(NotifyIcons.successIcon, "Спрей сохранён: " + name, 1500));
            close();
        } catch (IOException e) {
            NotifyManager nm = darkvisuals.getInstance().getNotifyManager();
            if (nm != null) nm.add(new Notify(NotifyIcons.failIcon, "Ошибка сохранения: " + e.getMessage(), 2000));
        }
    }
}
