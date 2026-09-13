package dev.darkvisuals.client.ui.crosshaireditor;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.crosshair.CrosshairPattern;
import dev.darkvisuals.client.util.math.MathUtils;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.impl.render.Crosshair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

 
public class CrosshairEditorScreen extends Screen {

    private static final int GRID = CrosshairPattern.SIZE;
    private static final float CELL = 18f;       
    private static final float PAD = 16f;        

    private final Crosshair module;
    private final CrosshairPattern pattern;      

     
    private static final Color[] PALETTE = {
            new Color(255, 255, 255), new Color(20, 20, 20),
            new Color(235, 64, 52),   new Color(52, 235, 100),
            new Color(66, 135, 245),  new Color(250, 204, 21),
            new Color(34, 211, 238),  new Color(217, 70, 239)
    };
    private Color selectedColor;
    private int selectedIndex = 0;    
    private boolean themeColorSelected;

     
    private boolean symmetry = true;
    private boolean paintingLeft, paintingRight;

     
    private float panelX, panelY, panelW, panelH;
    private float gridX, gridY;

    public CrosshairEditorScreen(Crosshair module) {
        super(Text.of("Crosshair Editor"));
        this.module = module;

        CrosshairPattern current = module.getPattern();
        this.pattern = current != null ? current.copy() : CrosshairPattern.defaultPattern();
        this.selectedColor = PALETTE[0];
    }

     
     
     

    private void layout() {
        float gridSize = GRID * CELL;                  
        float previewW = 96f;                          
        panelW = PAD + gridSize + 12f + previewW + PAD;
        panelH = PAD + 20f    + 8f + gridSize + 10f + 24f    + 10f + 26f    + PAD;
        panelX = (width - panelW) / 2f;
        panelY = (height - panelH) / 2f;
        gridX = panelX + PAD;
        gridY = panelY + PAD + 20f + 8f;
    }

