package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

/**
 * GlassNotes — липкая записка из жидкого стекла.
 */
public class GlassNotes extends Module {

    private final NumberSetting posX = new NumberSetting("X", 20f, 0f, 3840f, 1f);
    private final NumberSetting posY = new NumberSetting("Y", 200f, 0f, 2160f, 1f);
    private final NumberSetting width = new NumberSetting("Ширина", 150f, 60f, 500f, 1f);
    private final NumberSetting fontSize = new NumberSetting("Размер шрифта", 8f, 5f, 16f, 0.5f);
    private final NumberSetting radius = new NumberSetting("Радиус", 12f, 0f, 40f, 1f);
    private final NumberSetting distortion = new NumberSetting("Искажение", 8f, 0f, 40f, 1f);
    private final NumberSetting edge = new NumberSetting("Контур", 3f, 0f, 12f, 0.5f);
    private final NumberSetting shine = new NumberSetting("Глубина", 3f, 0f, 6f, 0.5f);
    private final ColorSetting tint = new ColorSetting("Оттенок", new Color(255, 220, 130, 60).getRGB());
    private final StringSetting text = new StringSetting("Текст", "Не забыть: ...", false);

    public GlassNotes() {
        super("GlassNotes", Category.Utility, "Липкая записка из стекла");
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck() || !Fonts.isLoaded()) return;

        String content = text.getValue();
        if (content == null || content.isEmpty()) return;

        MatrixStack matrices = event.getContext().getMatrices();

        float fs = fontSize.getValue();
        String[] lines = wrap(content, (int) ((width.getValue() - 16f) / Math.max(1f, fs * 0.55f)));
        float lineHeight = Fonts.MEDIUM.getHeight(fs) + 2f;
        float w = width.getValue();
        float h = 12f + lines.length * lineHeight;
        float px = posX.getValue();
        float py = posY.getValue();

        Render2D.drawGlass(matrices, px, py, w, h,
                1f, radius.getValue(), tint.getColor().getRGB(),
                distortion.getValue(), 3f, edge.getValue(), shine.getValue());

        for (int i = 0; i < lines.length; i++) {
            Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), lines[i],
                    px + 8f, py + 8f + i * lineHeight, new Color(255, 255, 255, 235));
        }
    }

    private String[] wrap(String text, int charsPerLine) {
        if (charsPerLine <= 0) charsPerLine = 10;
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() + word.length() + 1 > charsPerLine) {
                out.add(current.toString().trim());
                current.setLength(0);
            }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }
        if (current.length() > 0) out.add(current.toString().trim());
        return out.toArray(new String[0]);
    }
}
