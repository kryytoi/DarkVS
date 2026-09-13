package dev.darkvisuals.modules.impl.render;

import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.util.perf.Perf;
import dev.darkvisuals.client.util.renderer.Render3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;

 
public class HitRange extends Module {

    private final NumberSetting radius = new NumberSetting("setting.hitrange.radius", 3.0f, 1.0f, 6.0f, 0.5f);
    private final NumberSetting lineWidth = new NumberSetting("setting.hitrange.lineWidth", 2.0f, 0.5f, 6.0f, 0.5f);
    private final NumberSetting segments = new NumberSetting("setting.hitrange.segments", 48f, 16f, 96f, 4f);

    private final ColorSetting safeColor = new ColorSetting("setting.hitrange.safeColor", new Color(80, 200, 255, 255).getRGB());
    private final ColorSetting dangerColor = new ColorSetting("setting.hitrange.dangerColor", new Color(255, 40, 40, 255).getRGB());

    public HitRange() {
        super("HitRange", Category.Render, "module.hitrange.description");
        getSettings().add(radius);
        getSettings().add(lineWidth);
        getSettings().add(segments);
        getSettings().add(safeColor);
        getSettings().add(dangerColor);
    }

    @EventHandler
    private void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;
        ClientPlayerEntity player = mc.player;

        try (var __ = Perf.scopeCpu("HitRange.onRender3D")) {
            float tickDelta = Render3D.getTickDelta();
            double x = player.prevX + (player.getX() - player.prevX) * tickDelta;
            double y = player.prevY + (player.getY() - player.prevY) * tickDelta;
            double z = player.prevZ + (player.getZ() - player.prevZ) * tickDelta;

            double r = radius.getValue();
             
             
            Vec3d center = new Vec3d(x, y + 0.02, z);

            boolean danger = isEntityInRange(player, r);
            Color base = danger ? dangerColor.getColor() : safeColor.getColor();
            int argb = base.getRGB();

             
             
             
             
            Render3D.DEBUG_LINE_WIDTH = lineWidth.getValue();

            int segs = Math.max(8, segments.getValue().intValue());
            Vec3d prev = null;
            for (int i = 0; i <= segs; i++) {
                double angle = (Math.PI * 2 * i) / segs;
                Vec3d point = new Vec3d(
                        center.x + Math.cos(angle) * r,
                        center.y,
                        center.z + Math.sin(angle) * r
                );
                if (prev != null) Render3D.drawLine(prev, point, argb, lineWidth.getValue());
                prev = point;
            }
        }
    }

      
    private boolean isEntityInRange(ClientPlayerEntity player, double r) {
        if (mc.world == null) return false;

        Vec3d center = player.getPos();
        Box searchBox = player.getBoundingBox().expand(r);

        for (Entity entity : mc.world.getEntities()) {
            if (entity == player) continue;
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
            if (!searchBox.intersects(entity.getBoundingBox())) continue;

            double dist = center.distanceTo(entity.getBoundingBox().getCenter());
            if (dist <= r) return true;
        }
        return false;
    }
}
