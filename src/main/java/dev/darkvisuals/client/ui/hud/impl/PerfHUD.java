package dev.darkvisuals.client.ui.hud.impl;

import dev.darkvisuals.client.events.impl.EventRender2D;
import dev.darkvisuals.client.ui.hud.HudElement;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;

import java.awt.*;
import java.util.List;

public class PerfHUD extends HudElement {
    private static final float WIDTH = 180f;
    private static final float ROW_H = 10f;
    private static final float PADDING = 4f;

    public PerfHUD() {
        super("Perf");
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        var matrices = e.getContext().getMatrices();

        Perf.endFrame();

        List<Perf.Row> rows = Perf.snapshot();
        int max = Math.min(10, rows.size());
        float h = ROW_H * (max + 1) + PADDING * 2;
        setBounds(getX(), getY(), WIDTH, h);

        Render2D.drawHudBackground(matrices, getX(), getY(), WIDTH, h, 4f, 1f);

        float x = getX() + PADDING;
        float y = getY() + PADDING;
        float size = 7.5f;
        Render2D.drawFont(matrices, Fonts.REGULAR.getFont(size), "Section", x, y, Color.WHITE);
        Render2D.drawFont(matrices, Fonts.REGULAR.getFont(size), "CPU ms % | GPU ms %", x + 82f, y, Color.LIGHT_GRAY);
        y += ROW_H;

        for (int i = 0; i < max; i++) {
            var r = rows.get(i);
            String left = r.name;
            String right = String.format("%.2f %2.0f | %.2f %2.0f", r.cpuMs, r.cpuPct, r.gpuMs, r.gpuPct);
            Render2D.drawFont(matrices, Fonts.REGULAR.getFont(size), left, x, y, Color.WHITE);
            float rw = Fonts.REGULAR.getWidth(right, size);
            Render2D.drawFont(matrices, Fonts.REGULAR.getFont(size), right, getX() + WIDTH - PADDING - rw, y, Color.WHITE);
            y += ROW_H;
        }

        super.onRender2D(e);
    }
}
