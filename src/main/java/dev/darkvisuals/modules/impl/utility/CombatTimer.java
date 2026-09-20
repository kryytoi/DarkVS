package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;

/**
 * CombatTimer — отсчёт времени после выхода из комбата.
 */
public class CombatTimer extends Module {

    private final NumberSetting duration = new NumberSetting("Длительность", 10f, 1f, 60f, 1f);
    private final NumberSetting x = new NumberSetting("X", 20f, 0f, 3840f, 1f);
    private final NumberSetting y = new NumberSetting("Y", 120f, 0f, 2160f, 1f);
    private final NumberSetting width = new NumberSetting("Ширина", 150f, 60f, 400f, 1f);
    private final NumberSetting height = new NumberSetting("Высота", 22f, 12f, 60f, 1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 11f, 0f, 40f, 1f);
    private final ColorSetting tint = new ColorSetting("Оттенок", new Color(255, 90, 90, 70).getRGB());

    private long lastCombatMs = -10_000L;

    public CombatTimer() {
        super("CombatTimer", Category.Utility, "Таймер выхода из комбата");
    }

    @EventHandler
    public void onAttack(EventAttackEntity event) {
        if (mc.player == null) return;
        if (event.getPlayer() != mc.player) return;
        if (!(event.getTarget() instanceof PlayerEntity)) return;
        lastCombatMs = System.currentTimeMillis();
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck() || !Fonts.isLoaded()) return;

        long totalMs = (long) (duration.getValue() * 1000f);
        long age = System.currentTimeMillis() - lastCombatMs;
        if (age < 0 || age > totalMs) return;

        float remaining = 1f - age / (float) totalMs;
        int secondsLeft = (int) Math.ceil((totalMs - age) / 1000.0);

        MatrixStack matrices = event.getContext().getMatrices();

        float w = width.getValue();
        float h = height.getValue();
        float px = x.getValue();
        float py = y.getValue();

        Render2D.drawGlass(matrices, px, py, w, h,
                1f, radius.getValue(), tint.getColor().getRGB(),
                8f, 3f, 3f, 2f);

        String label = "PvP " + secondsLeft + "s";
        float fs = Math.min(8f, h * 0.5f);
        float textW = Fonts.MEDIUM.getWidth(label, fs);
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), label,
                px + (w - textW) / 2f, py + (h - Fonts.MEDIUM.getHeight(fs)) / 2f,
                new Color(255, 255, 255, 235));

        // полоса оставшегося времени
        float barH = 2f;
        Render2D.drawRect(matrices, px + 6f, py + h - 4f, (w - 12f) * remaining, barH,
                new Color(255, 120, 120, 220));
    }
}
