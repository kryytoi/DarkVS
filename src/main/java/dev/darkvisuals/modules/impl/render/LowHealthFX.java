package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

/**
 * LowHealthFX — пульсирующая цветная виньетка по краям экрана при низком здоровье.
 */
public class LowHealthFX extends Module {

    private final NumberSetting threshold =
            new NumberSetting("Порог здоровья", 6f, 2f, 16f, 0.5f);
    private final NumberSetting intensity =
            new NumberSetting("Сила", 0.7f, 0.1f, 1.0f, 0.05f);
    private final BooleanSetting pulse =
            new BooleanSetting("Пульсация", true);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(220, 30, 30, 255).getRGB());

    public LowHealthFX() {
        super("LowHealthFX", Category.Render, "Виньетка при низком здоровье");
        getSettings().add(threshold);
        getSettings().add(intensity);
        getSettings().add(pulse);
        getSettings().add(color);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;
        if (mc.player.isDead()) return;

        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        float max = Math.max(1f, threshold.getValue());

        if (health > max) return;

        // 1.0 на пороге -> 0.0 при нулевом здоровье
        float danger = Math.max(0f, Math.min(1f, 1f - health / max));

        DrawContext context = e.getContext();
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        float strength = intensity.getValue() * (0.35f + 0.65f * danger);
        if (pulse.getValue()) {
            // пульс как сердцебиение, учащается при меньшем здоровье
            double rate = 2.0 + danger * 5.0;
            double beat = Math.abs(Math.sin(System.currentTimeMillis() / 1000.0 * Math.PI * rate));
            strength *= (float) (0.6 + 0.4 * beat);
        }

        Color base = color.getColor();
        int maxAlpha = (int) (190 * Math.max(0f, Math.min(1f, strength)));

        drawVignette(context, width, height, maxAlpha, base);
    }

    private void drawVignette(DrawContext context, int width, int height, int maxAlpha, Color base) {
        if (maxAlpha <= 2) return;
        int band = Math.max(30, Math.round(Math.min(width, height) * 0.22f));
        int c = Math.max(0, Math.min(255, maxAlpha));

        int argb = (c << 24) | (base.getRed() << 16) | (base.getGreen() << 8) | base.getBlue();
        int transparent = 0;

        context.fillGradient(0, 0, width, band, argb, transparent);
        context.fillGradient(0, height - band, width, height, transparent, argb);
        context.fillGradient(0, 0, band, height, argb, transparent);
        context.fillGradient(width - band, 0, width, height, transparent, argb);
    }
}
