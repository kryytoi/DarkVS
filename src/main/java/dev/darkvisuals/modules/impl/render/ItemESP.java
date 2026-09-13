package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;

/**
 * ItemESP — подсветка выпавших предметов на земле.
 */
public class ItemESP extends Module {

    private final NumberSetting range =
            new NumberSetting("Дистанция", 40f, 8f, 96f, 1f);
    private final NumberSetting size =
            new NumberSetting("Размер обводки", 1.0f, 0.5f, 2.0f, 0.1f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 210, 90, 200).getRGB());

    public ItemESP() {
        super("ItemESP", Category.Render, "Подсветка выпавших предметов");
        getSettings().add(range);
        getSettings().add(size);
        getSettings().add(color);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        float maxRange = range.getValue();
        Color base = color.getColor();
        Vec3d playerPos = mc.player.getPos();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity)) continue;

            double dist = entity.getPos().distanceTo(playerPos);
            if (dist > maxRange) continue;

            float fade = 1f - (float) (dist / maxRange) * 0.6f;
            int alpha = (int) (base.getAlpha() * fade);
            Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                    Math.max(20, Math.min(255, alpha)));

            Box box = entity.getBoundingBox();
            if (size.getValue() != 1.0f) {
                Vec3d center = box.getCenter();
                double half = Math.max(0.15, 0.25 * size.getValue());
                box = new Box(center.x - half, center.y - half, center.z - half,
                        center.x + half, center.y + half, center.z + half);
            }
            Render3D.renderBoxOutline(event.getMatrices(), box, c);
        }
    }
}
