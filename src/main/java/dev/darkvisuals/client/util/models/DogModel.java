package dev.darkvisuals.client.util.models;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class DogModel {

    private static final Identifier TEXTURE = Identifier.of("darkvisuals", "textures/taksa.png");
    private static final float TW = 60.0F;
    private static final float TH = 36.0F;

    private float headYaw;
    private float headPitch;
    private float bodyYaw;
    private float frontLeftLegX, frontRightLegX, backLeftLegX, backRightLegX;
    private float frontLeftLegY, frontRightLegY, backLeftLegY, backRightLegY;
    private float tailX, tailZ;
    private boolean lay;

    public void setRotationAngles(float ageInTicks, DogBrain brain) {
        this.headYaw   = -brain.getYaw()   * (float) (Math.PI / 180.0);
        this.headPitch =  brain.getPitch() * (float) (Math.PI / 180.0);
        this.bodyYaw   =  brain.getBody();
        this.lay       =  brain.isLay();

         
        if (brain.isBiting()) {
            this.headPitch += (float) Math.toRadians(40.0);
        }

        float swing  = brain.limbSwing;
        float amount = brain.limbSwingAmount;

        frontLeftLegX  = MathHelper.cos(swing * 0.6662F)                    * 1.4F * amount;
        frontRightLegX = MathHelper.cos(swing * 0.6662F + (float) Math.PI)  * 1.4F * amount;
        backLeftLegX   = MathHelper.cos(swing * 0.6662F + (float) Math.PI)  * 1.4F * amount;
        backRightLegX  = MathHelper.cos(swing * 0.6662F)                    * 1.4F * amount;

        if (lay) {
            frontLeftLegX  = (float) Math.toRadians(-90.0);
            frontRightLegX = (float) Math.toRadians(-90.0);
            backLeftLegX   = (float) Math.toRadians( 90.0);
            backRightLegX  = (float) Math.toRadians( 90.0);
            frontLeftLegY  = (float) Math.toRadians(-22.0);
            frontRightLegY = (float) Math.toRadians( 22.0);
            backLeftLegY   = (float) Math.toRadians( 22.0);
            backRightLegY  = (float) Math.toRadians(-22.0);
        } else {
            frontLeftLegY = frontRightLegY = backLeftLegY = backRightLegY = 0.0F;
        }

         
        float tailSpeed = amount > 0.1F ? 0.6F : 0.15F;  
        float tailAmplitude = amount > 0.1F ? 0.5F : 0.2F;  

        tailX = (float) Math.toRadians(lay ? 45.0 : 22.0);
        tailZ = (float) Math.toRadians(-22.5)
                + (float) Math.toRadians(22.5)
                + (float) Math.cos(ageInTicks * tailSpeed) * tailAmplitude;
    }

    public void render(MatrixStack ms, VertexConsumerProvider vcp, DogBrain brain, int light) {
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        int overlay = OverlayTexture.DEFAULT_UV;

        ms.push();
        ms.translate(0.0, 1.4F - (lay ? 0.3F : 0.0F), 0.0);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bodyYaw));

         
        if (!lay) {
            float jumpPitch = (float) MathHelper.clamp(brain.getVelocityY() * -25.0, -35.0, 35.0);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(jumpPitch));
        }

        renderHead(ms, vc, light, overlay);
        renderNeck(ms, vc, light, overlay);
        renderBody(ms, vc, light, overlay);
        renderLegs(ms, vc, light, overlay);
        renderTail(ms, vc, light, overlay);
        ms.pop();
    }

    private void renderHead(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0F, 0.65625F, -0.425F);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(headYaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(headPitch));

        cube(ms, vc, light, overlay, -3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 4.0F, 0,  0,  false);
        cube(ms, vc, light, overlay, -1.5F,  0.0F, -7.0F, 3.0F, 3.0F, 3.0F, 21, 0,  false);

        ms.push();
        ms.translate(0.1875F, 0.1875F, -0.125F);
        cube(ms, vc, light, overlay,  0.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F, 32, 4,  false);
        cube(ms, vc, light, overlay,  0.0F, -5.5F, -0.75F, 1.0F, 1.0F, 1.5F, 34, 1, false);
        ms.pop();

        ms.push();
        ms.translate(-0.1875F, 0.1875F, -0.125F);
        cube(ms, vc, light, overlay, -1.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F, 32, 4,  true);
        cube(ms, vc, light, overlay, -1.0F, -5.5F, -0.75F, 1.0F, 1.0F, 1.5F, 34, 1, true);
        ms.pop();

        ms.pop();
    }

    private void renderNeck(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0F, 0.65625F, -0.3125F);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F));
        cube(ms, vc, light, overlay, -2.95F, -1.0F, -4.0F, 5.9F, 5.0F, 6.0F, 15, 7, false);
        ms.pop();
    }

    private void renderBody(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0F, 0.84375F, -0.3125F);

        ms.push();
        ms.translate(0.0F, 0.0F, 0.1875F);
        cube(ms, vc, light, overlay, -4.0F, -3.5F, -3.0F, 8.0F, 7.0F, 6.0F, 32, 13, false);
        ms.pop();

        ms.push();
        ms.translate(0.0F, -0.03125F, 0.34375F);
        cube(ms, vc, light, overlay, -3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 11.0F, 3, 19, false);
        ms.pop();

        ms.pop();
    }

    private void renderLegs(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.09375F, 1.0F, -0.1875F);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(frontLeftLegY));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(frontLeftLegX));
        cube(ms, vc, light, overlay, -1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, 42, 0, false);
        ms.pop();

        ms.push();
        ms.translate(-0.09375F, 1.0F, -0.1875F);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(frontRightLegY));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(frontRightLegX));
        cube(ms, vc, light, overlay, -1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, 42, 0, true);
        ms.pop();

        ms.push();
        ms.translate(0.09375F, 1.0F, 0.5625F);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(backLeftLegY));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(backLeftLegX));
        cube(ms, vc, light, overlay, -1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, 52, 0, false);
        ms.pop();

        ms.push();
        ms.translate(-0.09375F, 1.0F, 0.5625F);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(backRightLegY));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(backRightLegX));
        cube(ms, vc, light, overlay, -1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, 52, 0, true);
        ms.pop();
    }

    private void renderTail(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0F, 0.5625F, 0.625F);
        ms.multiply(RotationAxis.POSITIVE_X.rotation(tailX));
        ms.multiply(RotationAxis.POSITIVE_Z.rotation(tailZ));
        cube(ms, vc, light, overlay, -1.0F, 2.0F, -1.0F, 2.0F, 8.0F, 2.0F, 2, 12, false);
        ms.pop();
    }

    private void cube(MatrixStack ms, VertexConsumer vc, int light, int overlay,
                      float ox, float oy, float oz, float w, float h, float d,
                      int u, int v, boolean mirror) {
        float x0 = ox * 0.0625F;
        float x1 = (ox + w) * 0.0625F;
        float y0 = oy * 0.0625F;
        float y1 = (oy + h) * 0.0625F;
        float z0 = oz * 0.0625F;
        float z1 = (oz + d) * 0.0625F;

        if (mirror) { float t = x0; x0 = x1; x1 = t; }

        Matrix4f m4 = ms.peek().getPositionMatrix();
        Entry entry = ms.peek();

        quad(vc, m4, entry, light, overlay, x1,y0,z1, (u+d)/60f, (v+d)/36f, x1,y0,z0, u/60f, (v+d)/36f, x1,y1,z0, u/60f, (v+d+h)/36f, x1,y1,z1, (u+d)/60f, (v+d+h)/36f, 1,0,0);
        quad(vc, m4, entry, light, overlay, x0,y0,z0, (u+2f*d+w)/60f, (v+d)/36f, x0,y0,z1, (u+d+w)/60f, (v+d)/36f, x0,y1,z1, (u+d+w)/60f, (v+d+h)/36f, x0,y1,z0, (u+2f*d+w)/60f, (v+d+h)/36f, -1,0,0);
        quad(vc, m4, entry, light, overlay, x1,y0,z1, (u+d+w)/60f, v/36f, x0,y0,z1, (u+d)/60f, v/36f, x0,y0,z0, (u+d)/60f, (v+d)/36f, x1,y0,z0, (u+d+w)/60f, (v+d)/36f, 0,-1,0);
        quad(vc, m4, entry, light, overlay, x1,y1,z0, (u+d+2f*w)/60f, v/36f, x0,y1,z0, (u+d+w)/60f, v/36f, x0,y1,z1, (u+d+w)/60f, (v+d)/36f, x1,y1,z1, (u+d+2f*w)/60f, (v+d)/36f, 0,1,0);
        quad(vc, m4, entry, light, overlay, x1,y0,z0, (u+d+w)/60f, (v+d)/36f, x0,y0,z0, (u+d)/60f, (v+d)/36f, x0,y1,z0, (u+d)/60f, (v+d+h)/36f, x1,y1,z0, (u+d+w)/60f, (v+d+h)/36f, 0,0,-1);
        quad(vc, m4, entry, light, overlay, x0,y0,z1, (u+2f*d+2f*w)/60f, (v+d)/36f, x1,y0,z1, (u+2f*d+w)/60f, (v+d)/36f, x1,y1,z1, (u+2f*d+w)/60f, (v+d+h)/36f, x0,y1,z1, (u+2f*d+2f*w)/60f, (v+d+h)/36f, 0,0,1);
    }

    private void quad(VertexConsumer vc, Matrix4f m4, Entry entry, int light, int overlay,
                      float x0, float y0, float z0, float u0, float v0,
                      float x1, float y1, float z1, float u1, float v1,
                      float x2, float y2, float z2, float u2, float v2,
                      float x3, float y3, float z3, float u3, float v3,
                      float nx, float ny, float nz) {
        vc.vertex(m4, x0, y0, z0).texture(u0, v0).color(1f,1f,1f,1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x1, y1, z1).texture(u1, v1).color(1f,1f,1f,1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x2, y2, z2).texture(u2, v2).color(1f,1f,1f,1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x3, y3, z3).texture(u3, v3).color(1f,1f,1f,1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
    }
}