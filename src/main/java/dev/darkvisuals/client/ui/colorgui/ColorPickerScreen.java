package dev.darkvisuals.client.ui.colorgui;

import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;

public class ColorPickerScreen extends Screen {
    private final ColorSetting setting;
    private float hue, saturation, brightness;

     
    private boolean draggingSquare = false;
    private boolean draggingHue = false;

     
    private final int squareSize = 150;
    private final int hueWidth = 15;

     
    private int[] squarePixels;

    public ColorPickerScreen(ColorSetting setting) {
        super(Text.of("Color Picker"));
        this.setting = setting;

        Color color = new Color(setting.getValue());
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];

        generateSquareBuffer();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

 
        Render2D.drawStyledRect(context.getMatrices(),
                centerX - squareSize - hueWidth - 20, centerY - squareSize / 2 - 20,
                squareSize + hueWidth + 40, squareSize + 60, 5f,
                new Color(0, 0, 0, 180), 255);

 
        Render2D.drawFont(context.getMatrices(), Fonts.SEMIBOLD.getFont(12f),
                "Color Picker", centerX - 30, centerY - squareSize / 2 - 10, Color.WHITE);

 
        drawColorSquare(context, centerX - squareSize / 2 - 20, centerY - squareSize / 2);

 
        drawHueBar(context, centerX + squareSize / 2, centerY - squareSize / 2, hueWidth, squareSize);

 
        Color preview = Color.getHSBColor(hue, saturation, brightness);
        Render2D.drawStyledRect(context.getMatrices(),
                centerX - 30, centerY + squareSize / 2 + 10, 60, 20, 3f, preview, 255);

 
        Render2D.drawStyledRect(context.getMatrices(),
                centerX - 30, centerY + squareSize / 2 + 40, 60, 16, 3f,
                new Color(100, 20, 20, 180), 255);
        Render2D.drawFont(context.getMatrices(), Fonts.MEDIUM.getFont(9f),
                "Close", centerX - 15, centerY + squareSize / 2 + 43, Color.WHITE);

        super.render(context, mouseX, mouseY, delta);
    }

    private void generateSquareBuffer() {
        squarePixels = new int[squareSize * squareSize];
        for (int i = 0; i < squareSize; i++) {
            for (int j = 0; j < squareSize; j++) {
                float sat = (float) i / (float) squareSize;
                float bri = 1f - (float) j / (float) squareSize;
                squarePixels[j * squareSize + i] = Color.getHSBColor(hue, sat, bri).getRGB();
            }
        }
    }

    private void drawColorSquare(DrawContext context, int x, int y) {
        int step = 2;  
        for (int i = 0; i < squareSize; i += step) {
            for (int j = 0; j < squareSize; j += step) {
                int rgb = squarePixels[j * squareSize + i];
                Render2D.drawRect(context.getMatrices(), x + i, y + j, step, step, new Color(rgb));
            }
        }

 
        int markerX = x + (int) (saturation * squareSize);
        int markerY = y + (int) ((1 - brightness) * squareSize);

 
        int crossSize = 6;
        Render2D.drawRect(context.getMatrices(), markerX - crossSize, markerY - 1, crossSize * 2, 2, Color.BLACK);
        Render2D.drawRect(context.getMatrices(), markerX - 1, markerY - crossSize, 2, crossSize * 2, Color.BLACK);
        Render2D.drawRect(context.getMatrices(), markerX - crossSize + 1, markerY, crossSize * 2 - 2, 1, new Color(255, 255, 255, 200));
        Render2D.drawRect(context.getMatrices(), markerX, markerY - crossSize + 1, 1, crossSize * 2 - 2, new Color(255, 255, 255, 200));
    }

    private void drawHueBar(DrawContext context, int x, int y, int w, int h) {
        for (int j = 0; j < h; j++) {
            float hueVal = (float) j / (float) h;
            Color c = Color.getHSBColor(hueVal, 1f, 1f);
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

        int squareX = centerX - squareSize / 2 - 20;
        int squareY = centerY - squareSize / 2;

        int hueX = centerX + squareSize / 2;
        int hueY = centerY - squareSize / 2;

 
        if (isHovered(mouseX, mouseY, centerX - 30, centerY + squareSize / 2 + 40, 60, 16)) {
            this.close();
            return true;
        }

 
        if (isHovered(mouseX, mouseY, squareX, squareY, squareSize, squareSize)) {
            draggingSquare = true;
            updateSquare(mouseX, mouseY, squareX, squareY, squareSize, squareSize);
            return true;
        }

 
        if (isHovered(mouseX, mouseY, hueX, hueY, hueWidth, squareSize)) {
            draggingHue = true;
            updateHue(mouseY, hueY, squareSize);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
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

        int squareX = centerX - squareSize / 2 - 20;
        int squareY = centerY - squareSize / 2;
        int hueY = centerY - squareSize / 2;

        if (draggingSquare) {
            updateSquare(mouseX, mouseY, squareX, squareY, squareSize, squareSize);
            return true;
        } else if (draggingHue) {
            updateHue(mouseY, hueY, squareSize);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    private void updateSquare(double mouseX, double mouseY, int x, int y, int w, int h) {
        float sat = (float) (mouseX - x) / (float) w;
        float bri = 1f - (float) (mouseY - y) / (float) h;
        saturation = Math.max(0f, Math.min(1f, sat));
        brightness = Math.max(0f, Math.min(1f, bri));
        updateColor();
    }

    private void updateHue(double mouseY, int y, int h) {
        float value = (float) (mouseY - y) / (float) h;
        hue = Math.max(0f, Math.min(1f, value));
        generateSquareBuffer();  
        updateColor();
    }

    private void updateColor() {
        int rgb = Color.getHSBColor(hue, saturation, brightness).getRGB();
        setting.set(rgb);
    }

    private boolean isHovered(double mouseX, double mouseY, double x, double y, double w, double h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public void close() {
        this.client.setScreen(null);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
 
    }

    @Override
    public boolean shouldPause() {
        return false;  
    }
}
