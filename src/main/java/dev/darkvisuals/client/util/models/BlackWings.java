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

 
public final class BlackWings {

    private BlackWings() {
    }

     
     
     
     
    private static final String[] WING_SHAPE = {
            "..W...........",
            ".BWB......W...",
            ".BDDB....BW...",
            "BDMMDB..BDB...",
            "BDMMMMDBDDB...",
            ".DMMMMMMMDDB..",
            ".DMMMMMMMMMDB.",
            ".MMMGMMMMMMMD.",
            ".MMMGMMMM.MMM.",
            "..MM.GMM...MM.",
            "..M...MM....M.",
            "..M....M......"
    };

    private static final int COLOR_CLAW     = 0xFFEDEDED;  
    private static final int COLOR_BONE     = 0xFF7D7D7D;  
    private static final int COLOR_DARK     = 0xFF262626;  
    private static final int COLOR_MEMBRANE = 0xF50A0A0A;  
    private static final int COLOR_GLINT    = 0xFF383838;  

    private static final float PIXEL = 0.072f;  
    private static final float FLAP_SPEED = 0.26f;

    public static void render(PlayerEntity player, float tickDelta, MatrixStack matrices, Vec3d cameraPos,
                              float sizeSetting, boolean animated) {
        if (!player.isAlive() || player.isInvisible()) return;
        if (player.isGliding() || player.getPose() == EntityPose.SWIMMING || player.isInSwimmingPose()) return;

        Vec3d velocity = player.getVelocity();
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float yawRad = bodyYaw * 0.017453292F;
        Vec3d forward = new Vec3d(-MathHelper.sin(yawRad), 0.0, MathHelper.cos(yawRad));

        float forwardMove = (float) (velocity.x * forward.x + velocity.z * forward.z);
        float verticalMove = (float) velocity.y;
        float motion = MathHelper.clamp(Math.abs(forwardMove) * 1.2f + Math.abs(verticalMove) * 0.9f, 0.0f, 1.5f);

         
        float anim = (player.age + tickDelta) * FLAP_SPEED * (1.0f + motion * 0.9f);
        float sin = animated ? MathHelper.sin(anim) : 0.0f;

        float spreadAngle = 26.0f + motion * 6.0f;               
        float rollAngle = animated ? sin * 16.0f + 6.0f : 6.0f;  
        float pitchAngle = 8.0f + (animated ? MathHelper.cos(anim) * 3.0f : 0.0f);

        double px = MathHelper.lerp(tickDelta, player.prevX, player.getX()) - cameraPos.x;
        double py = MathHelper.lerp(tickDelta, player.prevY, player.getY()) - cameraPos.y;
        double pz = MathHelper.lerp(tickDelta, player.prevZ, player.getZ()) - cameraPos.z;

        matrices.push();
        matrices.translate(px, py, pz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));

         
        if (player.isSneaking()) {
            matrices.translate(0.0f, 1.02f, 0.02f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(24.0f));
            matrices.translate(0.0f, 0.0f, 0.10f);
        } else {
            matrices.translate(0.0f, 1.18f, 0.14f);
        }

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        renderWingSide(matrices, 1.0f, spreadAngle, pitchAngle, rollAngle, sizeSetting);
        renderWingSide(matrices, -1.0f, spreadAngle, pitchAngle, rollAngle, sizeSetting);

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        matrices.pop();
    }

    private static void renderWingSide(MatrixStack matrices, float side, float spread, float pitch, float roll, float size) {
        matrices.push();
        matrices.translate(side * 0.13f, 0.0f, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * spread));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-side * roll));

        float pixel = PIXEL * size;
        float topY = 3.0f * pixel;  

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int row = 0; row < WING_SHAPE.length; row++) {
            String line = WING_SHAPE[row];
            for (int col = 0; col < line.length(); col++) {
                int color = colorFor(line.charAt(col));
                if (color == 0) continue;

                float x0 = side * (0.04f + col * pixel);
                float x1 = side * (0.04f + (col + 1) * pixel);
                float y0 = topY - row * pixel;
                float y1 = topY - (row + 1) * pixel;
                 
                float z = -col * pixel * 0.10f;

                quad(buffer, matrix, x0, y0, x1, y1, z, color);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();
    }

    private static int colorFor(char c) {
        return switch (c) {
            case 'W' -> COLOR_CLAW;
            case 'B' -> COLOR_BONE;
            case 'D' -> COLOR_DARK;
            case 'M' -> COLOR_MEMBRANE;
            case 'G' -> COLOR_GLINT;
            default -> 0;
        };
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix,
                             float x0, float y0, float x1, float y1, float z, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        buffer.vertex(matrix, x0, y0, z).color(r, g, b, a);
        buffer.vertex(matrix, x0, y1, z).color(r, g, b, a);
        buffer.vertex(matrix, x1, y1, z).color(r, g, b, a);
        buffer.vertex(matrix, x1, y0, z).color(r, g, b, a);
    }
}