    private float paletteY()  { return gridY + GRID * CELL + 10f; }
    private float buttonsY()  { return paletteY() + 24f + 10f; }
    private float previewX()  { return gridX + GRID * CELL + 12f; }

     
     
     

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        MatrixStack ms = context.getMatrices();
        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();

         
        Render2D.drawRect(ms, 0, 0, width, height, new Color(0, 0, 0, 120));

         
        Render2D.drawRoundedRect(ms, panelX, panelY, panelW, panelH, 8f, new Color(18, 18, 22, 240));
        Render2D.drawBorder(ms, panelX, panelY, panelW, panelH, 8f, 1f, 1f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));

         
        Render2D.drawFont(ms, Fonts.SEMIBOLD.getFont(11f), "Редактор прицела",
                panelX + PAD, panelY + PAD, Color.WHITE);
        String hint = "ЛКМ — рисовать, ПКМ — стирать";
        float hintW = Fonts.REGULAR.getWidth(hint, 7f);
        Render2D.drawFont(ms, Fonts.REGULAR.getFont(7f), hint,
                panelX + panelW - PAD - hintW, panelY + PAD + 3f, new Color(140, 140, 150));

        renderGrid(ms, mouseX, mouseY);
        renderPreview(ms);
        renderPalette(ms, mouseX, mouseY, accent);
        renderButtons(ms, mouseX, mouseY, accent);
    }

    private void renderGrid(MatrixStack ms, int mouseX, int mouseY) {
        float size = GRID * CELL;
         
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                boolean dark = (x + y) % 2 == 0;
                Render2D.drawRect(ms, gridX + x * CELL, gridY + y * CELL, CELL, CELL,
                        dark ? new Color(32, 32, 38) : new Color(40, 40, 46));
            }
        }

         
        int c = GRID / 2;
        Render2D.drawRect(ms, gridX + c * CELL, gridY, CELL, size, new Color(255, 255, 255, 10));
        Render2D.drawRect(ms, gridX, gridY + c * CELL, size, CELL, new Color(255, 255, 255, 10));

         
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int argb = pattern.get(x, y);
                if ((argb >>> 24) == 0) continue;
                Render2D.drawRect(ms, gridX + x * CELL + 1, gridY + y * CELL + 1,
                        CELL - 2, CELL - 2, new Color(argb, true));
            }
        }

         
        int hx = cellAtX(mouseX), hy = cellAtY(mouseY);
        if (hx >= 0 && hy >= 0) {
            Render2D.drawBorder(ms, gridX + hx * CELL, gridY + hy * CELL, CELL, CELL,
                    2f, 1f, 1f, new Color(255, 255, 255, 160));
            if (symmetry) {
                highlightMirror(ms, GRID - 1 - hx, hy);
                highlightMirror(ms, hx, GRID - 1 - hy);
                highlightMirror(ms, GRID - 1 - hx, GRID - 1 - hy);
            }
        }
    }

    private void highlightMirror(MatrixStack ms, int x, int y) {
        Render2D.drawBorder(ms, gridX + x * CELL, gridY + y * CELL, CELL, CELL,
                2f, 1f, 1f, new Color(255, 255, 255, 50));
    }

    private void renderPreview(MatrixStack ms) {
        float px = previewX();
        float pw = 96f;

        Render2D.drawFont(ms, Fonts.MEDIUM.getFont(8f), "Превью", px, gridY, new Color(160, 160, 170));

         
        float boxY = gridY + 14f;
        Render2D.drawRoundedRect(ms, px, boxY, pw, 64f, 4f, new Color(28, 28, 32));
        drawPatternAt(ms, px + pw / 2f, boxY + 32f, 2f);

         
        float boxY2 = boxY + 64f + 8f;
        Render2D.drawRoundedRect(ms, px, boxY2, pw, 64f, 4f, new Color(205, 205, 210));
        drawPatternAt(ms, px + pw / 2f, boxY2 + 32f, 2f);

         
        float boxY3 = boxY2 + 64f + 8f;
        Render2D.drawRoundedRect(ms, px, boxY3, pw, 40f, 4f, new Color(28, 28, 32));
        Render2D.drawFont(ms, Fonts.REGULAR.getFont(6.5f), "1:1", px + 4f, boxY3 + 3f, new Color(120, 120, 130));
        drawPatternAt(ms, px + pw / 2f, boxY3 + 22f, 1f);
    }

    private void drawPatternAt(MatrixStack ms, float cx, float cy, float scale) {
        float half = GRID * scale / 2f;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int argb = pattern.get(x, y);
                if ((argb >>> 24) == 0) continue;
                Render2D.drawRect(ms, cx - half + x * scale, cy - half + y * scale,
                        scale, scale, new Color(argb, true));
            }
        }
    }

    private void renderPalette(MatrixStack ms, int mouseX, int mouseY, Color accent) {
        float y = paletteY();
        float sw = 20f, gapX = 6f;
        float x = gridX;

         
        boolean hoverTheme = MathUtils.isHovered(x, y, sw, sw, mouseX, mouseY);
        Render2D.drawRoundedRect(ms, x, y, sw, sw, 4f, accent);
        if (themeColorSelected || hoverTheme)
            Render2D.drawBorder(ms, x - 1.5f, y - 1.5f, sw + 3f, sw + 3f, 5f, 1f, 1f,
                    themeColorSelected ? Color.WHITE : new Color(255, 255, 255, 120));
        x += sw + gapX;

        for (int i = 0; i < PALETTE.length; i++) {
            boolean hover = MathUtils.isHovered(x, y, sw, sw, mouseX, mouseY);
            Render2D.drawRoundedRect(ms, x, y, sw, sw, 4f, PALETTE[i]);
            if ((!themeColorSelected && selectedIndex == i) || hover)
                Render2D.drawBorder(ms, x - 1.5f, y - 1.5f, sw + 3f, sw + 3f, 5f, 1f, 1f,
                        (!themeColorSelected && selectedIndex == i) ? Color.WHITE : new Color(255, 255, 255, 120));
            x += sw + gapX;
        }

         
        float tx = gridX + GRID * CELL - 86f;
        boolean hoverSym = MathUtils.isHovered(tx, y, 86f, sw, mouseX, mouseY);
        Color symBg = symmetry
                ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200)
                : new Color(55, 55, 62, hoverSym ? 220 : 170);
        Render2D.drawRoundedRect(ms, tx, y, 86f, sw, 4f, symBg);
        String symText = "Симметрия ×4";
        float symW = Fonts.MEDIUM.getWidth(symText, 7f);
        Render2D.drawFont(ms, Fonts.MEDIUM.getFont(7f), symText,
                tx + (86f - symW) / 2f, y + (sw - Fonts.MEDIUM.getHeight(7f)) / 2f, Color.WHITE);
    }

    private void renderButtons(MatrixStack ms, int mouseX, int mouseY, Color accent) {
        float y = buttonsY();
        float bw = 84f, bh = 22f, gapX = 8f;
        float x = gridX;

        drawButton(ms, x, y, bw, bh, "Сохранить", accent, true, mouseX, mouseY);
        x += bw + gapX;
        drawButton(ms, x, y, bw, bh, "Очистить", accent, false, mouseX, mouseY);
        x += bw + gapX;
        drawButton(ms, x, y, bw, bh, "Отмена", accent, false, mouseX, mouseY);
    }

    private void drawButton(MatrixStack ms, float x, float y, float w, float h,
                            String text, Color accent, boolean primary, int mouseX, int mouseY) {
        boolean hover = MathUtils.isHovered(x, y, w, h, mouseX, mouseY);
        Color bg = primary
                ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hover ? 255 : 210)
                : new Color(55, 55, 62, hover ? 230 : 170);
        Render2D.drawRoundedRect(ms, x, y, w, h, 5f, bg);
        float tw = Fonts.MEDIUM.getWidth(text, 8f);
        Render2D.drawFont(ms, Fonts.MEDIUM.getFont(8f), text,
                x + (w - tw) / 2f, y + (h - Fonts.MEDIUM.getHeight(8f)) / 2f, Color.WHITE);
    }

     
     
     

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();

         
        int cx = cellAtX(mouseX), cy = cellAtY(mouseY);
        if (cx >= 0 && cy >= 0) {
            if (button == 0) { paintingLeft = true; paint(cx, cy, currentArgb()); return true; }
            if (button == 1) { paintingRight = true; paint(cx, cy, 0); return true; }
        }

        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

         
        float py = paletteY();
        float sw = 20f, gapX = 6f;
        float x = gridX;
        if (MathUtils.isHovered(x, py, sw, sw, (float) mouseX, (float) mouseY)) {
            themeColorSelected = true;
            return true;
        }
        x += sw + gapX;
        for (int i = 0; i < PALETTE.length; i++) {
            if (MathUtils.isHovered(x, py, sw, sw, (float) mouseX, (float) mouseY)) {
                themeColorSelected = false;
                selectedIndex = i;
                selectedColor = PALETTE[i];
                return true;
            }
            x += sw + gapX;
        }

         
        float tx = gridX + GRID * CELL - 86f;
        if (MathUtils.isHovered(tx, py, 86f, sw, (float) mouseX, (float) mouseY)) {
            symmetry = !symmetry;
            return true;
        }

         
        float by = buttonsY();
        float bw = 84f, bh = 22f, bGap = 8f;
        float bx = gridX;
        if (MathUtils.isHovered(bx, by, bw, bh, (float) mouseX, (float) mouseY)) { save(); return true; }
        bx += bw + bGap;
        if (MathUtils.isHovered(bx, by, bw, bh, (float) mouseX, (float) mouseY)) { pattern.clear(); return true; }
        bx += bw + bGap;
        if (MathUtils.isHovered(bx, by, bw, bh, (float) mouseX, (float) mouseY)) { close(); return true; }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        int cx = cellAtX(mouseX), cy = cellAtY(mouseY);
        if (cx >= 0 && cy >= 0) {
            if (paintingLeft)  { paint(cx, cy, currentArgb()); return true; }
            if (paintingRight) { paint(cx, cy, 0); return true; }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) paintingLeft = false;
        if (button == 1) paintingRight = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void paint(int x, int y, int argb) {
        pattern.set(x, y, argb);
        if (symmetry) {
            pattern.set(GRID - 1 - x, y, argb);
            pattern.set(x, GRID - 1 - y, argb);
            pattern.set(GRID - 1 - x, GRID - 1 - y, argb);
        }
    }

      
    private int currentArgb() {
        Color c = themeColorSelected
                ? ThemeManager.getInstance().getCurrentTheme().getAccentColor()
                : selectedColor;
        return (0xFF << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private int cellAtX(double mouseX) {
        int cx = (int) Math.floor((mouseX - gridX) / CELL);
        return (cx >= 0 && cx < GRID && mouseXInGridY()) ? cx : (cx >= 0 && cx < GRID ? cx : -1);
    }

     
    private boolean mouseXInGridY() { return true; }

    private int cellAtY(double mouseY) {
        int cy = (int) Math.floor((mouseY - gridY) / CELL);
        return (cy >= 0 && cy < GRID) ? cy : -1;
    }

    private void save() {
        module.applyPattern(pattern);
        close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}