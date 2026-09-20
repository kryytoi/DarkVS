package dev.darkvisuals.client.util.models;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Роботизированные щупальца за спиной: тёмные металлические сегменты,
 * светящиеся сервоприводы в цвет темы и клешни на концах.
 */
public final class RobotTentaclesModel {

    private RobotTentaclesModel() {
    }

    private static final int SEGMENTS = 11;
    private static final float PIXEL = 0.0625f;
    private static final float SERVO_STEP = 1.2f;

    // { yaw, backX, rollZ, bendZ, lenMul, phase, widthMul }
    private static final float[][] TENTACLES = {
            { -16f, -20f, +34f, +3.4f, 1.05f, 0.0f, 1.00f },
            { +16f, -20f, -34f, -3.4f, 1.05f, 1.6f, 1.00f },
            {  -9f, -14f, +88f, +2.2f, 0.85f, 3.2f, 0.85f },
            {  +9f, -14f, -88f, -2.2f, 0.85f, 4.8f, 0.85f },
    };

    public static void render(PlayerEntity player, float tickDelta, MatrixStack matrices, Vec3d cameraPos,
                              float animTime, float sizeSetting, boolean animated) {
        if (!player.isAlive() || player.isInvisible()) return;
        if (player.isGliding() || player.getPose() == EntityPose.SWIMMING || player.isInSwimmingPose()) return;

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);

        double px = MathHelper.lerp(tickDelta, player.prevX, player.getX()) - cameraPos.x;
        double py = MathHelper.lerp(tickDelta, player.prevY, player.getY()) - cameraPos.y;
        double pz = MathHelper.lerp(tickDelta, player.prevZ, player.getZ()) - cameraPos.z;

