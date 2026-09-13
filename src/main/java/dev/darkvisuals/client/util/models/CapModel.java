package dev.darkvisuals.client.util.models;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class CapModel {

    private static final Identifier BLANK_TEXTURE = Identifier.of("minecraft", "textures/misc/white.png");

     
    private static final int COLOR_YELLOW = 0xFDD835;
    private static final int COLOR_RED    = 0xE53935;
    private static final int COLOR_BLUE   = 0x1E88E5;
    private static final int COLOR_GREEN  = 0x2E7D32;
    private static final int COLOR_AXIS   = 0x1565C0;

    public void render(MatrixStack ms, VertexConsumerProvider vcp, int light, float propellerRotation) {
         
         
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(BLANK_TEXTURE));
        int overlay = OverlayTexture.DEFAULT_UV;

        ms.push();

         
        float crownY0 = 3.0F, crownH = 3.2F;
        float crownZ0 = -4.2F, crownD = 8.4F;

        colorCube(ms, vc, light, overlay, -4.6F, crownY0, crownZ0, 3.0F, crownH, crownD, COLOR_YELLOW);
        colorCube(ms, vc, light, overlay, -1.6F, crownY0, crownZ0, 3.2F, crownH, crownD, COLOR_RED);
        colorCube(ms, vc, light, overlay,  1.6F, crownY0, crownZ0, 3.0F, crownH, crownD, COLOR_BLUE);

         
        colorCube(ms, vc, light, overlay, -4.6F, crownY0 - 0.4F, crownZ0 - 2.4F, 9.2F, 0.6F, 2.6F, COLOR_GREEN);

         
        float beadY = crownY0 + crownH;
        colorCube(ms, vc, light, overlay, -0.4F, beadY,        -0.4F, 0.8F, 0.6F, 0.8F, COLOR_YELLOW);
        colorCube(ms, vc, light, overlay, -0.4F, beadY + 0.6F, -0.4F, 0.8F, 0.6F, 0.8F, COLOR_RED);
        colorCube(ms, vc, light, overlay, -0.4F, beadY + 1.2F, -0.4F, 0.8F, 0.6F, 0.8F, COLOR_BLUE);

         
        float axisY = beadY + 1.8F;
        colorCube(ms, vc, light, overlay, -0.15F, axisY, -0.15F, 0.3F, 1.0F, 0.3F, COLOR_AXIS);

        ms.push();
        ms.translate(0.0F, (axisY + 1.0F) * 0.0625F, 0.0F);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(propellerRotation));

         
        colorCube(ms, vc, light, overlay, -5.0F, 0.0F, -0.5F, 10.0F, 0.2F, 1.0F, COLOR_BLUE);

        ms.pop();
        ms.pop();
    }

    private void colorCube(MatrixStack ms, VertexConsumer vc, int light, int overlay,
                           float ox, float oy, float oz, float w, float h, float d,
                           int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        float x0 = ox * 0.0625F;
        float x1 = (ox + w) * 0.0625F;
        float y0 = oy * 0.0625F;
        float y1 = (oy + h) * 0.0625F;
        float z0 = oz * 0.0625F;
        float z1 = (oz + d) * 0.0625F;

        Matrix4f m4 = ms.peek().getPositionMatrix();
        MatrixStack.Entry entry = ms.peek();

        quad(vc, m4, entry, light, overlay, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0, 0, -1, r, g, b);
        quad(vc, m4, entry, light, overlay, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0, 0, 1, r, g, b);
        quad(vc, m4, entry, light, overlay, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, -1, 0, 0, r, g, b);
        quad(vc, m4, entry, light, overlay, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, 1, 0, 0, r, g, b);
        quad(vc, m4, entry, light, overlay, x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1, 0, 1, 0, r, g, b);
        quad(vc, m4, entry, light, overlay, x1, y0, z1, x0, y0, z1, x0, y0, z0, x1, y0, z0, 0, -1, 0, r, g, b);
    }

    private void quad(VertexConsumer vc, Matrix4f m4, MatrixStack.Entry entry, int light, int overlay,
                      float x0, float y0, float z0,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float nx, float ny, float nz, float r, float g, float b) {
        vc.vertex(m4, x0, y0, z0).color(r, g, b, 1f).texture(0f, 0f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x1, y1, z1).color(r, g, b, 1f).texture(0f, 0f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x2, y2, z2).color(r, g, b, 1f).texture(0f, 0f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x3, y3, z3).color(r, g, b, 1f).texture(0f, 0f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
    }
}