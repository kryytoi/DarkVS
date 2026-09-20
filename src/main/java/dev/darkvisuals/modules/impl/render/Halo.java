package dev.darkvisuals.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.darkvisuals.client.events.impl.EventRender3D;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

/**
 * Halo — светящийся нимб над головой игрока.
 * Плоское кольцо света, висящее над головой, всегда
                повёрнуто к камере и мягко пульсирует.
 */
public class Halo extends Module {

    private final NumberSetting height =
            new NumberSetting("Высота", 2.2f, 0.3f, 6f, 0.1f);
    private final NumberSetting radius =
            new NumberSetting("Радиус", 0.95f, 0.3f, 3f, 0.05f);
    private final NumberSetting thickness =
            new NumberSetting("Толщина", 0.14f, 0.02f, 0.6f, 0.01f);
    private final NumberSetting pulseSpeed =
            new NumberSetting("Скорость пульсации", 1.0f, 0f, 3f, 0.1f);
    private final ColorSetting color =
            new ColorSetting("Цвет", new Color(255, 235, 170, 255).getRGB());

    public Halo() {
        super("Halo", Category.Render, "Светящийся нимб над головой игрока");
        getSettings().add(height);
        getSettings().add(radius);
        getSettings().add(thickness);
        getSettings().add(pulseSpeed);
        getSettings().add(color);
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float camYaw = mc.gameRenderer.getCamera().getYaw();
        float camPitch = mc.gameRenderer.getCamera().getPitch();

        Vec3d player = mc.player.getLerpedPos(event.getTickDelta());
        float eyeHeight = mc.player.getEyeHeight(mc.player.getPose());
        Vec3d pos = player.add(0, eyeHeight + height.getValue(), 0);

        // мягкая пульсация свечения
        long now = System.currentTimeMillis();
        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 1000.0 * pulseSpeed.getValue() * Math.PI * 2);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
        matrices.translate((float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z));
        // кольцо всегда лицом к камере
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

        drawRing(matrices.peek().getPositionMatrix(), radius.getValue(), thickness.getValue(), pulse, 1f);

        // лёгкое внешнее сияние вокруг нимба
        drawRing(matrices.peek().getPositionMatrix(),
                radius.getValue() * (1f + thickness.getValue() * 0.9f),
                thickness.getValue() * 1.8f, pulse, 0.3f);

        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    // плоское кольцо (кольцевая полоса) в локальной плоскости XY
    private void drawRing(Matrix4f matrix, float radius, float thickness, float pulse, float intensity) {
        float rIn = radius - thickness * 0.5f * (1f + 0.06f * pulse);
        float rOut = radius + thickness * 0.5f * (1f + 0.06f * pulse);

        Color c = color.getColor();
        int alpha = (int) (c.getAlpha() * intensity * (0.55f + 0.45f * pulse));
        if (alpha <= 4) return;

        int segments = 64;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;

            float x0i = (float) (Math.cos(a0) * rIn), y0i = (float) (Math.sin(a0) * rIn);
            float x1i = (float) (Math.cos(a1) * rIn), y1i = (float) (Math.sin(a1) * rIn);
            float x0o = (float) (Math.cos(a0) * rOut), y0o = (float) (Math.sin(a0) * rOut);
            float x1o = (float) (Math.cos(a1) * rOut), y1o = (float) (Math.sin(a1) * rOut);

            buffer.vertex(matrix, x0i, y0i, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, x1i, y1i, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, x1o, y1o, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            buffer.vertex(matrix, x0o, y0o, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
