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
public class OwlModel {

    private static final Identifier TEXTURE = Identifier.of("darkvisuals", "textures/owl_white.png");

     
    private static final float[] BODY   = {0.30F, 0.23F, 0.40F};  
    private static final float[] WING   = {0.24F, 0.18F, 0.34F};  
    private static final float[] FACE   = {0.42F, 0.33F, 0.55F};  
    private static final float[] CHEST  = {0.61F, 0.53F, 0.76F};  
    private static final float[] CHEST2 = {0.72F, 0.64F, 0.85F};  
    private static final float[] EYE    = {0.97F, 0.96F, 0.99F};  
    private static final float[] PUPIL  = {0.10F, 0.08F, 0.13F};  
    private static final float[] ORANGE = {0.91F, 0.58F, 0.23F};  
    private static final float[] ROPE   = {0.85F, 0.76F, 0.58F};  
    private static final float[] HANDLE = {0.55F, 0.42F, 0.28F};  

    private static final float WING_LEN = 6.5F * 0.0625F;  

    private float headYaw;
    private float headPitch;
    private float bodyYaw;
    private float leftLegX, rightLegX;
    private float leftWingZ, rightWingZ;
    private float ropeAngle;
    private float ropeAmount;
    private float hopY;
    private float bodyRoll;

    public void setRotationAngles(float ageInTicks, float tickDelta, OwlBrain brain) {
        this.headYaw   = -brain.getYaw()   * (float) (Math.PI / 180.0);
        this.headPitch =  brain.getPitch() * (float) (Math.PI / 180.0);
        this.bodyYaw   =  brain.getBody();

        float swing  = brain.limbSwing;
        float amount = brain.limbSwingAmount;

        this.ropeAmount = brain.getRopeAmount(tickDelta);
        this.ropeAngle  = brain.getRopePhase(tickDelta);

         
        leftLegX  = MathHelper.cos(swing * 0.9F)                   * 1.1F * amount;
        rightLegX = MathHelper.cos(swing * 0.9F + (float) Math.PI) * 1.1F * amount;

         
        float foldedL =  0.28F + MathHelper.cos(swing * 0.9F) * 0.35F * amount;
        float foldedR = -0.28F - MathHelper.cos(swing * 0.9F) * 0.35F * amount;
        float raisedL =  2.35F + MathHelper.sin(ropeAngle) * 0.08F;
        float raisedR = -2.35F - MathHelper.sin(ropeAngle) * 0.08F;
        leftWingZ  = MathHelper.lerp(ropeAmount, foldedL, raisedL);
        rightWingZ = MathHelper.lerp(ropeAmount, foldedR, raisedR);

         
        float hop = Math.max(0.0F, MathHelper.cos(ropeAngle));
        hopY = ropeAmount * hop * hop * 0.20F;

         
        bodyRoll = MathHelper.cos(swing * 0.9F) * 0.09F * amount * (1.0F - ropeAmount);
    }

