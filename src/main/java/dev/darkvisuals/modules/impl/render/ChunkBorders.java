package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;

/**
 * ChunkBorders — сетка границ чанков вокруг игрока (как F3+G).
 * Рисуется линиями через общую очередь Render3D.
 */
public class ChunkBorders extends Module {

    private final BooleanSetting neighbors =
            new BooleanSetting("Соседние чанки", true);
    private final NumberSetting lineWidth =
            new NumberSetting("Толщина линии", 1.5f, 0.5f, 4f, 0.5f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(120, 200, 255, 180).getRGB());

    public ChunkBorders() {
        super("ChunkBorders", Category.Render, "Границы чанков вокруг игрока");
        getSettings().add(neighbors);
        getSettings().add(lineWidth);
        getSettings().add(color);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Render3D.DEBUG_LINE_WIDTH = lineWidth.getValue();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        Color base = color.getColor();
        int bottomY = mc.world.getBottomY();
        int topY = bottomY + mc.world.getHeight();
        // рисуем колонну чуть выше и ниже видимой высоты
        double yMin = bottomY - 8;
        double yMax = topY + 8;

        ChunkPos center = mc.player.getChunkPos();
        int radius = neighbors.getValue() ? 1 : 0;

        Render3D.DEBUG_LINE_WIDTH = lineWidth.getValue();

        for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
            for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
                boolean isCenter = cx == center.x && cz == center.z;
                Color c = isCenter
                        ? base
                        : new Color(base.getRed(), base.getGreen(), base.getBlue(),
                                Math.max(30, base.getAlpha() / 2));

                int startX = cx << 4;
                int startZ = cz << 4;

                // колонна текущего чанка: обводка от низа до верха
                Box chunkBox = new Box(startX, yMin, startZ, startX + 16, yMax, startZ + 16);
                Render3D.renderBoxOutline(event.getMatrices(), chunkBox, c);

                // крест в центре чанка, чтобы сетку было видно с высоты
                Vec3d mid = new Vec3d(startX + 8, yMin, startZ + 8);
                Vec3d midTop = new Vec3d(startX + 8, yMax, startZ + 8);
                Render3D.drawLine(mid, midTop, c.getRGB(), lineWidth.getValue());
            }
        }
    }
}
