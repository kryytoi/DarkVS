package dev.darkvisuals.client.ui.colorgui;
import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.awt.*;
public class CustomThemeScreen extends Screen {
    private float hue = 0.6f, saturation = 0.6f, brightness = 0.9f;
    private Color primary = Color.getHSBColor(0.6f, 0.6f, 0.9f);
    private Color secondary = Color.getHSBColor(0.6f, 0.3f, 0.5f);
    private boolean editingPrimary = true;

    private boolean draggingSquare = false;
    private boolean draggingHue = false;

    private final int squareSize = 130;
    private final int hueWidth = 14;

    private final StringBuilder nameBuffer = new StringBuilder();
    private boolean nameFocused = false;

    public CustomThemeScreen() {
        super(Text.of("Custom Theme"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - squareSize - hueWidth - 30;
        int panelY = centerY - squareSize / 2 - 40;
        int panelW = squareSize + hueWidth + 200;
        int panelH = squareSize + 110;

        Render2D.drawStyledRect(context.getMatrices(), panelX, panelY, panelW, panelH, 6f,
                new Color(0, 0, 0, 190), 255);

        Render2D.drawFont(context.getMatrices(), Fonts.SEMIBOLD.getFont(13f),
                "Своя тема", panelX + 14, panelY + 12, Color.WHITE);

        int squareX = panelX + 14;
        int squareY = panelY + 36;
        drawColorSquare(context, squareX, squareY);
        drawHueBar(context, squareX + squareSize + 8, squareY, hueWidth, squareSize);

         
        int swatchX = squareX + squareSize + hueWidth + 24;
        int swatchY = squareY;
        drawSwatch(context, swatchX, swatchY, "Цвет 1", primary, editingPrimary);
        drawSwatch(context, swatchX, swatchY + 40, "Цвет 2", secondary, !editingPrimary);

         
        int nameX = swatchX;
        int nameY = swatchY + 90;
        int nameW = 150, nameH = 20;
        Render2D.drawStyledRect(context.getMatrices(), nameX, nameY, nameW, nameH, 4f,
                nameFocused ? new Color(50, 50, 55, 220) : new Color(30, 30, 33, 200), 255);
        String shown = nameBuffer.length() == 0 && !nameFocused ? "Название темы..." : nameBuffer.toString();
        Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(9f),
                shown, nameX + 6, nameY + 6,
                nameBuffer.length() == 0 && !nameFocused ? new Color(150, 150, 150) : Color.WHITE);

         
        int btnX = nameX, btnY = nameY + nameH + 10, btnW = nameW, btnH = 22;
        boolean btnHovered = isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH);
        Render2D.drawStyledRect(context.getMatrices(), btnX, btnY, btnW, btnH, 4f,
                btnHovered ? new Color(90, 60, 200, 230) : new Color(70, 45, 170, 200), 255);
        Render2D.drawFont(context.getMatrices(), Fonts.SEMIBOLD.getFont(10f),
                "Сделать тему", btnX + 14, btnY + 6, Color.WHITE);

         
        int closeX = panelX + panelW - 26, closeY = panelY + 10;
        Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(10f), "X", closeX, closeY, Color.WHITE);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSwatch(DrawContext context, int x, int y, String label, Color color, boolean selected) {
        Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(9f), label, x, y, Color.WHITE);
        Render2D.drawStyledRect(context.getMatrices(), x, y + 12, 40, 20, 3f, color, 255);
         
