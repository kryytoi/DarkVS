package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

/**
 * SessionStats — статистика сессии: время, дистанция, прыжки.
 */
public class SessionStats extends Module {

    private final NumberSetting x = new NumberSetting("X", 20f, 0f, 3840f, 1f);
    private final NumberSetting y = new NumberSetting("Y", 60f, 0f, 2160f, 1f);
    private final NumberSetting width = new NumberSetting("Ширина", 140f, 80f, 400f, 1f);
    private final NumberSetting radius = new NumberSetting("Радиус", 10f, 0f, 40f, 1f);
    private final ColorSetting tint = new ColorSetting("Оттенок", new Color(120, 200, 255, 55).getRGB());

    private long sessionStart = 0L;
    private double distance = 0.0;
    private int jumps = 0;
    private Vec3d lastPos = null;
    private boolean wasOnGround = true;

    public SessionStats() {
        super("SessionStats", Category.Utility, "Статистика игровой сессии");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        sessionStart = System.currentTimeMillis();
        distance = 0.0;
        jumps = 0;
        lastPos = null;
        wasOnGround = true;
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        Vec3d pos = mc.player.getPos();
        if (lastPos != null) {
            double dx = pos.x - lastPos.x;
            double dz = pos.z - lastPos.z;
            double moved = Math.sqrt(dx * dx + dz * dz);
            if (moved > 0.0 && moved < 10.0) distance += moved;
        }
        lastPos = pos;

        boolean onGround = mc.player.isOnGround();
        if (wasOnGround && !onGround && mc.player.getVelocity().y > 0.0) jumps++;
        wasOnGround = onGround;
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (fullNullCheck() || !Fonts.isLoaded()) return;

        MatrixStack matrices = event.getContext().getMatrices();

        long elapsed = System.currentTimeMillis() - sessionStart;
        long seconds = elapsed / 1000L;
        String time = String.format("%02d:%02d", seconds / 60L, seconds % 60L);

        float w = width.getValue();
        float fs = 7.5f;
        float lineH = Fonts.MEDIUM.getHeight(fs) + 3f;
        float h = 10f + lineH * 3f;
        float px = x.getValue();
        float py = y.getValue();

        Render2D.drawGlass(matrices, px, py, w, h,
                1f, radius.getValue(), tint.getColor().getRGB(),
                8f, 3f, 3f, 2f);

        float textX = px + 8f;
        float textY = py + 6f;
        Color c = new Color(235, 240, 245, 230);
        Color c2 = new Color(255, 255, 255, 255);

        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), "Сессия", textX, textY, c);
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), time,
                px + w - 8f - Fonts.MEDIUM.getWidth(time, fs), textY, c2);
        textY += lineH;

        String dist = String.format("%.0f", distance) + " м";
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), "Дистанция", textX, textY, c);
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), dist,
                px + w - 8f - Fonts.MEDIUM.getWidth(dist, fs), textY, c2);
        textY += lineH;

        String jumps = "Прыжки";
        String jVal = String.valueOf(this.jumps);
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), jumps, textX, textY, c);
        Render2D.drawFont(matrices, Fonts.MEDIUM.getFont(fs), jVal,
                px + w - 8f - Fonts.MEDIUM.getWidth(jVal, fs), textY, c2);
    }
}