    public void render(MatrixStack ms, VertexConsumerProvider vcp, OwlBrain brain, int light) {
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        int overlay = OverlayTexture.DEFAULT_UV;

        ms.push();
         
        ms.translate(0.0, 0.58F + hopY, 0.0);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bodyYaw));
        ms.multiply(RotationAxis.POSITIVE_Z.rotation(bodyRoll));

         
        if (ropeAmount < 0.5F) {
            float jumpPitch = (float) MathHelper.clamp(brain.getVelocityY() * -25.0, -30.0, 30.0);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(jumpPitch));
        }

        renderHead(ms, vc, light, overlay);
        renderBody(ms, vc, light, overlay);
        renderWings(ms, vc, light, overlay);
        renderFeet(ms, vc, light, overlay);
        renderTail(ms, vc, light, overlay);
        if (ropeAmount > 0.05F) {
            renderRope(ms, vc, light, overlay);
        }
        ms.pop();
    }

    private void renderHead(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0F, 0.156F, 0.0F);  
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(headYaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(headPitch * 0.6F));

         
        cube(ms, vc, light, overlay, -3.5F, -2.5F, -3.0F, 7.0F, 5.0F, 6.0F, BODY);

         
        cube(ms, vc, light, overlay, -3.4F, -3.6F, -1.2F, 1.6F, 1.3F, 2.4F, WING);
        cube(ms, vc, light, overlay,  1.8F, -3.6F, -1.2F, 1.6F, 1.3F, 2.4F, WING);

         
        cube(ms, vc, light, overlay, -3.2F, -2.3F, -3.3F, 6.4F, 4.3F, 0.5F, FACE);

         
        cube(ms, vc, light, overlay, -3.0F, -1.9F, -3.55F, 2.7F, 2.8F, 0.5F, EYE);
        cube(ms, vc, light, overlay,  0.3F, -1.9F, -3.55F, 2.7F, 2.8F, 0.5F, EYE);

         
        cube(ms, vc, light, overlay, -2.1F, -1.0F, -3.75F, 1.3F, 1.6F, 0.4F, PUPIL);
        cube(ms, vc, light, overlay,  0.8F, -1.0F, -3.75F, 1.3F, 1.6F, 0.4F, PUPIL);

         
        cube(ms, vc, light, overlay, -1.7F, -0.8F, -3.9F, 0.5F, 0.5F, 0.3F, EYE);
        cube(ms, vc, light, overlay,  1.2F, -0.8F, -3.9F, 0.5F, 0.5F, 0.3F, EYE);

         
        cube(ms, vc, light, overlay, -3.1F, -2.5F, -3.6F, 2.9F, 0.7F, 0.5F, WING);
        cube(ms, vc, light, overlay,  0.2F, -2.5F, -3.6F, 2.9F, 0.7F, 0.5F, WING);

         
        cube(ms, vc, light, overlay, -0.7F, 0.6F, -3.95F, 1.4F, 1.5F, 1.1F, ORANGE);

        ms.pop();
    }

    private void renderBody(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0F, 0.344F, 0.0F);

         
        cube(ms, vc, light, overlay, -3.0F, -1.0F, -2.5F, 6.0F, 4.2F, 5.0F, BODY);

         
        cube(ms, vc, light, overlay, -2.2F, -1.0F, -2.8F, 4.4F, 3.7F, 0.5F, CHEST);
         
        cube(ms, vc, light, overlay, -1.3F, -0.2F, -3.0F, 2.6F, 2.3F, 0.4F, CHEST2);

        ms.pop();
    }

    private void renderWings(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
         
        ms.push();
        ms.translate(-0.19F, 0.30F, 0.0F);
        ms.multiply(RotationAxis.POSITIVE_Z.rotation(leftWingZ));
        cube(ms, vc, light, overlay, -0.75F, 0.0F, -1.2F, 1.5F, 6.5F, 2.4F, WING);
         
        if (ropeAmount > 0.05F) {
            cube(ms, vc, light, overlay, -0.5F, 6.3F, -0.6F, 1.0F, 1.6F, 1.2F, HANDLE);
        }
        ms.pop();

         
        ms.push();
        ms.translate(0.19F, 0.30F, 0.0F);
        ms.multiply(RotationAxis.POSITIVE_Z.rotation(rightWingZ));
        cube(ms, vc, light, overlay, -0.75F, 0.0F, -1.2F, 1.5F, 6.5F, 2.4F, WING);
        if (ropeAmount > 0.05F) {
            cube(ms, vc, light, overlay, -0.5F, 6.3F, -0.6F, 1.0F, 1.6F, 1.2F, HANDLE);
        }
        ms.pop();
    }

    private void renderFeet(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(-0.10F, 0.51F, 0.0F);
        ms.multiply(RotationAxis.POSITIVE_X.rotation(leftLegX));
        cube(ms, vc, light, overlay, -0.9F, 0.0F, -1.7F, 1.8F, 1.1F, 2.9F, ORANGE);
        ms.pop();

        ms.push();
        ms.translate(0.10F, 0.51F, 0.0F);
        ms.multiply(RotationAxis.POSITIVE_X.rotation(rightLegX));
        cube(ms, vc, light, overlay, -0.9F, 0.0F, -1.7F, 1.8F, 1.1F, 2.9F, ORANGE);
        ms.pop();
    }

    private void renderTail(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
        ms.push();
        ms.translate(0.0F, 0.46F, 0.14F);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F));
        cube(ms, vc, light, overlay, -1.6F, 0.0F, 0.0F, 3.2F, 1.0F, 3.4F, WING);
        ms.pop();
    }

 
    private void renderRope(MatrixStack ms, VertexConsumer vc, int light, int overlay) {
         
        float a = Math.abs(leftWingZ);
        float tipX = 0.19F + WING_LEN * MathHelper.sin(a);
        float tipY = 0.30F + WING_LEN * MathHelper.cos(a);

        float ropeRadius = 0.62F;
        int segments = 15;

        for (int i = 0; i <= segments; i++) {
            float t = (i / (float) segments) * 2.0F - 1.0F;  
            float x = -t * tipX;
            float r = ropeRadius * MathHelper.cos(t * (float) (Math.PI / 2.0));
             
            float offY = r * MathHelper.cos(ropeAngle);
            float offZ = r * MathHelper.sin(ropeAngle);

            ms.push();
            ms.translate(x, tipY + offY, offZ);
            float s = 0.45F;
            cube(ms, vc, light, overlay, -s, -s, -s, s * 2, s * 2, s * 2, ROPE);
            ms.pop();
        }
    }

    private void cube(MatrixStack ms, VertexConsumer vc, int light, int overlay,
                      float ox, float oy, float oz, float w, float h, float d,
                      float[] color) {
        float x0 = ox * 0.0625F;
        float x1 = (ox + w) * 0.0625F;
        float y0 = oy * 0.0625F;
        float y1 = (oy + h) * 0.0625F;
        float z0 = oz * 0.0625F;
        float z1 = (oz + d) * 0.0625F;

        Matrix4f m4 = ms.peek().getPositionMatrix();
        Entry entry = ms.peek();
        float r = color[0], g = color[1], b = color[2];

        quad(vc, m4, entry, light, overlay, r, g, b, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, 1,0,0);
        quad(vc, m4, entry, light, overlay, r, g, b, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, -1,0,0);
        quad(vc, m4, entry, light, overlay, r, g, b, x1,y0,z1, x0,y0,z1, x0,y0,z0, x1,y0,z0, 0,-1,0);
        quad(vc, m4, entry, light, overlay, r, g, b, x1,y1,z0, x0,y1,z0, x0,y1,z1, x1,y1,z1, 0,1,0);
        quad(vc, m4, entry, light, overlay, r, g, b, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, 0,0,-1);
        quad(vc, m4, entry, light, overlay, r, g, b, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, 0,0,1);
    }

    private void quad(VertexConsumer vc, Matrix4f m4, Entry entry, int light, int overlay,
                      float r, float g, float b,
                      float x0, float y0, float z0,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float nx, float ny, float nz) {
        vc.vertex(m4, x0, y0, z0).texture(0.25F, 0.25F).color(r, g, b, 1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x1, y1, z1).texture(0.75F, 0.25F).color(r, g, b, 1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x2, y2, z2).texture(0.75F, 0.75F).color(r, g, b, 1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
        vc.vertex(m4, x3, y3, z3).texture(0.25F, 0.75F).color(r, g, b, 1f).overlay(overlay).light(light).normal(entry, nx, ny, nz);
    }
}
