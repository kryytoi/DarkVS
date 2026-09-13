package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.Color;

/**
 * HitMarker — маркер-крестик в центре экрана при ударе по цели.
 * Четыре диагональные полосы разлетаются от центра и гаснут.
 */
public class HitMarker extends Module {

    private static final long FADE_MS = 400L;

    private final NumberSetting size =
            new NumberSetting("Размер", 7f, 3f, 20f, 0.5f);
    private final NumberSetting thickness =
            new NumberSetting("Толщина", 2f, 1f, 5f, 0.5f);
    private final NumberSetting spread =
            new NumberSetting("Разлёт", 3f, 0f, 12f, 0.5f);
    private final BooleanSetting onlyPlayers =
            new BooleanSetting("Только игроки", false);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 255, 255, 255).getRGB());

    private long lastHitMs = -10_000L;

    public HitMarker() {
        super("HitMarker", Category.Render, "Маркер попадания в центре экрана");
        getSettings().add(size);
        getSettings().add(thickness);
        getSettings().add(spread);
        getSettings().add(onlyPlayers);
        getSettings().add(color);
    }

    @EventHandler
    public void onAttack(EventAttackEntity event) {
        if (mc.player == null) return;
        if (event.getPlayer() != mc.player) return;
        if (onlyPlayers.getValue() && !(event.getTarget() instanceof net.minecraft.entity.player.PlayerEntity)) return;
        lastHitMs = System.currentTimeMillis();
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        long age = System.currentTimeMillis() - lastHitMs;
        if (age < 0 || age > FADE_MS) return;

        float progress = age / (float) FADE_MS; // 0 -> 1
        float fade = 1f - progress;
        // плавное затухание в конце
        float eased = fade * fade;

        DrawContext context = event.getContext();
        int cx = context.getScaledWindowWidth() / 2;
        int cy = context.getScaledWindowHeight() / 2;

        float s = size.getValue();
        float gap = 3f + spread.getValue() * progress; // полосы разлетаются
        float w = Math.max(1f, thickness.getValue() * (0.4f + 0.6f * fade));

        Color base = color.getColor();
        Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                (int) (255 * Math.max(0f, Math.min(1f, eased))));

        MatrixStack matrices = context.getMatrices();

        // четыре диагональные полосы: / \ по обе стороны от центра
        float o = gap;
        float e = gap + s;
        Render2D.drawLine(matrices, cx - e, cy - e, cx - o, cy - o, w, c); // лево-верх /
        Render2D.drawLine(matrices, cx + o, cy - o, cx + e, cy - e, w, c); // право-верх \
        Render2D.drawLine(matrices, cx - e, cy + e, cx - o, cy + o, w, c); // лево-низ \
        Render2D.drawLine(matrices, cx + o, cy + o, cx + e, cy + e, w, c); // право-низ /
    }
}
