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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * ElytraTrails — светящийся след от кончиков крыльев при полёте на элитрах.
 * Рисуется billboard-квадами (как ArrowTrails) напрямую в матрицу события —
 * без Render3D.prepare()/render(), чтобы не чистить и не дублировать общие очереди.
 */
public class ElytraTrails extends Module {

    private final NumberSetting length =
            new NumberSetting("Длина", 45f, 10f, 120f, 5f);
    private final NumberSetting size =
            new NumberSetting("Размер", 0.3f, 0.08f, 1.0f, 0.02f);
    private final NumberSetting brightness =
            new NumberSetting("Яркость", 200f, 40f, 255f, 10f);
    private final BooleanSetting rainbow = new BooleanSetting("Радуга", true);
    private final ColorSetting trailColor =
            new ColorSetting("Цвет", new Color(170, 130, 255, 255).getRGB());

    private final ArrayDeque<Vec3d> leftTrail = new ArrayDeque<>();
    private final ArrayDeque<Vec3d> rightTrail = new ArrayDeque<>();

    public ElytraTrails() {
        super("ElytraTrails", Category.Render, "Шлейф от крыльев при полёте");
        getSettings().add(length);
        getSettings().add(size);
        getSettings().add(brightness);
        getSettings().add(rainbow);
        getSettings().add(trailColor);
    }

    @Override
    public void onDisable() {
        leftTrail.clear();
        rightTrail.clear();
        super.onDisable();
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;

        int maxPoints = (int) (float) length.getValue();

        if (mc.player.isGliding()) {
            // кончики крыльев: по бокам и чуть позади игрока
            float yawRad = (float) Math.toRadians(mc.player.getYaw());
            double wingSpan = 0.62;
            double back = 0.35;
            double wingY = mc.player.getY() + 1.05;

            double bx = mc.player.getX() - Math.sin(yawRad) * back;
            double bz = mc.player.getZ() + Math.cos(yawRad) * back;

            Vec3d left = new Vec3d(
                    bx + Math.cos(yawRad) * -wingSpan, wingY, bz - Math.sin(yawRad) * -wingSpan);
            Vec3d right = new Vec3d(
                    bx + Math.cos(yawRad) * wingSpan, wingY, bz - Math.sin(yawRad) * wingSpan);

            leftTrail.addLast(left);
            rightTrail.addLast(right);
        } else {
            // после приземления шлейф быстро тает
            if (!leftTrail.isEmpty()) leftTrail.pollFirst();
            if (!rightTrail.isEmpty()) rightTrail.pollFirst();
        }

        while (leftTrail.size() > maxPoints) leftTrail.pollFirst();
        while (rightTrail.size() > maxPoints) rightTrail.pollFirst();
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (leftTrail.isEmpty() && rightTrail.isEmpty()) return;
        if (fullNullCheck()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float camYaw = mc.gameRenderer.getCamera().getYaw();
        float camPitch = mc.gameRenderer.getCamera().getPitch();
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        renderTrail(matrices, leftTrail, cam, camYaw, camPitch, now);
        renderTrail(matrices, rightTrail, cam, camYaw, camPitch, now);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderTrail(MatrixStack matrices, ArrayDeque<Vec3d> trail,
                             Vec3d cam, float camYaw, float camPitch, long now) {
        int count = trail.size();
        if (count < 2) return;

        float maxAlpha = brightness.getValue();
        Color base = rainbow.getValue() ? null : trailColor.getColor();
        float maxSize = size.getValue();

        // индекс от хвоста (старые) к голове (у игрока)
        int idx = 0;
        for (Iterator<Vec3d> it = trail.descendingIterator(); it.hasNext(); ) {
            Vec3d point = it.next();
            // t=0 у игрока (самая свежая), t=1 на хвосте
            float t = (float) idx / (float) (count - 1);
            idx++;

            float fade = (1f - t);
            float pointSize = maxSize * (0.4f + 0.6f * fade);
            int alpha = (int) (maxAlpha * fade * fade);
            if (alpha <= 4) continue;

            Color c;
            if (base != null) {
                c = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
            } else {
                int rgb = Color.HSBtoRGB((now / 7000f + t * 0.3f) % 1f, 0.7f, 1f);
                c = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
            }

            matrices.push();
            matrices.translate((float) (point.x - cam.x), (float) (point.y - cam.y), (float) (point.z - cam.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = pointSize;
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, -half, -half, 0).color(c.getRGB());
            buffer.vertex(matrix, half, -half, 0).color(c.getRGB());
            buffer.vertex(matrix, half, half, 0).color(c.getRGB());
            buffer.vertex(matrix, -half, half, 0).color(c.getRGB());
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }
    }
}