        matrices.push();
        matrices.translate(px, py, pz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

        float backY = player.isSneaking() ? 0.80f : 1.05f;
        matrices.translate(0.0, backY, -0.30);
        if (player.isSneaking()) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(24.0f));
        }

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // Светящийся проход: ядро крепления и сервоприводы
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        BufferBuilder glow = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        renderHub(glow, matrices.peek().getPositionMatrix(), sizeSetting, true);
        for (float[] cfg : TENTACLES) {
            renderTentacle(matrices, glow, cfg, animTime, sizeSetting, animated, true);
        }
        BufferRenderer.drawWithGlobalProgram(glow.end());

        // Основной проход: металл
        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        renderHub(buffer, matrices.peek().getPositionMatrix(), sizeSetting, false);
        for (float[] cfg : TENTACLES) {
            renderTentacle(matrices, buffer, cfg, animTime, sizeSetting, animated, false);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void renderHub(BufferBuilder buffer, Matrix4f m, float size, boolean glowPass) {
        float w = 3.6f * size * (glowPass ? 2.2f : 1.0f) * PIXEL;
        int color = glowPass ? accent(1.4f, 90) : metal(0);
        cube(buffer, m, -w, -w, -w * 0.6f, w, w, w * 0.6f, color);
    }

    private static void renderTentacle(MatrixStack matrices, BufferBuilder buffer, float[] cfg,
                                       float animTime, float size, boolean animated, boolean glowPass) {
        float yaw = cfg[0];
        float backX = cfg[1];
        float rollZ = cfg[2];
        float bendZ = cfg[3];
        float lenMul = cfg[4];
        float phase = cfg[5];
        float widthMul = cfg[6];

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(backX));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rollZ));

        float segLen = 2.4f * size * lenMul;
        float widthStart = 3.2f * size * widthMul;
        float widthEnd = 1.1f * size * widthMul;

        int jointColor = glowPass ? accent(1.45f, 70) : accent(1.25f, 255);
        int clawColor = glowPass ? accent(1.2f, 45) : accent(0.65f, 255);

        for (int seg = 0; seg < SEGMENTS; seg++) {
            float t = seg / (float) (SEGMENTS - 1);

            float bend = bendZ;
            float pitch = 0.0f;

            if (animated) {
                // Ступенчатое движение сервоприводов вместо плавной волны
                float amp = 0.5f + t * 2.0f;
                float wave = MathHelper.sin(animTime * 0.8f + phase + seg * 0.55f) * amp;
                bend += Math.round(wave / SERVO_STEP) * SERVO_STEP;
                float wavePitch = MathHelper.cos(animTime * 0.6f + phase + seg * 0.4f) * amp * 0.5f;
                pitch += Math.round(wavePitch / SERVO_STEP) * SERVO_STEP;
            }

            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(bend));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

            float w = MathHelper.lerp(t, widthStart, widthEnd) * 0.5f;
            float lenBlocks = segLen * PIXEL;
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            if (glowPass) {
                // светящееся кольцо сервопривода между сегментами
                float jw = w * 2.4f;
                cube(buffer, matrix, -jw * PIXEL, 0.0f, -jw * PIXEL, jw * PIXEL, lenBlocks * 0.35f, jw * PIXEL, jointColor);
            } else {
                cube(buffer, matrix, -w * PIXEL, 0.0f, -w * PIXEL, w * PIXEL, lenBlocks * 0.92f, w * PIXEL,
                        seg % 2 == 0 ? metal(0) : metal(1));
                // сам сервопривод
                float jw = w * 1.05f;
                cube(buffer, matrix, -jw * PIXEL, lenBlocks * 0.92f, -jw * PIXEL, jw * PIXEL, lenBlocks, jw * PIXEL, jointColor);
            }

            matrices.translate(0.0, lenBlocks, 0.0);
        }

        if (!glowPass) {
            // клешня: два шипа-захвата
            float w = MathHelper.lerp(1.0f, widthStart, widthEnd) * 0.5f;
            float clawLen = 4.2f * size * lenMul * PIXEL;
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(24.0f));
            spike(buffer, matrices.peek().getPositionMatrix(), w * 0.55f * PIXEL, clawLen, clawColor);
            matrices.pop();
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-24.0f));
            spike(buffer, matrices.peek().getPositionMatrix(), w * 0.55f * PIXEL, clawLen, clawColor);
            matrices.pop();
        }

        matrices.pop();
    }

    private static int accent(float brightness, int alpha) {
        java.awt.Color c = dev.darkvisuals.client.managers.ThemeManager.getInstance()
                .getCurrentTheme().getAccentColor();
        int r = Math.min(255, (int) (c.getRed() * brightness));
        int g = Math.min(255, (int) (c.getGreen() * brightness));
        int b = Math.min(255, (int) (c.getBlue() * brightness));
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private static int metal(int variant) {
        // два оттенка тёмного металла для чередования сегментов
        return variant == 0 ? 0xFF363B44 : 0xFF4A505C;
    }

    private static void spike(BufferBuilder buffer, Matrix4f m, float w, float len, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        quad(buffer, m, -w, 0, -w,  w, 0, -w,  0, len, 0,  0, len, 0, r, g, b, a);
        quad(buffer, m,  w, 0,  w, -w, 0,  w,  0, len, 0,  0, len, 0, r, g, b, a);
        quad(buffer, m, -w, 0,  w, -w, 0, -w,  0, len, 0,  0, len, 0, r, g, b, a);
        quad(buffer, m,  w, 0, -w,  w, 0,  w,  0, len, 0,  0, len, 0, r, g, b, a);
        quad(buffer, m, -w, 0, -w, -w, 0,  w,  w, 0,  w,  w, 0, -w, r, g, b, a);
    }

    private static void cube(BufferBuilder buffer, Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        quad(buffer, m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, a);
        quad(buffer, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        quad(buffer, m, x1, y0, z1, x0, y0, z1, x0, y0, z0, x1, y0, z0, r, g, b, a);
        quad(buffer, m, x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1, r, g, b, a);
        quad(buffer, m, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, a);
        quad(buffer, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
    }

    private static void quad(BufferBuilder buffer, Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             int r, int g, int b, int a) {
        buffer.vertex(m, x0, y0, z0).color(r, g, b, a);
        buffer.vertex(m, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(m, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(m, x3, y3, z3).color(r, g, b, a);
    }
}
