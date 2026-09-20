package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.events.impl.TotemPopEvent;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

/**
 * TotemPops — счётчик сломанных тотемов за текущую жизнь.
 */
public class TotemPops extends Module {

    private final NumberSetting x = new NumberSetting("X", 60f, 0f, 3840f, 1f);
    private final NumberSetting y = new NumberSetting("Y", 120f, 0f, 2160f, 1f);
    private final NumberSetting size = new NumberSetting("Размер", 46f, 20f, 160f, 1f);
    private final NumberSetting fontSize = new NumberSetting("Шрифт", 14f, 6f, 32f, 1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 12f, 0f, 60f, 1f);
    private final ColorSetting tint = new ColorSetting("Оттенок", new Color(255, 120, 60, 70).getRGB());

    private int pops = 0;

    public TotemPops() {
        super("TotemPops", Category.Utility, "Счётчик сломанных тотемов");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        pops = 0;
    }

    @EventHandler
    public void onTotemPop(TotemPopEvent event) {
        if (mc.player == null) return;
        if (event.getEntity() == null || !event.getEntity().equals(mc.player)) return;
        pops++;
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck() || !Fonts.isLoaded()) return;

        MatrixStack matrices = event.getContext().getMatrices();

        float s = size.getValue();
        float px = x.getValue();
        float py = y.getValue();

        Render2D.drawGlass(matrices, px, py, s, s,
                1f, radius.getValue(), tint.getColor().getRGB(),
                8f, 3f, 3f, 2f);

        String label = String.valueOf(pops);
        float fs = fontSize.getValue();
        float textW = Fonts.MEDIUM.getWidth(label, fs);
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), label,
                px + (s - textW) / 2f, py + (s - Fonts.MEDIUM.getHeight(fs)) / 2f,
                pops == 0 ? new Color(200, 205, 210, 200) : new Color(255, 200, 110, 245));

        String hint = "pops";
        float fs2 = Math.min(7f, fs * 0.6f);
        float hintW = Fonts.MEDIUM.getWidth(hint, fs2);
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs2), hint,
                px + (s - hintW) / 2f, py + s + 3f, new Color(180, 185, 190, 170));
    }
}
