package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;

/**
 * Hitboxes — обводка хитбоксов сущностей вокруг игрока.
 * Рисуется через общую очередь Render3D (очищается и рисуется самим миксином кадра).
 */
public class Hitboxes extends Module {

    private final NumberSetting range =
            new NumberSetting("Дистанция", 32f, 8f, 96f, 1f);
    private final BooleanSetting playersOnly =
            new BooleanSetting("Только игроки", false);
    private final BooleanSetting showSelf =
            new BooleanSetting("Показывать себя", true);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 80, 80, 200).getRGB());

    public Hitboxes() {
        super("Hitboxes", Category.Render, "Обводка хитбоксов сущностей");
        getSettings().add(range);
        getSettings().add(playersOnly);
        getSettings().add(showSelf);
        getSettings().add(color);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        float maxRange = range.getValue();
        Color base = color.getColor();
        Vec3d playerPos = mc.player.getPos();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player && !showSelf.getValue()) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (playersOnly.getValue() && !(entity instanceof PlayerEntity)) continue;

            double dist = entity.getPos().distanceTo(playerPos);
            if (dist > maxRange) continue;

            // чем дальше — тем прозрачнее
            float fade = 1f - (float) (dist / maxRange) * 0.6f;
            int alpha = (int) (base.getAlpha() * fade);
            Color c = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                    Math.max(20, Math.min(255, alpha)));

            Box box = entity.getBoundingBox();
            Render3D.renderBoxOutline(event.getMatrices(), box, c);
        }
    }
}
