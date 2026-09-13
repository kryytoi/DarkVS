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

 
public final class KaguneModel {

    private KaguneModel() {
    }

 
    private static int themeColor(float brightness, int alpha) {
        java.awt.Color c = dev.darkvisuals.client.managers.ThemeManager.getInstance()
                .getCurrentTheme().getAccentColor();
        int r = Math.min(255, (int) (c.getRed() * brightness));
        int g = Math.min(255, (int) (c.getGreen() * brightness));
        int b = Math.min(255, (int) (c.getBlue() * brightness));
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private static final int SEGMENTS = 12;           
    private static final float PIXEL = 0.0625f;       

 
    private static final float[][] TENTACLES = {
             
            {  -14f,  -18f,   +38f,    +4.2f,    1.00f,  0.0f },  
            {  +14f,  -18f,   -38f,    -4.2f,    1.00f,  1.6f },  
            {  -10f,  -12f,  +112f,    +2.6f,    0.82f,  3.2f },  
            {  +10f,  -12f,  -112f,    -2.6f,    0.82f,  4.8f },  
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

         
         
         
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        BufferBuilder glow = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (float[] cfg : TENTACLES) {
            renderTentacle(matrices, glow, cfg, animTime, sizeSetting, animated, true);
        }
        BufferRenderer.drawWithGlobalProgram(glow.end());

         
        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (float[] cfg : TENTACLES) {
            renderTentacle(matrices, buffer, cfg, animTime, sizeSetting, animated, false);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void renderTentacle(MatrixStack matrices, BufferBuilder buffer, float[] cfg,
                                       float animTime, float size, boolean animated, boolean glowPass) {
        float yaw = cfg[0];
        float backX = cfg[1];
        float rollZ = cfg[2];
        float bendZ = cfg[3];
        float lenMul = cfg[4];
        float phase = cfg[5];

        matrices.push();
         
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));     
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(backX));  
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rollZ));  

         
        float widthMul = glowPass ? 2.1f : 1.0f;
        float segLen = 2.6f * size * lenMul;                     
        float widthStart = 3.4f * size * widthMul;               
        float widthEnd = 0.9f * size * widthMul;                 

         
         
        int colorRoot = glowPass ? themeColor(1.25f, 60) : themeColor(1.0f, 255);
        int colorTip  = glowPass ? themeColor(1.10f, 35) : themeColor(0.45f, 255);
        int colorSpike = glowPass ? themeColor(1.10f, 35) : themeColor(0.30f, 255);

        for (int seg = 0; seg < SEGMENTS; seg++) {
            float t = seg / (float) (SEGMENTS - 1);

             
             
            float bend = bendZ;
            float pitch = 0.0f;

            if (animated) {
                 
                 
                float amp = 0.6f + t * 2.4f;
                bend += MathHelper.sin(animTime * 0.9f + phase + seg * 0.45f) * amp;
                pitch = MathHelper.cos(animTime * 0.7f + phase + seg * 0.35f) * amp * 0.5f;
            }

            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(bend));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

            float w = MathHelper.lerp(t, widthStart, widthEnd) * 0.5f;
            int color = lerpColor(t, colorRoot, colorTip);

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float lenBlocks = segLen * PIXEL;
            cube(buffer, matrix, -w * PIXEL, 0.0f, -w * PIXEL, w * PIXEL, lenBlocks, w * PIXEL, color);

            matrices.translate(0.0, lenBlocks, 0.0);
        }

         
        float tipW = widthEnd * 0.6f * PIXEL;
        float tipLen = 5.5f * size * lenMul * PIXEL;
        spike(buffer, matrices.peek().getPositionMatrix(), tipW, tipLen, colorSpike);

        matrices.pop();
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

    private static int lerpColor(float t, int from, int to) {
        int a = (int) MathHelper.lerp(t, (from >> 24) & 0xFF, (to >> 24) & 0xFF);
        int r = (int) MathHelper.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(t, from & 0xFF, to & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
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
