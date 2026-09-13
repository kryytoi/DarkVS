package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;

/**
 * CrystalRadius — круг радиуса взрыва эндер-кристаллов на земле.
 * Полезно в кристальном PvP: сразу видно опасную зону.
 */
public class CrystalRadius extends Module {

    private final NumberSetting range =
            new NumberSetting("Дистанция", 32f, 8f, 96f, 1f);
    private final NumberSetting radius =
            new NumberSetting("Радиус взрыва", 6f, 1f, 12f, 0.5f);
    private final NumberSetting lineWidth =
            new NumberSetting("Толщина линии", 2f, 0.5f, 5f, 0.5f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 70, 70, 190).getRGB());

    public CrystalRadius() {
        super("CrystalRadius", Category.Render, "Круг радиуса взрыва кристаллов");
        getSettings().add(range);
        getSettings().add(radius);
        getSettings().add(lineWidth);
        getSettings().add(color);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        float maxRange = range.getValue();
        float r = radius.getValue();
        Color base = color.getColor();
        Vec3d playerPos = mc.player.getPos();
        long now = System.currentTimeMillis();

        // лёгкая пульсация, чтобы круг был заметен
        float pulse = 0.8f + 0.2f * (float) Math.sin(now / 300.0);
        Render3D.DEBUG_LINE_WIDTH = lineWidth.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;

            double dist = entity.getPos().distanceTo(playerPos);
            if (dist > maxRange) continue;

            float fade = 1f - (float) (dist / maxRange) * 0.5f;
            int alpha = (int) (base.getAlpha() * fade * pulse);
            alpha = Math.max(25, Math.min(255, alpha));
            Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);

            drawCircle(entity.getX(), entity.getY(), entity.getZ(), r, c);
        }
    }

    private void drawCircle(double x, double y, double z, double radius, Color color) {
        int segments = 40;
        int rgba = color.getRGB();
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2;
            Vec3d p1 = new Vec3d(x + Math.cos(a1) * radius, y, z + Math.sin(a1) * radius);
            Vec3d p2 = new Vec3d(x + Math.cos(a2) * radius, y, z + Math.sin(a2) * radius);
            Render3D.drawLine(p1, p2, rgba, lineWidth.getValue());
        }
    }
}
