package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ColorSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * ArrowTrails — светящийся след за летящими стрелами (и трезубцами).
 * История позиций хранится по сущности и рисуется затухающими billboard-квадами.
 */
public class ArrowTrails extends Module {

    private final NumberSetting length =
            new NumberSetting("Длина следа", 22f, 6f, 60f, 1f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.12f, 0.03f, 0.4f, 0.01f);
    private final NumberSetting range =
            new NumberSetting("Дистанция", 48f, 8f, 96f, 1f);
    private final BooleanSetting rainbow = new BooleanSetting("Радуга", true);
    private final ColorSetting trailColor =
            new ColorSetting("Цвет", new Color(140, 220, 255, 255).getRGB());

    private final Map<Entity, ArrayDeque<Vec3d>> trails = new HashMap<>();

    public ArrowTrails() {
        super("ArrowTrails", Category.Render, "Светящийся след за летящими стрелами");
        getSettings().add(length);
        getSettings().add(size);
        getSettings().add(range);
        getSettings().add(rainbow);
        getSettings().add(trailColor);
    }

    @Override
    public void onDisable() {
        trails.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        int maxPoints = (int) (float) length.getValue();

        // удаляем следы сущностей, которых больше нет рядом
        trails.keySet().removeIf(e -> e.isRemoved() || e.age > 600
                || e.getPos().distanceTo(mc.player.getPos()) > range.getValue() * 1.5);

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PersistentProjectileEntity)) continue;
            if (entity.getPos().distanceTo(mc.player.getPos()) > range.getValue()) continue;
            // летящая стрела двигается; застрявшая — нет
            if (entity.getVelocity().lengthSquared() < 0.04) continue;

            ArrayDeque<Vec3d> trail = trails.computeIfAbsent(entity, k -> new ArrayDeque<>());
            Vec3d pos = entity.getPos();
            Vec3d last = trail.peekLast();
            if (last == null || last.squaredDistanceTo(pos) > 0.02) {
                trail.addLast(pos);
            }
            while (trail.size() > maxPoints) trail.pollFirst();
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (trails.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        long now = System.currentTimeMillis();
        Color base = trailColor.getColor();
        float maxRange = range.getValue();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (ArrayDeque<Vec3d> trail : trails.values()) {
            if (trail.isEmpty()) continue;

            int count = trail.size();
            int idx = 0;
            for (Vec3d point : trail) {
                // самая свежая точка — ярче и крупнее
                float t = count <= 1 ? 1f : (float) idx / (float) (count - 1);
                float fade = 1f - t;

                if (rainbow.getValue()) {
                    float hue = (now / 6000f + t * 0.35f) % 1f;
                    base = new Color(Color.HSBtoRGB(hue, 0.75f, 1f));
                }

                float pointSize = size.getValue() * (0.35f + 0.65f * fade);
                int alpha = (int) (170 * fade * fade);
                if (alpha <= 3) {
                    idx++;
                    continue;
                }

                matrices.push();
                matrices.translate((float) (point.x - cam.x), (float) (point.y - cam.y), (float) (point.z - cam.z));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                float half = pointSize;
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                buffer.vertex(matrix, -half, -half, 0).color(base.getRed(), base.getGreen(), base.getBlue(), 0);
                buffer.vertex(matrix, -half, half, 0).color(base.getRed(), base.getGreen(), base.getBlue(), alpha / 2);
                buffer.vertex(matrix, half, half, 0).color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
                buffer.vertex(matrix, half, -half, 0).color(base.getRed(), base.getGreen(), base.getBlue(), alpha / 2);
                BufferRenderer.drawWithGlobalProgram(buffer.end());

                matrices.pop();
                idx++;
            }
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