        if (selected) {
            Render2D.drawStyledRect(context.getMatrices(), x - 2, y + 10, 44, 24, 4f,
                    new Color(150, 100, 200, 15), 0);
        }
    }

    private void drawColorSquare(DrawContext context, int x, int y) {
        Color hueColor = Color.getHSBColor(hue, 1f, 1f);

         
        Render2D.drawRect(context.getMatrices(), x, y, squareSize, squareSize, hueColor);

         
        Render2D.drawGradientRect(context.getMatrices(), x, y, squareSize, squareSize,
                new Color(255, 255, 255, 255), new Color(255, 255, 255, 0), true);

         
        Render2D.drawGradientRect(context.getMatrices(), x, y, squareSize, squareSize,
                new Color(0, 0, 0, 0), new Color(0, 0, 0, 255), false);

         
        int markerX = x + (int) (saturation * squareSize);
        int markerY = y + (int) ((1 - brightness) * squareSize);

         
        int crossSize = 5;
        Render2D.drawRect(context.getMatrices(), markerX - crossSize, markerY - 1, crossSize * 2, 2, Color.BLACK);
        Render2D.drawRect(context.getMatrices(), markerX - 1, markerY - crossSize, 2, crossSize * 2, Color.BLACK);
        Render2D.drawRect(context.getMatrices(), markerX - crossSize + 1, markerY, crossSize * 2 - 2, 1, new Color(255, 255, 255, 180));
        Render2D.drawRect(context.getMatrices(), markerX, markerY - crossSize + 1, 1, crossSize * 2 - 2, new Color(255, 255, 255, 180));
    }

    private void drawHueBar(DrawContext context, int x, int y, int w, int h) {
        for (int j = 0; j < h; j++) {
            Color c = Color.getHSBColor((float) j / h, 1f, 1f);
            Render2D.drawRect(context.getMatrices(), x, y + j, w, 1, c);
        }
         
        int markerY = y + (int) (hue * h);
        Render2D.drawRect(context.getMatrices(), x - 1, markerY - 1, w + 2, 2, Color.BLACK);
        Render2D.drawRect(context.getMatrices(), x - 1, markerY, w + 2, 1, new Color(220, 220, 220, 200));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - squareSize - hueWidth - 30;
        int panelY = centerY - squareSize / 2 - 40;
        int panelW = squareSize + hueWidth + 200;

        int squareX = panelX + 14;
        int squareY = panelY + 36;
        int hueX = squareX + squareSize + 8;

        int swatchX = squareX + squareSize + hueWidth + 24;
        int swatchY = squareY;

         
        if (isHovered(mouseX, mouseY, panelX + panelW - 26, panelY + 10, 12, 12)) {
            close();
            return true;
        }

        if (isHovered(mouseX, mouseY, squareX, squareY, squareSize, squareSize)) {
            draggingSquare = true;
            updateSquare(mouseX, mouseY, squareX, squareY);
            nameFocused = false;
            return true;
        }
        if (isHovered(mouseX, mouseY, hueX, squareY, hueWidth, squareSize)) {
            draggingHue = true;
            updateHue(mouseY, squareY);
            nameFocused = false;
            return true;
        }
        if (isHovered(mouseX, mouseY, swatchX, swatchY, 44, 32)) {
            editingPrimary = true;
            syncHsbFromActive();
            nameFocused = false;
            return true;
        }
        if (isHovered(mouseX, mouseY, swatchX, swatchY + 40, 44, 32)) {
            editingPrimary = false;
            syncHsbFromActive();
            nameFocused = false;
            return true;
        }

        int nameX = swatchX, nameY = swatchY + 90, nameW = 150, nameH = 20;
        if (isHovered(mouseX, mouseY, nameX, nameY, nameW, nameH)) {
            nameFocused = true;
            return true;
        }
        nameFocused = false;

        int btnX = nameX, btnY = nameY + nameH + 10, btnW = nameW, btnH = 22;
        if (isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
            createTheme();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void syncHsbFromActive() {
        Color c = editingPrimary ? primary : secondary;
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hue = hsb[0]; saturation = hsb[1]; brightness = hsb[2];
    }

    private void createTheme() {
        String name = nameBuffer.toString().trim();
        ThemeManager.getInstance().addCustomTheme(name, primary, secondary);
        close();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSquare = false;
        draggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelX = centerX - squareSize - hueWidth - 30;
        int panelY = centerY - squareSize / 2 - 40;
        int squareX = panelX + 14;
        int squareY = panelY + 36;

        if (draggingSquare) {
            updateSquare(mouseX, mouseY, squareX, squareY);
            return true;
        } else if (draggingHue) {
            updateHue(mouseY, squareY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    private void updateSquare(double mouseX, double mouseY, int x, int y) {
        saturation = Math.max(0f, Math.min(1f, (float) (mouseX - x) / squareSize));
        brightness = Math.max(0f, Math.min(1f, 1f - (float) (mouseY - y) / squareSize));
        updateActiveColor();
    }

    private void updateHue(double mouseY, int y) {
        hue = Math.max(0f, Math.min(1f, (float) (mouseY - y) / squareSize));
        updateActiveColor();
    }

    private void updateActiveColor() {
        Color c = Color.getHSBColor(hue, saturation, brightness);
        if (editingPrimary) primary = c; else secondary = c;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (nameFocused && nameBuffer.length() < 24 && !Character.isISOControl(chr)) {
            nameBuffer.append(chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameFocused) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && nameBuffer.length() > 0) {
                nameBuffer.deleteCharAt(nameBuffer.length() - 1);
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                createTheme();
                return true;
            }
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isHovered(double mouseX, double mouseY, double x, double y, double w, double h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public void close() {
        this.client.setScreen(null);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean shouldPause() {
        return false;
    }
}
